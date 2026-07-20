package com.tulingshop.common.constant;

/**
 * Redis Key 常量，统一前缀 mrb:
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    public static final String PREFIX = "mrb:";

    // 用户相关
    public static final String USER_TOKEN = PREFIX + "user:token:";

    // 会议室缓存
    public static final String MEETING_ROOM_CACHE = PREFIX + "meeting:room:";

    // 预约锁
    public static final String RESERVATION_LOCK = PREFIX + "reservation:lock:";

    // 预约幂等
    public static final String RESERVATION_IDEMPOTENT = PREFIX + "reservation:idempotent:";
}
