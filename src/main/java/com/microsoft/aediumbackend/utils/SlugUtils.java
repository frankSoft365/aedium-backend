package com.microsoft.aediumbackend.utils;

public class SlugUtils {

    public static String makeSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // 1. 统一转小写，去掉首尾空格
        String slug = name.toLowerCase().trim();

        // 2. 核心正则：[^a-z0-9\u4e00-\u9fa5]+
        // 意思是：除了 小写英文(a-z)、数字(0-9)、中文(\u4e00-\u9fa5)，其他杂七杂八的符号统统变成连字符 "-"
        slug = slug.replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");

        // 3. 把连续的多个连字符（如 "---"）合并成一个 "-"
        slug = slug.replaceAll("-+", "-");

        // 4. 去掉开头和结尾由于标点符号产生的残余连字符
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }
}