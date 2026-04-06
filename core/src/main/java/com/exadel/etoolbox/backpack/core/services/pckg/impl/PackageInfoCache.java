package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class PackageInfoCache implements ConcurrentMap<String, PackageInfo> {

    private static final int MAX_CACHE_SIZE = 100;

    private final ConcurrentMap<String, CacheEntry> packageInfos = new ConcurrentHashMap<>();
    private final long cacheTtlMillis;

    PackageInfoCache(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
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

    @Override
    public PackageInfo putIfAbsent(String key, PackageInfo value) {
        Objects.requireNonNull(value);
        while (true) {
            CacheEntry current = packageInfos.get(key);
            if (current != null) {
                PackageInfo currentValue = unwrapEntry(key, current);
                if (currentValue != null) {
                    return currentValue;
                }
                continue;
            }
            synchronized (this) {
                CacheEntry currentInsideLock = packageInfos.get(key);
                if (currentInsideLock != null) {
                    PackageInfo currentValue = unwrapEntry(key, currentInsideLock);
                    if (currentValue != null) {
                        return currentValue;
                    }
                    continue;
                }
                evictExpiredEntries();
                ensureCapacityFor(key);
                if (packageInfos.putIfAbsent(key, newCacheEntry(value)) == null) {
                    return null;
                }
            }
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (!(key instanceof String) || !(value instanceof PackageInfo)) {
            return false;
        }
        CacheEntry current = packageInfos.get(key);
        if (current == null) {
            return false;
        }
        PackageInfo currentValue = unwrapEntry((String) key, current);
        return currentValue != null && currentValue.equals(value) && packageInfos.remove(key, current);
    }

    @Override
    public boolean replace(String key, PackageInfo oldValue, PackageInfo newValue) {
        Objects.requireNonNull(newValue);
        while (true) {
            CacheEntry current = packageInfos.get(key);
            if (current == null) {
                return false;
            }
            PackageInfo currentValue = unwrapEntry(key, current);
            if (currentValue == null) {
                continue;
            }
            if (!currentValue.equals(oldValue)) {
                return false;
            }
            if (packageInfos.replace(key, current, newCacheEntry(newValue))) {
                return true;
            }
        }
    }

    @Override
    public PackageInfo replace(String key, PackageInfo value) {
        Objects.requireNonNull(value);
        while (true) {
            CacheEntry current = packageInfos.get(key);
            if (current == null) {
                return null;
            }
            PackageInfo currentValue = unwrapEntry(key, current);
            if (currentValue == null) {
                continue;
            }
            if (packageInfos.replace(key, current, newCacheEntry(value))) {
                return currentValue;
            }
        }
    }

    @Override
    public int size() {
        evictExpiredEntries();
        return packageInfos.size();
    }

    @Override
    public boolean isEmpty() {
        evictExpiredEntries();
        return packageInfos.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof PackageInfo)) {
            return false;
        }
        for (Map.Entry<String, CacheEntry> entry : packageInfos.entrySet()) {
            PackageInfo currentValue = unwrapEntry(entry.getKey(), entry.getValue());
            if (value.equals(currentValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PackageInfo get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        CacheEntry entry = packageInfos.get(key);
        if (entry == null) {
            return null;
        }
        return unwrapEntry((String) key, entry);
    }

    @Override
    public PackageInfo put(String key, PackageInfo value) {
        Objects.requireNonNull(value);
        evictExpiredEntries();
        ensureCapacityFor(key);
        CacheEntry previous = packageInfos.put(key, newCacheEntry(value));
        return previous == null ? null : unwrapEntry(key, previous);
    }

    @Override
    public PackageInfo remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        CacheEntry removed = packageInfos.remove(key);
        return removed == null ? null : unwrapEntry((String) key, removed);
    }

    @Override
    public void putAll(Map<? extends String, ? extends PackageInfo> m) {
        for (Map.Entry<? extends String, ? extends PackageInfo> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        packageInfos.clear();
    }

    @Override
    public Set<String> keySet() {
        evictExpiredEntries();
        return packageInfos.keySet();
    }

    @Override
    public Collection<PackageInfo> values() {
        return new AbstractCollection<PackageInfo>() {
            @Override
            public Iterator<PackageInfo> iterator() {
                return new Iterator<PackageInfo>() {
                    private final Iterator<Map.Entry<String, CacheEntry>> iterator = packageInfos.entrySet().iterator();
                    private PackageInfo nextValue;

                    @Override
                    public boolean hasNext() {
                        while (nextValue == null && iterator.hasNext()) {
                            Map.Entry<String, CacheEntry> next = iterator.next();
                            nextValue = unwrapEntry(next.getKey(), next.getValue());
                        }
                        return nextValue != null;
                    }

                    @Override
                    public PackageInfo next() {
                        if (!hasNext()) {
                            throw new java.util.NoSuchElementException();
                        }
                        PackageInfo value = nextValue;
                        nextValue = null;
                        return value;
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }

            @Override
            public int size() {
                return PackageInfoCache.this.size();
            }
        };
    }

    @Override
    public Set<Entry<String, PackageInfo>> entrySet() {
        return new AbstractSet<Entry<String, PackageInfo>>() {
            @Override
            public Iterator<Entry<String, PackageInfo>> iterator() {
                return new Iterator<Entry<String, PackageInfo>>() {
                    private final Iterator<Map.Entry<String, CacheEntry>> iterator = packageInfos.entrySet().iterator();
                    private Entry<String, PackageInfo> nextEntry;

                    @Override
                    public boolean hasNext() {
                        while (nextEntry == null && iterator.hasNext()) {
                            Map.Entry<String, CacheEntry> next = iterator.next();
                            PackageInfo value = unwrapEntry(next.getKey(), next.getValue());
                            if (value != null) {
                                nextEntry = new AbstractMap.SimpleEntry<>(next.getKey(), value);
                            }
                        }
                        return nextEntry != null;
                    }

                    @Override
                    public Entry<String, PackageInfo> next() {
                        if (!hasNext()) {
                            throw new java.util.NoSuchElementException();
                        }
                        Entry<String, PackageInfo> entry = nextEntry;
                        nextEntry = null;
                        return entry;
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }

            @Override
            public int size() {
                return PackageInfoCache.this.size();
            }
        };
    }

    private CacheEntry newCacheEntry(PackageInfo value) {
        long expiresAt = cacheTtlMillis <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + cacheTtlMillis;
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
}

