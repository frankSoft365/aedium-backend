package com.microsoft.aediumbackend.utils;

import java.util.regex.Pattern;

public class RegexUtils {
    private static final String PASSWORD_VALID_REGEX = "^[a-zA-Z0-9!@#$%*_+\\-=]+$";
    private static final String USERACCOUNT_VALID_REGEX = "^[a-zA-Z0-9_-]+$";
    public static final String PHONE_VALID_REGEX = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";
    public static final String EMAIL_VALID_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public static Boolean isValidPassword(String toBeVerifiedPassword) {
        return Pattern.matches(PASSWORD_VALID_REGEX, toBeVerifiedPassword);
    }

    public static Boolean isValidUserAccount(String toBeVerifiedUserAccount) {
        return Pattern.matches(USERACCOUNT_VALID_REGEX, toBeVerifiedUserAccount);
    }

    public static Boolean isValidPhone(String toBeVerifiedPhone) {
        return Pattern.matches(PHONE_VALID_REGEX, toBeVerifiedPhone);
    }

    public static Boolean isValidEmail(String toBeVerifiedEmail) {
        return Pattern.matches(EMAIL_VALID_REGEX, toBeVerifiedEmail);
    }
}
