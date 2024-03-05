package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.StringRedisManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_PREFIX;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL_MINUTES;

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
    private StringRedisManager stringRedisManager;

    private String getShopJSONById(Long id) {
        return JSONUtil.toJsonStr(getById(id));
    }

    @Override
    public Result cachedGetById(Long id) {
        String shopJson = stringRedisManager.getByIdCached(CACHE_SHOP_PREFIX, id, this::getShopJSONById, CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES);
        if (StrUtil.isBlank(shopJson)) {
            return Result.fail("商铺不存在");
        }
        return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
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
        boolean deleteCacheResult = stringRedisManager.delete(CACHE_SHOP_PREFIX + shop.getId());
        if (!deleteCacheResult) {
            // 缓存删除失败，抛出异常，事务回滚
            throw new RuntimeException("缓存删除失败");
        }
        return Result.ok();
    }
}
