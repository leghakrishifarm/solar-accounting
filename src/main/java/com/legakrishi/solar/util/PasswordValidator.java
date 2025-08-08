package com.legakrishi.solar.util;

public class PasswordValidator {
    public static boolean isValid(String password) {
        if (password == null) return false;
        // Regex: at least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(regex);
    }
}