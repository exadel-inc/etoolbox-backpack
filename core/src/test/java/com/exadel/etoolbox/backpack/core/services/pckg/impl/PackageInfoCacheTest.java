package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

public class PackageInfoCacheTest {

    private static final long SHORT_TTL_MILLIS = 20L;
    private static final long WAIT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final long CONCURRENT_WAIT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    @Test
    public void shouldStoreAndExpireEntriesOnAccess() {
        PackageInfoCache cache = new PackageInfoCache(SHORT_TTL_MILLIS);
        PackageInfo value = packageInfo("package");

        assertNull(cache.put("/etc/packages/test.zip", value));
        assertSame(value, cache.get("/etc/packages/test.zip"));
        assertTrue(cache.containsKey("/etc/packages/test.zip"));
        assertTrue(cache.containsValue(value));

        awaitUntil(() -> cache.get("/etc/packages/test.zip") == null);

        assertNull(cache.get("/etc/packages/test.zip"));
        assertFalse(cache.containsKey("/etc/packages/test.zip"));
        assertFalse(cache.containsValue(value));
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void shouldApplyConcurrentMapMutations() {
        PackageInfoCache cache = new PackageInfoCache(5_000L);
        PackageInfo first = packageInfo("package-1");
        PackageInfo equalToFirst = packageInfo("package-1");
        PackageInfo second = packageInfo("package-2");

        assertNull(cache.putIfAbsent("key", first));
        assertSame(first, cache.putIfAbsent("key", second));
        assertSame(first, cache.get("key"));

        assertTrue(cache.replace("key", equalToFirst, second));
        assertSame(second, cache.get("key"));

        assertSame(second, cache.replace("key", first));
        assertSame(first, cache.get("key"));

        assertFalse(cache.remove("key", second));
        assertTrue(cache.remove("key", equalToFirst));
        assertNull(cache.get("key"));

        cache.put("other", second);
        assertSame(second, cache.get("other"));
        assertSame(second, cache.remove("other"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void shouldEvictExpiredEntriesBeforeApplyingCapacityLimit() {
        PackageInfoCache cache = new PackageInfoCache(SHORT_TTL_MILLIS);
        cache.put("expired", packageInfo("expired"));

        awaitUntil(() -> cache.get("expired") == null);

        for (int i = 0; i < 100; i++) {
            cache.put("live-" + i, packageInfo("live-" + i));
        }

        assertEquals(100, cache.size());
        assertNull(cache.get("expired"));
        assertFalse(cache.containsKey("expired"));
    }

    @Test
    public void shouldKeepCacheBoundedAtMaxSize() {
        PackageInfoCache cache = new PackageInfoCache(5_000L);

        for (int i = 0; i < 101; i++) {
            cache.put("key-" + i, packageInfo("package-" + i));
        }

        assertEquals(100, cache.size());

        int present = 0;
        int missing = 0;
        for (int i = 0; i < 101; i++) {
            if (cache.get("key-" + i) != null) {
                present++;
            } else {
                missing++;
            }
        }

        assertEquals(100, present);
        assertEquals(1, missing);
    }

    @Test
    public void shouldKeepPutIfAbsentBoundedUnderConcurrentWrites() throws InterruptedException {
        for (int round = 0; round < 25; round++) {
            final int currentRound = round;
            PackageInfoCache cache = new PackageInfoCache(5_000L);
            for (int i = 0; i < 99; i++) {
                cache.put("seed-" + currentRound + '-' + i, packageInfo("seed-" + currentRound + '-' + i));
            }

            int writerCount = 24;
            CountDownLatch ready = new CountDownLatch(writerCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(writerCount);
            List<Throwable> failures = new CopyOnWriteArrayList<>();

            for (int i = 0; i < writerCount; i++) {
                final int writerIndex = i;
                Thread thread = new Thread(() -> {
                    ready.countDown();
                    try {
                        awaitLatch(start);
                        cache.putIfAbsent("concurrent-" + currentRound + '-' + writerIndex,
                                packageInfo("concurrent-" + currentRound + '-' + writerIndex));
                    } catch (Throwable exception) {
                        failures.add(exception);
                    } finally {
                        done.countDown();
                    }
                }, "package-info-cache-writer-" + currentRound + '-' + i);
                thread.start();
            }

            assertTrue("Writers did not become ready in round " + currentRound,
                    ready.await(CONCURRENT_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

            start.countDown();

            assertTrue("Writers did not finish in round " + currentRound,
                    done.await(CONCURRENT_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            assertTrue("Unexpected failure in round " + currentRound + ": " + failures,
                    failures.isEmpty());
            assertEquals("Cache size exceeded max capacity in round " + currentRound, 100, cache.size());
        }
    }

    @Test
    public void shouldExposeOnlyLiveEntriesInCollectionViews() {
        PackageInfoCache cache = new PackageInfoCache(SHORT_TTL_MILLIS);
        PackageInfo expired = packageInfo("expired");
        PackageInfo first = packageInfo("first");
        PackageInfo second = packageInfo("second");

        cache.put("expired", expired);
        awaitUntil(() -> cache.get("expired") == null);
        cache.put("first", first);
        cache.put("second", second);

        assertEquals(new HashSet<>(Arrays.asList("first", "second")), new HashSet<>(cache.keySet()));
        assertEquals(new HashSet<>(Arrays.asList(first, second)), new HashSet<>(cache.values()));

        Set<String> entryKeys = new HashSet<>();
        for (Map.Entry<String, PackageInfo> entry : cache.entrySet()) {
            entryKeys.add(entry.getKey());
            assertNotSame(expired, entry.getValue());
        }
        assertEquals(new HashSet<>(Arrays.asList("first", "second")), entryKeys);
    }

    @Test
    public void shouldRemoveUnderlyingEntriesViaValuesIterator() {
        PackageInfoCache cache = new PackageInfoCache(5_000L);
        PackageInfo first = packageInfo("first");
        PackageInfo second = packageInfo("second");
        cache.put("first", first);
        cache.put("second", second);

        Iterator<PackageInfo> iterator = cache.values().iterator();
        PackageInfo removed = iterator.next();
        iterator.remove();

        assertEquals(1, cache.size());
        assertFalse(cache.containsValue(removed));
        assertTrue(cache.containsValue(first) ^ cache.containsValue(second));
    }

    @Test
    public void shouldRemoveUnderlyingEntriesViaEntrySetIterator() {
        PackageInfoCache cache = new PackageInfoCache(5_000L);
        PackageInfo first = packageInfo("first");
        PackageInfo second = packageInfo("second");
        cache.put("first", first);
        cache.put("second", second);

        Iterator<Map.Entry<String, PackageInfo>> iterator = cache.entrySet().iterator();
        Map.Entry<String, PackageInfo> removed = iterator.next();
        iterator.remove();

        assertEquals(1, cache.size());
        assertFalse(cache.containsKey(removed.getKey()));
        assertTrue(cache.containsValue(first) ^ cache.containsValue(second));
    }

    private static PackageInfo packageInfo(String packageName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setGroupName("group");
        packageInfo.setPackageName(packageName);
        return packageInfo;
    }

    private interface Condition {
        boolean isMet();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(CONCURRENT_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                fail("Latch was not released within timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Thread was interrupted while waiting on latch");
        }
    }

    private static void awaitUntil(Condition condition) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_TIMEOUT_MILLIS);
        while (!condition.isMet()) {
            if (System.nanoTime() >= deadline) {
                fail("Condition was not met within timeout");
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }
}
