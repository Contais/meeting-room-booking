package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 文件业务类型枚举
 * <p>
 * 决定文件在存储中的根目录分段，便于按业务隔离与清理。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum FileBizType {

    AVATAR("avatar", "用户头像"),
    ROOM_IMAGE("room", "会议室图片");

    private final String path;
    private final String desc;

    /**
     * 忽略大小写匹配，匹配不到返回 null。
     */
    public static FileBizType of(String name) {
        if (name == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
