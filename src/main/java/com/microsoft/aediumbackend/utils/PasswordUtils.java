package com.microsoft.aediumbackend.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class PasswordUtils {
    public static String encodePassword(String password) {
        return DigestUtils.md5Hex(password);
    }
}
