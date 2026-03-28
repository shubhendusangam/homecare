package com.homecare.payment.service;

import com.homecare.booking.entity.Booking;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.PaymentStatus;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.payment.config.PaymentConfig;
import com.homecare.payment.dto.WalletResponse;
import com.homecare.payment.entity.Wallet;
import com.homecare.payment.entity.WalletTransaction;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import com.homecare.payment.gateway.PaymentGateway;
import com.homecare.payment.gateway.RefundResponse;
import com.homecare.payment.mapper.WalletTransactionResponseMapper;
import com.homecare.payment.repository.WalletRepository;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentConfig paymentConfig;
    @Mock private PaymentGateway paymentGateway;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WalletTransactionResponseMapper walletTransactionResponseMapper;

    @InjectMocks private WalletService walletService;

    private User customer;
    private User helper;
    private Wallet customerWallet;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());

        customerWallet = Wallet.builder()
                .user(customer).balance(BigDecimal.valueOf(1000))
                .heldAmount(BigDecimal.ZERO).currency("INR").build();
        customerWallet.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).helper(helper)
                .serviceType(ServiceType.CLEANING).status(BookingStatus.COMPLETED)
                .totalPrice(500).basePrice(299)
                .paymentStatus(PaymentStatus.PAID).build();
        booking.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("getOrCreateWallet")
    class GetOrCreateWallet {

        @Test
        @DisplayName("existing wallet → returns it")
        void existingWallet() {
            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));

            Wallet result = walletService.getOrCreateWallet(customer.getId());
            assertEquals(customerWallet.getId(), result.getId());
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("no wallet → creates new one")
        void createsNewWallet() {
            when(walletRepository.findByUserId(customer.getId())).thenReturn(Optional.empty());
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(paymentConfig.getDefaultCurrency()).thenReturn("INR");
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.getOrCreateWallet(customer.getId());
            assertEquals(BigDecimal.ZERO, result.getBalance());
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("user not found → throws")
        void userNotFound() {
            when(walletRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> walletService.getOrCreateWallet(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("creditWallet")
    class CreditWallet {

        @Test
        @DisplayName("increases balance and records transaction")
        void happyPath() {
            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WalletResponse response = walletService.creditWallet(
                    customer.getId(), BigDecimal.valueOf(200), "ref_123");

            assertEquals(BigDecimal.valueOf(1200), customerWallet.getBalance());
            verify(transactionRepository).save(argThat(tx ->
                    tx.getType() == TransactionType.CREDIT_TOPUP &&
                    tx.getStatus() == TransactionStatus.SUCCESS));
        }
    }

    @Nested
    @DisplayName("payBookingViaWallet")
    class PayBookingViaWallet {

        @Test
        @DisplayName("sufficient balance → hold placed")
        void sufficientBalance() {
            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.payBookingViaWallet(customer.getId(), booking);

            assertEquals(0, BigDecimal.valueOf(500).compareTo(customerWallet.getHeldAmount()));
            verify(transactionRepository).save(argThat(tx ->
                    tx.getType() == TransactionType.DEBIT_BOOKING &&
                    tx.getStatus() == TransactionStatus.PENDING));
        }

        @Test
        @DisplayName("insufficient balance → throws")
        void insufficientBalance() {
            customerWallet.setBalance(BigDecimal.valueOf(100));
            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));

            assertThrows(BusinessException.class,
                    () -> walletService.payBookingViaWallet(customer.getId(), booking));
        }
    }

    @Nested
    @DisplayName("releaseHold")
    class ReleaseHold {

        @Test
        @DisplayName("finalizes deduction and credits helper")
        void happyPath() {
            customerWallet.setHeldAmount(BigDecimal.valueOf(500));
            Wallet helperWallet = Wallet.builder()
                    .user(helper).balance(BigDecimal.ZERO)
                    .heldAmount(BigDecimal.ZERO).currency("INR").build();
            helperWallet.setId(UUID.randomUUID());

            WalletTransaction pendingTx = WalletTransaction.builder()
                    .wallet(customerWallet).booking(booking)
                    .type(TransactionType.DEBIT_BOOKING).status(TransactionStatus.PENDING)
                    .amount(BigDecimal.valueOf(500)).build();

            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));
            when(walletRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperWallet));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByBookingIdAndTypeAndStatus(
                    booking.getId(), TransactionType.DEBIT_BOOKING, TransactionStatus.PENDING))
                    .thenReturn(Optional.of(pendingTx));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentConfig.getPlatformFee()).thenReturn(new BigDecimal("0.15"));

            walletService.releaseHold(booking.getId(), booking);

            // Customer wallet: deducted 500
            assertEquals(0, BigDecimal.valueOf(500).compareTo(customerWallet.getBalance()));
            assertEquals(0, BigDecimal.ZERO.compareTo(customerWallet.getHeldAmount()));

            // Helper wallet: credited 85% of 500 = 425
            assertEquals(new BigDecimal("425.00"), helperWallet.getBalance());

            // Transaction marked as SUCCESS
            assertEquals(TransactionStatus.SUCCESS, pendingTx.getStatus());
        }
    }

    @Nested
    @DisplayName("refund")
    class Refund {

        @Test
        @DisplayName("BUG FIX 13: wallet-paid refund doesn't go negative")
        void walletPaidRefund_noNegativeBalance() {
            // Simulate: customer wallet balance was withdrawn after hold
            customerWallet.setBalance(BigDecimal.valueOf(100)); // was 1000, now only 100
            customerWallet.setHeldAmount(BigDecimal.valueOf(500));

            WalletTransaction holdTx = WalletTransaction.builder()
                    .wallet(customerWallet).booking(booking)
                    .type(TransactionType.DEBIT_BOOKING).status(TransactionStatus.PENDING)
                    .amount(BigDecimal.valueOf(500)).build();

            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));
            when(transactionRepository.findByBookingIdAndTypeAndStatus(
                    booking.getId(), TransactionType.DEBIT_BOOKING, TransactionStatus.PENDING))
                    .thenReturn(Optional.of(holdTx));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.refund(booking.getId(), booking, BigDecimal.ONE);

            // Balance: 100 - 500 + 500 = 100, but the guard prevents negative interim
            assertTrue(customerWallet.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                    "Balance should not be negative");
            assertTrue(customerWallet.getHeldAmount().compareTo(BigDecimal.ZERO) >= 0,
                    "Held amount should not be negative");
        }

        @Test
        @DisplayName("Razorpay-paid refund credits wallet + initiates gateway refund")
        void razorpayPaidRefund() {
            booking.setPaymentReference("pay_razorpay_123");

            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));
            when(transactionRepository.findByBookingIdAndTypeAndStatus(any(), any(), any()))
                    .thenReturn(Optional.empty()); // no pending hold → Razorpay-paid
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentGateway.initiateRefund(anyString(), any()))
                    .thenReturn(RefundResponse.builder()
                            .refundId("rfnd_123").status("processed").amount(BigDecimal.valueOf(500)).build());

            walletService.refund(booking.getId(), booking, BigDecimal.ONE);

            // Balance should be increased by refund amount
            assertEquals(BigDecimal.valueOf(1500).setScale(2), customerWallet.getBalance().setScale(2));
            verify(paymentGateway).initiateRefund(eq("pay_razorpay_123"), any());
        }

        @Test
        @DisplayName("50% refund computes correct amount")
        void partialRefund() {
            when(walletRepository.findByUserId(customer.getId()))
                    .thenReturn(Optional.of(customerWallet));
            when(transactionRepository.findByBookingIdAndTypeAndStatus(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.refund(booking.getId(), booking, new BigDecimal("0.50"));

            // 50% of 500 = 250 refunded → 1000 + 250 = 1250
            assertEquals(BigDecimal.valueOf(1250).setScale(2), customerWallet.getBalance().setScale(2));
        }
    }
}

