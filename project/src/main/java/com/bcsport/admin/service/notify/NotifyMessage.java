package com.bcsport.admin.service.notify;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 推送消息模型
 */
@Data
@Builder
public class NotifyMessage {

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型 */
    private NotifyType type;

    /** 扩展参数 */
    private Map<String, Object> extras;
}
