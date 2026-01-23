package com.wlx.ojbackendauthservice.utils;

import com.wlx.ojbackendmodel.model.dto.user.WeChatRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class WeChatUtils {
    /*微信公众平台-测试公众号-接口配置信息-Token*/
    private static String weChatToken = "wxtoken";

    /*
    * 核实微信请求
    * */
    public static boolean checkWeChat(WeChatRequest weChatRequestDTO) throws NoSuchAlgorithmException{
        // 将 token, timestamp, nonce 三个参数进行字典序排序
        String[] array = { weChatToken, weChatRequestDTO.getTimestamp(), weChatRequestDTO.getNonce() };
        Arrays.sort(array);
        // 将三个参数字符串拼接成一个字符串
        String str = String.join("", array);
        // 进行 SHA-1 加密
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] digest = messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));
        // 将二进制字节数组 digest 逐个转为 16 进制字符串，拼接成最终字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String s = Integer.toHexString(b & 0xff);
            // 遇到长度 == 1 的数据时，前面补 0
            if (s.length() == 1) {
                hexString.append('0');
            }
            hexString.append(s);
        }
        // 开发者获得加密后的字符串可与 signature 对比，标识该请求来源于微信
        return weChatRequestDTO.getSignature() != null && weChatRequestDTO.getSignature().equals(hexString.toString());
    }
}
