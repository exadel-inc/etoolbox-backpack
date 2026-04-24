package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoCache;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class PackageInfoCacheImpl implements PackageInfoCache {

    private static final int MAX_CACHE_SIZE = 100;

    private final Object lock = new Object();
    private final ConcurrentMap<String, CacheEntry> packageInfos = new ConcurrentHashMap<>();
    private final long cacheTtlMillis;

    PackageInfoCacheImpl(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
    }

    @Override
    public PackageInfo get(String key) {
        CacheEntry entry = packageInfos.get(key);
        if (entry == null) {
            return null;
        }
        return unwrapEntry(key, entry);
    }

    @Override
    public PackageInfo put(String key, PackageInfo value) {
        if(value == null){
            throw new IllegalArgumentException("Package info cannot be null.");
        }
        synchronized (lock) {
            evictExpiredEntries();
            ensureCapacityFor(key);
            CacheEntry previous = packageInfos.put(key, newCacheEntry(value));
            return previous == null ? null : unwrapEntry(key, previous);
        }
    }

    @Override
    public PackageInfo remove(String key) {
        CacheEntry removed = packageInfos.remove(key);
        return removed == null ? null : unwrapEntry(key, removed);
    }

    @Override
    public void clear() {
        packageInfos.clear();
    }

    private CacheEntry newCacheEntry(PackageInfo value) {
        long expiresAt;
        if (cacheTtlMillis < 0) {
            expiresAt = Long.MAX_VALUE;
        } else if (cacheTtlMillis == 0) {
            expiresAt = 0L;
        } else {
            expiresAt = System.currentTimeMillis() + cacheTtlMillis;
        }
        return new CacheEntry(value, expiresAt);
    }

    private PackageInfo unwrapEntry(String key, CacheEntry entry) {
        long now = System.currentTimeMillis();
        if (entry.isExpired(now)) {
            if (key != null) {
                packageInfos.remove(key, entry);
            }
            return null;
        }
        return entry.value;
    }

    private void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        packageInfos.entrySet()
                .removeIf(entry -> entry.getValue().isExpired(now));
    }

    private void ensureCapacityFor(String key) {
        if (packageInfos.containsKey(key) || packageInfos.size() < MAX_CACHE_SIZE) {
            return;
        }
        evictOneEntry();
    }

    private void evictOneEntry() {
        Iterator<Map.Entry<String, CacheEntry>> iterator = packageInfos.entrySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static final class CacheEntry {
        private final PackageInfo value;
        private final long expiresAt;

        private CacheEntry(PackageInfo value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired(long now) {
            return expiresAt <= now;
        }
    }
}

