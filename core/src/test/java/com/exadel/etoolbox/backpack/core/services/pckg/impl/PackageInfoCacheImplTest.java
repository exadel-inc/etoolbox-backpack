package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class PackageInfoCacheImplTest {

    private static final long NON_EXPIRING_TTL = -1L;
    private static final long SHORT_TTL_MILLIS = 50L;
    private static final int MAX_CACHE_SIZE = 100;

    @Test
    public void shouldReturnNullForMissingAndNonStringKeys() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);

        assertNull(cache.get("missing"));
    }

    @Test
    public void shouldStoreAndReturnEntriesWhenTtlDoesNotExpire() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);
        PackageInfo packageInfo = createPackageInfo("package-1");

        assertNull(cache.put("/etc/packages/package-1.zip", packageInfo));
        assertSame(packageInfo, cache.get("/etc/packages/package-1.zip"));
    }

    @Test
    public void shouldExpireEntriesImmediatelyWhenTtlIsZero() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(0L);
        PackageInfo packageInfo = createPackageInfo("package-1");

        assertNull(cache.put("/etc/packages/package-1.zip", packageInfo));
        assertNull(cache.get("/etc/packages/package-1.zip"));
    }

    @Test
    public void shouldExpireEntriesAfterPositiveTtl() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(SHORT_TTL_MILLIS);
        String key = "/etc/packages/package-1.zip";
        PackageInfo packageInfo = createPackageInfo("package-1");

        cache.put(key, packageInfo);
        assertSame(packageInfo, cache.get(key));

        awaitExpiration(cache, key);

        assertNull(cache.get(key));
    }

    @Test
    public void shouldReturnNullWhenRemovingExpiredEntries() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(SHORT_TTL_MILLIS);
        String key = "/etc/packages/package-1.zip";

        cache.put(key, createPackageInfo("package-1"));

        pauseMillis(SHORT_TTL_MILLIS + 20L);

        assertNull(cache.remove(key));
        assertNull(cache.get(key));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullValues() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);

        cache.put("/etc/packages/package-1.zip", null);
    }

    @Test
    public void shouldReturnPreviousValueWhenReplacingExistingKey() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);
        String key = "/etc/packages/package-1.zip";
        PackageInfo first = createPackageInfo("package-1");
        PackageInfo second = createPackageInfo("package-2");

        cache.put(key, first);

        assertSame(first, cache.put(key, second));
        assertSame(second, cache.get(key));
    }

    @Test
    public void shouldRemoveAndClearEntries() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);
        String firstKey = "/etc/packages/package-1.zip";
        String secondKey = "/etc/packages/package-2.zip";
        PackageInfo first = createPackageInfo("package-1");
        PackageInfo second = createPackageInfo("package-2");

        cache.put(firstKey, first);
        cache.put(secondKey, second);

        assertSame(first, cache.remove(firstKey));
        assertNull(cache.get(firstKey));
        assertSame(second, cache.get(secondKey));

        cache.clear();

        assertNull(cache.get(secondKey));
    }

    @Test
    public void shouldKeepCacheWithinMaxSizeWhenAddingNewKeys() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            String key = "/etc/packages/package-" + i + ".zip";
            keys.add(key);
            cache.put(key, createPackageInfo("package-" + i));
        }

        String overflowKey = "/etc/packages/package-overflow.zip";
        keys.add(overflowKey);

        cache.put(overflowKey, createPackageInfo("package-overflow"));

        assertNotNull(cache.get(overflowKey));
        assertEquals(MAX_CACHE_SIZE, countAccessibleEntries(cache, keys));
    }

    @Test
    public void shouldNotEvictAnotherEntryWhenReplacingAtCapacity() {
        PackageInfoCacheImpl cache = new PackageInfoCacheImpl(NON_EXPIRING_TTL);
        List<String> keys = new ArrayList<>();
        PackageInfo original = null;
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            String key = "/etc/packages/package-" + i + ".zip";
            keys.add(key);
            PackageInfo packageInfo = createPackageInfo("package-" + i);
            if (i == 0) {
                original = packageInfo;
            }
            cache.put(key, packageInfo);
        }

        String replacementKey = keys.get(0);
        PackageInfo replacement = createPackageInfo("replacement");

        PackageInfo previous = cache.put(replacementKey, replacement);

        assertSame(original, previous);
        assertSame(replacement, cache.get(replacementKey));
        assertEquals(MAX_CACHE_SIZE, countAccessibleEntries(cache, keys));
    }

    private static PackageInfo createPackageInfo(String packageName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageName(packageName);
        packageInfo.setGroupName("group");
        return packageInfo;
    }

    private static int countAccessibleEntries(PackageInfoCacheImpl cache, List<String> keys) {
        int count = 0;
        for (String key : keys) {
            if (cache.get(key) != null) {
                count++;
            }
        }
        return count;
    }

    private static void awaitExpiration(PackageInfoCacheImpl cache, String key) {
        long deadline = System.currentTimeMillis() + 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (cache.get(key) == null) {
                return;
            }
            pauseMillis(10L);
        }
        fail("Cache entry did not expire within the expected time window");
    }

    private static void pauseMillis(long millis) {
        long remainingNanos = TimeUnit.MILLISECONDS.toNanos(millis);
        long deadline = System.nanoTime() + remainingNanos;
        while (remainingNanos > 0L) {
            LockSupport.parkNanos(remainingNanos);
            remainingNanos = deadline - System.nanoTime();
        }
    }
}




