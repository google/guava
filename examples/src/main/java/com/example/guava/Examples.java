package com.example.guava;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.RateLimiter;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Examples {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        immutableCollections();
        multimapExample();
        multisetExample();
        optionalExample();
        cacheExample();
        rateLimiterExample();
    }

    private static void immutableCollections() {
        ImmutableList<String> list = ImmutableList.of("apple", "banana", "cherry");
        ImmutableMap<String, Integer> counts = ImmutableMap.<String, Integer>builder()
                .put("apple", 1)
                .put("banana", 2)
                .build();
        System.out.println("ImmutableList: " + list);
        System.out.println("ImmutableMap: " + counts);
    }

    private static void multimapExample() {
        Multimap<String, String> mm = ArrayListMultimap.create();
        mm.put("fruit", "apple");
        mm.put("fruit", "banana");
        mm.put("color", "red");
        System.out.println("Multimap entries for 'fruit': " + mm.get("fruit"));
        System.out.println("All Multimap: " + mm);
    }

    private static void multisetExample() {
        HashMultiset<String> ms = HashMultiset.create();
        ms.add("apple");
        ms.add("apple");
        ms.add("banana");
        System.out.println("Multiset counts: " + ms.entrySet());
        System.out.println("Count of apple: " + ms.count("apple"));
    }

    private static void optionalExample() {
        Optional<String> maybe = Optional.ofNullable(null);
        System.out.println("Optional is present? " + maybe.isPresent());
        Optional<String> withValue = Optional.of("hello");
        System.out.println("Optional value: " + withValue.orElse("default"));
    }

    private static void cacheExample() throws ExecutionException {
        LoadingCache<String, String> cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) {
                        return "computed:" + key;
                    }
                });

        System.out.println("Cache get foo -> " + cache.get("foo"));
        System.out.println("Cache get foo again -> " + cache.get("foo"));
    }

    private static void rateLimiterExample() throws InterruptedException {
        RateLimiter limiter = RateLimiter.create(2.0); // 2 permits per second
        System.out.println("Acquiring 3 permits (should take ~1.5s total)...");
        long t0 = System.nanoTime();
        limiter.acquire(1);
        limiter.acquire(1);
        limiter.acquire(1);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("Elapsed ms for 3 acquires: " + elapsedMs);
    }
}
