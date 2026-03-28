package com.homecare.admin.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of banned user IDs.
 * Checked by the JWT filter to reject tokens for banned users immediately.
 * In a distributed deployment, replace with Redis or a shared cache.
 */
@Component
public class BannedUserStore {

    private final Set<UUID> bannedUserIds = ConcurrentHashMap.newKeySet();

    public void ban(UUID userId) {
        bannedUserIds.add(userId);
    }

    public void unban(UUID userId) {
        bannedUserIds.remove(userId);
    }

    public boolean isBanned(UUID userId) {
        return bannedUserIds.contains(userId);
    }
}

