package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL_MINUTES;
import static com.hmdp.utils.RedisConstants.LOCK_UPDATE_PREFIX;

@Component
public class StringRedisManager {
    /**
     * 缓存重建线程池
     */
    public static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将数据加入Redis，并设置有效期
     *
     * @param key     key
     * @param value   value
     * @param timeout 有效期
     * @param unit    有效期的时间单位
     */
    public void set(String key, String value, Long timeout, TimeUnit unit) {
        // 加上随机时间，防止缓存雪崩
        Duration realTimeout = Duration.ofSeconds(unit.toSeconds(timeout));
        realTimeout = realTimeout.plusSeconds(RandomUtil.randomLong(100));
        stringRedisTemplate.opsForValue().set(key, value, realTimeout);
    }

    /**
     * 将数据加入Redis，并设置逻辑过期时间
     *
     * @param key     key
     * @param value   value
     * @param timeout 有效期
     * @param unit    有效期的时间单位
     */
    public void setWithLogicalExpire(String key, String value, Long timeout, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        // unit.toSeconds()是为了确保计时单位是秒
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 根据键查询数据（处理缓存穿透）
     *
     * @param key        key
     * @param dbFallback 根据id查询数据的函数
     * @param timeout    有效期
     * @param unit       有效期的时间单位
     * @return 查询到的数据
     */
    public String getByKeyCached(String key, Supplier<String> dbFallback, Long timeout, TimeUnit unit) {
        // 1. 从 Redis 中查询数据
        String data = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断缓存是否命中
        if (data != null) {
            // 3. 缓存命中空对象，直接返回失败信息
            if (data.isEmpty()) {
                return null;
            }
            // 4. 缓存命中，直接返回数据
            return data;
        }

        // 5. 缓存未命中，从数据库中查询店铺数据
        data = dbFallback.get();
        if (data != null) {
            // 6. 数据库中存在，重建缓存，并返回数据
            this.set(key, data, timeout, unit);
            return data;
        }
        // 7. 数据库中不存在，缓存空对象（解决缓存穿透），返回失败信息
        this.set(key, "", CACHE_NULL_TTL_MINUTES, TimeUnit.SECONDS);
        return null;
    }

    /**
     * 根据id查询数据（处理缓存穿透）
     *
     * @param keyPrefix  key前缀
     * @param id         查询id
     * @param dbFallback 根据id查询数据的函数
     * @param timeout    有效期
     * @param unit       有效期的时间单位
     * @param <ID>       id类型
     * @return 查询到的数据
     */
    public <ID> String getByIdCached(String keyPrefix, ID id, Function<ID, String> dbFallback, Long timeout, TimeUnit unit) {
        return this.getByKeyCached(keyPrefix + id, () -> dbFallback.apply(id), timeout, unit);
    }


    public String getHotDataByKeyCached(String key, Supplier<String> dbFallback, Long timeout, TimeUnit unit) {
        // 1、从Redis中查询店铺数据，并判断缓存是否命中
        String redisDataStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(redisDataStr)) {
            // 1.1 缓存未命中，直接返回失败信息
            return null;
        }
        // 1.2 缓存命中，将JSON字符串反序列化未对象，并判断缓存数据是否逻辑过期
        RedisData redisData = JSONUtil.toBean(redisDataStr, RedisData.class);
        String data = redisData.getData();
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 当前缓存数据未过期，直接返回
            return data;
        }

        // 2、缓存数据已过期，获取互斥锁，并且重建缓存
        String lockKey = LOCK_UPDATE_PREFIX + key;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 获取锁成功，开启一个子线程去重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    String got = dbFallback.get();
                    // 将查询到的数据保存到Redis
                    this.setWithLogicalExpire(key, got, timeout, unit);
                } finally {
                    unlock(lockKey);
                }
            });
        }

        // 3、获取锁失败，再次查询缓存，判断缓存是否重建（这里双检是有必要的）
        redisDataStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(redisDataStr)) {
            // 3.1 缓存未命中，直接返回失败信息
            return null;
        }
        // 3.2 缓存命中，将JSON字符串反序列化未对象，并判断缓存数据是否逻辑过期
        redisData = JSONUtil.toBean(redisDataStr, RedisData.class);
        data = redisData.getData();
        expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 当前缓存数据未过期，直接返回
            return data;
        }

        // 4、返回过期数据
        return data;
    }

    /**
     * 根据id查询数据（处理缓存击穿）
     *
     * @param keyPrefix  key前缀
     * @param id         查询id
     * @param dbFallback 根据id查询数据的函数
     * @param timeout    有效期
     * @param unit       有效期的时间单位
     * @param <ID>       id类型
     * @return 查询到的数据
     */
    public <ID> String getHotDataByIdCached(String keyPrefix, ID id,
                                            Function<ID, String> dbFallback, Long timeout, TimeUnit unit) {
        return getHotDataByKeyCached(keyPrefix + id, () -> dbFallback.apply(id), timeout, unit);
    }

    /**
     * 获取锁
     *
     * @param key 锁的key
     * @return 是否获取锁成功
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // 拆箱要判空，防止NPE
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key 锁的key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存的key
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return BooleanUtil.isTrue(stringRedisTemplate.delete(key));
    }
}
