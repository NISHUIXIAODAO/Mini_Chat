package com.easychat.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SessionIdUtils {
    /***
     * 生成 SessionId
     * @param userId1
     * @param userId2
     * @return
     */
    public static String generateSessionId(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("userId cannot be null when generating sessionId");
        }
        Integer[] userIds = {userId1, userId2};
        Arrays.sort(userIds); // 确保顺序一致
        return md5(userIds[0] + "_" + userIds[1]);
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString(); // MD5 生成固定 32 位字符串
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found!", e);
        }
    }
}
