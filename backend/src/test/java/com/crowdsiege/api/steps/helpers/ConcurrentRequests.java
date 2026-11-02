package com.crowdsiege.api.steps.helpers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;

/**
 * Fires requests at the same moment (latch-released threads) to exercise concurrency rules.
 */
public final class ConcurrentRequests {

    private ConcurrentRequests() {
    }

    public static List<ResponseEntity<String>> runTogether(List<Supplier<ResponseEntity<String>>> requests) {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<CompletableFuture<ResponseEntity<String>>> futures = requests.stream()
                    .map(request -> CompletableFuture.supplyAsync(() -> {
                        awaitQuietly(start);
                        return request.get();
                    }, executor))
                    .toList();
            start.countDown();
            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdown();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
