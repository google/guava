package com.john.doe.cache;

import java.lang.instrument.Instrumentation;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.*;
import org.junit.Test;

import com.john.doe.entity.User;

/**
 * Created by joker on 2022/12/6.
 */
public class CacheExpireTest {

    private static Random rand = new Random();

    @Test
    // 创建缓存实例
    public void createCacheInstance() {
        LoadingCache<String, User> cache = CacheBuilder.newBuilder().initialCapacity(1000) // 初始容量
                .maximumSize(10000L) // 最大容量
                .expireAfterWrite(30L, TimeUnit.MINUTES) // 写入过期时间
                .concurrencyLevel(8) // 最大并发写线程数
                .refreshAfterWrite(1L, TimeUnit.MINUTES) // 自动刷新数据时间
                .recordStats() // 开启缓存指标统计
                .build(new CacheLoader<String, User>() {
                    @Override
                    public User load(String userId) throws Exception {
                        return getUserById(userId);
                    }
                });
        System.out.println(cache.size());
    }

    @Test
    // 按创建时间过期
    public void expireByCreateTime() {
        Cache<String, User> cache = CacheBuilder.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).build();
        System.out.println(cache.size());
    }

    @Test
    // 按访问时间过期
    public void expireByVisitTime() {
        Cache<String, User> cache = CacheBuilder.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES).build();
        System.out.println(cache.size());
    }

    @Test
    // 限制缓存条数
    public void restrictCacheBySize() {
        Cache<String, User> cache = CacheBuilder.newBuilder().maximumSize(10000L).build();
        System.out.println(cache.size());
    }

    @Test
    // 限制缓存权重
    public void restrictCacheByWeight() {
        Cache<String, User> cache = CacheBuilder.newBuilder().maximumWeight(10000L)
                .weigher((k, v) -> (int) Math.ceil(getObjectSize(v) / 1024.0)).build();
    }

    @Test
    // 集成数据源-穿透型缓存
    public void integrateDataSource() {
        User user = findUser(CacheBuilder.newBuilder().maximumSize(1000).build(),
                UUID.randomUUID().toString().substring(4));
        System.out.println(user);
    }

    @Test
    // 通过 CacheLoader 方式创建缓存，同样可以集成数据源
    public void createCacheByCacheLoader() {
        LoadingCache<String, User> cache = CacheBuilder.newBuilder().build(new CacheLoader<String, User>() {
            @Override
            public User load(String userId) throws Exception {
                System.out.println(userId + " 用户缓存不存在，尝试回源查找并返回");
                return getUserById(userId);
            }
        });
        try {
            User user = cache.get(UUID.randomUUID().toString().substring(4));
            System.out.println(user);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    // 缓存指标统计
    public void cacheMonitorStats() {
        LoadingCache<String, User> cache =
                CacheBuilder.newBuilder().recordStats().build(new CacheLoader<String, User>() {
                    @Override
                    public User load(String userId) throws Exception {
                        System.out.println(userId + " 用户缓存不存在，尝试回源查找并返回");
                        User user = getUserById(userId);
                        if (user == null) {
                            System.out.println(userId + " 用户不存在");
                        }
                        return user;
                    }
                });
        // 获取统计指标
        CacheStats stats = cache.stats();
        System.out.println(stats);
    }

    /**
     * 获取对象大小-fake
     * 
     * @param obj
     * @return
     */
    private Long getObjectSize(Object obj) {
        return new Random().nextLong();
    }

    private User findUser(Cache<String, User> cache, String userId) {
        try {
            return cache.get(userId, () -> {
                System.out.println(userId + " 用户缓存不存在，尝试回源查找并返回");
                return getUserById(userId);
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private User getUserById(String userId) {
        return new User(UUID.randomUUID().toString().substring(4), rand.nextInt(100));
    }
}
