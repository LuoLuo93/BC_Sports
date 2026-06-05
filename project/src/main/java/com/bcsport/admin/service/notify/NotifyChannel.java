package com.bcsport.admin.service.notify;

/**
 * 推送渠道接口
 *
 * 实现此接口即可被 NotifyManager 自动识别和调用
 */
public interface NotifyChannel {

    /**
     * 获取渠道名称
     */
    String getName();

    /**
     * 是否启用
     */
    boolean isEnabled();

    /**
     * 发送消息
     *
     * @param message 推送消息
     */
    void send(NotifyMessage message);
}
