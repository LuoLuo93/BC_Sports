package com.bcsport.admin.config.xss;

/**
 * XSS 清洗工具类
 *
 * 使用 HTML 实体编码方式防御 XSS 攻击。
 * 将危险字符编码为实体（&amp; &lt; &gt;），使攻击者无法构造可执行的 HTML 标签。
 * 相比删除式清洗，编码方式可彻底防止嵌套绕过攻击。
 *
 * 注意：不编码引号（" 和 '），避免破坏 JSON 请求体解析。
 * 在 HTML 上下文中，只要 &lt; 和 &gt; 被编码，引号无法独立构成 XSS 向量。
 */
public class XssCleaner {

    /**
     * 清洗 XSS 攻击代码
     *
     * 通过 HTML 实体编码，将危险字符转换为安全形式：
     * - & → &amp;（必须先编码，防止双重编码）
     * - < → &lt;（阻止所有 HTML 标签构造）
     * - > → &gt;（阻止所有 HTML 标签闭合）
     *
     * @param value 原始值
     * @return 编码后的安全值
     */
    public static String clean(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 编码顺序很重要：& 必须最先，避免将后续编码产生的 & 再次编码
        value = value.replace("&", "&amp;");
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");

        return value;
    }
}
