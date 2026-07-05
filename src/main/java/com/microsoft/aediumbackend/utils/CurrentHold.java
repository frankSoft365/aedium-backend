package com.microsoft.aediumbackend.utils;

public class CurrentHold {
    // 这个工具对象存用户id
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 将员工id存入
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 取员工id
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 清理存储
     */
    public static void removeId() {
        threadLocal.remove();
    }
}
