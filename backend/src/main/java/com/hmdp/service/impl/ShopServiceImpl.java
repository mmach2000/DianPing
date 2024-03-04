package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_PREFIX;

/**
 * <p>
 *  服务实现类
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
    public Result getByIdCached(Long id) {
        // 1. 从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_PREFIX + id);
        // 2. 如果redis中有，直接返回数据
        if (shopJson != null) {
            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
        }
        // 3. 如果redis中没有，从数据库中查询
        Shop shop = getById(id);
        // 4. 如果数据库中有，写入redis
        if (shop != null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_PREFIX + id, JSONUtil.toJsonStr(shop));
            return Result.ok(shop);
        }
        // 5. 如果数据库中没有，返回错误信息
        return Result.fail("商铺不存在");
    }
}
