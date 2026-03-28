package com.homecare.payment.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.PaymentStatus;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.payment.config.PaymentConfig;
import com.homecare.payment.dto.TopupInitiateResponse;
import com.homecare.payment.dto.WalletResponse;
import com.homecare.payment.entity.PaymentOrder;
import com.homecare.payment.gateway.MockPaymentGateway;
import com.homecare.payment.gateway.PaymentGateway;
import com.homecare.payment.gateway.PaymentOrderResponse;
import com.homecare.payment.mapper.WalletTransactionResponseMapper;
import com.homecare.payment.repository.PaymentOrderRepository;
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
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PaymentConfig paymentConfig;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WalletTransactionResponseMapper walletTransactionResponseMapper;

    @InjectMocks private PaymentService paymentService;

    private User customer;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).serviceType(ServiceType.CLEANING)
                .status(BookingStatus.ASSIGNED).paymentStatus(PaymentStatus.PENDING)
                .totalPrice(500).basePrice(299).build();
        booking.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("initiateTopup")
    class InitiateTopup {

        @Test
        @DisplayName("happy path — creates order and returns response")
        void happyPath() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(paymentConfig.getDefaultCurrency()).thenReturn("INR");
            when(paymentConfig.getRazorpayKeyId()).thenReturn("rzp_test");
            when(paymentGateway.createOrder(any(), anyString(), anyString()))
                    .thenReturn(PaymentOrderResponse.builder()
                            .orderId("order_123").amount(BigDecimal.valueOf(500)).currency("INR").build());
            when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TopupInitiateResponse response = paymentService.initiateTopup(
                    customer.getId(), BigDecimal.valueOf(500));

            assertEquals("order_123", response.getRazorpayOrderId());
            assertEquals(BigDecimal.valueOf(500), response.getAmount());
        }

        @Test
        @DisplayName("user not found → throws")
        void userNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> paymentService.initiateTopup(UUID.randomUUID(), BigDecimal.valueOf(500)));
        }
    }

    @Nested
    @DisplayName("payBookingViaWallet")
    class PayBookingViaWallet {

        @Test
        @DisplayName("happy path — sets payment status to HELD (BUG 6 regression)")
        void happyPath() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentService.payBookingViaWallet(customer.getId(), booking.getId());

            verify(walletService).payBookingViaWallet(customer.getId(), booking);
            assertEquals(PaymentStatus.HELD, booking.getPaymentStatus());
            assertTrue(booking.getPaymentReference().startsWith("wallet_"));
        }

        @Test
        @DisplayName("not the booking owner → throws FORBIDDEN")
        void notOwner() {
            UUID otherUser = UUID.randomUUID();
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> paymentService.payBookingViaWallet(otherUser, booking.getId()));
        }

        @Test
        @DisplayName("already paid → throws")
        void alreadyPaid() {
            booking.setPaymentStatus(PaymentStatus.PAID);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> paymentService.payBookingViaWallet(customer.getId(), booking.getId()));
        }
    }

    @Nested
    @DisplayName("refundBooking")
    class RefundBooking {

        @Test
        @DisplayName("happy path — delegates to walletService.refund")
        void happyPath() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentService.refundBooking(booking.getId(), BigDecimal.ONE);

            verify(walletService).refund(eq(booking.getId()), eq(booking), eq(BigDecimal.ONE));
            assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        }

        @Test
        @DisplayName("already refunded → throws")
        void alreadyRefunded() {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> paymentService.refundBooking(booking.getId(), BigDecimal.ONE));
        }
    }

    @Nested
    @DisplayName("releaseBookingPayment")
    class ReleaseBookingPayment {

        @Test
        @DisplayName("delegates to walletService.releaseHold")
        void delegatesToWalletService() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            paymentService.releaseBookingPayment(booking.getId());

            verify(walletService).releaseHold(booking.getId(), booking);
        }
    }
}

