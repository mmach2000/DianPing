package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_PREFIX = "login:phone:";
    public static final String LOGIN_TOKEN_PREFIX = "login:token:";
    public static final String CACHE_SHOP_PREFIX = "cache:shop:";

    public static final String LOCK_SHOP_PREFIX = "lock:shop:";

    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
    public static final String BLOG_LIKED_PREFIX = "blog:liked:";
    public static final String FEED_PREFIX = "feed:";
    public static final String SHOP_GEO_PREFIX = "shop:geo:";
    public static final String USER_SIGN_PREFIX = "sign:";

    public static final Long LOGIN_CODE_TTL_MINUTES = 2L;
    public static final Long LOGIN_TOKEN_TTL_MINUTES = 30L;
    public static final Long LOCK_SHOP_TTL_MINUTES = 10L;
}
