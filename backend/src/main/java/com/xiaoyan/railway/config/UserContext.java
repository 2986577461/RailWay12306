package com.xiaoyan.railway.config;

/**
 * Current authenticated user for the request thread, populated by {@link AuthInterceptor}
 * from the JWT and cleared in {@code afterCompletion}. Never use across threads.
 */
public final class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public record LoginUser(Long userId, String phone) { }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private UserContext() { }
}
