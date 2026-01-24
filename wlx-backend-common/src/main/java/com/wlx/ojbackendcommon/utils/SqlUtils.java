package com.wlx.ojbackendcommon.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * SQL 工具
 *
 */
public class SqlUtils {

    /**
     * 校验排序字段是否合法（防止 SQL 注入）
     *
     * @param sortField
     * @return
     */
    public static boolean validSortField(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return false;
        }
        return !StringUtils.containsAny(sortField, "=", "(", ")", " ");
    }

    /**
     * 将驼峰命名转换为下划线命名
     *
     * @param camelCase 驼峰命名的字符串
     * @return 下划线命名的字符串
     */
    public static String camelToSnake(String camelCase) {
        if (StringUtils.isBlank(camelCase)) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
