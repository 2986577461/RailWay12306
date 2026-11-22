package com.xiaoyan.compute.config;

public final class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() { }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long userId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
