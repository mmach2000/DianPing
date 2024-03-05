package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IShopTypeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商铺类别控制器
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 查询商铺类别列表
     *
     * @return 商铺类别列表
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeListCached();
    }
}
