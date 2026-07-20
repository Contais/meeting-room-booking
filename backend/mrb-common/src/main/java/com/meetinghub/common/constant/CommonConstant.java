package com.meetinghub.common.constant;

/**
 * 通用常量
 */
public final class CommonConstant {

    private CommonConstant() {
    }

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final Integer STATUS_DISABLED = 0;
    public static final Integer STATUS_ENABLED = 1;

    public static final Integer DELETED_NO = 0;
    public static final Integer DELETED_YES = 1;
}
