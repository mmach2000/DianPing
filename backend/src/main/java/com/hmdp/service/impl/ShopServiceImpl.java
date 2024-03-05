package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result cachedGetById(Long id) {
        // 1. 从 Redis 中查询
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_PREFIX + id);
        // 2. 如果 Redis 中有，直接返回数据（或错误信息）
        if (shopJson != null) {
            if (shopJson.isEmpty()) {
                return Result.fail("商铺不存在");
            }
            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
        }
        // 3. 如果 Redis 中没有，从数据库中查询
        Shop shop = getById(id);
        // 4. 如果数据库中有，写入 Redis
        if (shop != null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_PREFIX + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES);
            return Result.ok(shop);
        }
        // 5. 如果数据库中没有，将空值写入 Redis，返回错误信息
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_PREFIX + id, "", CACHE_SHOP_NULL_TTL_MINUTES, TimeUnit.MINUTES);
        return Result.fail("商铺不存在");
    }

    @Transactional
    @Override
    public Result cachedUpdateById(Shop shop) {
        // 1. 更新数据库中的店铺数据
        boolean updateSQLResult = this.updateById(shop);
        if (!updateSQLResult) {
            // 缓存更新失败，抛出异常，事务回滚
            throw new RuntimeException("数据库更新失败");
        }
        // 2. 删除缓存
        Boolean deleteCacheResult = stringRedisTemplate.delete(CACHE_SHOP_PREFIX + shop.getId());
        if (deleteCacheResult == null || !deleteCacheResult) {
            // 缓存删除失败，抛出异常，事务回滚
            throw new RuntimeException("缓存删除失败");
        }
        return Result.ok();
    }
}
