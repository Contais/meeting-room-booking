package com.meetinghub.common.annotation;

import java.lang.annotation.*;

/**
 * 角色校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    String value();
}
