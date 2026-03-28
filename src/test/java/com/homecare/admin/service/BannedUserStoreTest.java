package com.homecare.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BannedUserStore")
class BannedUserStoreTest {

    private BannedUserStore store;

    @BeforeEach
    void setUp() {
        store = new BannedUserStore();
    }

    @Test
    @DisplayName("newly created store has no banned users")
    void initiallyEmpty() {
        assertFalse(store.isBanned(UUID.randomUUID()));
    }

    @Test
    @DisplayName("ban → isBanned returns true")
    void banUser() {
        UUID userId = UUID.randomUUID();
        store.ban(userId);
        assertTrue(store.isBanned(userId));
    }

    @Test
    @DisplayName("unban → isBanned returns false")
    void unbanUser() {
        UUID userId = UUID.randomUUID();
        store.ban(userId);
        assertTrue(store.isBanned(userId));

        store.unban(userId);
        assertFalse(store.isBanned(userId));
    }

    @Test
    @DisplayName("banning same user twice is idempotent")
    void banTwice() {
        UUID userId = UUID.randomUUID();
        store.ban(userId);
        store.ban(userId);
        assertTrue(store.isBanned(userId));
    }

    @Test
    @DisplayName("unbanning non-banned user is harmless")
    void unbanNotBanned() {
        UUID userId = UUID.randomUUID();
        assertDoesNotThrow(() -> store.unban(userId));
        assertFalse(store.isBanned(userId));
    }

    @Test
    @DisplayName("multiple users can be banned independently")
    void multipleUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        store.ban(user1);
        assertTrue(store.isBanned(user1));
        assertFalse(store.isBanned(user2));

        store.ban(user2);
        assertTrue(store.isBanned(user1));
        assertTrue(store.isBanned(user2));

        store.unban(user1);
        assertFalse(store.isBanned(user1));
        assertTrue(store.isBanned(user2));
    }
}

