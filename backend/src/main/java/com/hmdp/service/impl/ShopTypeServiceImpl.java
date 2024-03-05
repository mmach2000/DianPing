package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.StringRedisManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL_MINUTES;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisManager stringRedisManager;

    private String getTypeListJSON() {
        List<ShopType> typeList = query().orderByAsc("sort").list();
        return JSONUtil.toJsonStr(typeList);
    }

    @Override
    public Result queryTypeListCached() {
        String got = stringRedisManager.getByKeyCached(CACHE_SHOP_TYPE_KEY, this::getTypeListJSON, CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES);
        if (StrUtil.isBlank(got)) {
            return Result.fail("商铺类型不存在");
        }
        return Result.ok(JSONUtil.toList(JSONUtil.parseArray(got), ShopType.class));
    }
}
