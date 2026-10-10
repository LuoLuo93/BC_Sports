package com.bcsport.admin.task.qywx;

/**
 * 企微API业务错误(errcode != 0)：携带错误码，供 executeWithRetry 按码决定重试策略
 * (40014/42001刷新token重试、-1系统繁忙退避重试、其余直接抛出)
 */
public class QywxApiException extends RuntimeException {

    private final int errcode;

    public QywxApiException(int errcode, String message) {
        super(message);
        this.errcode = errcode;
    }

    public int getErrcode() {
        return errcode;
    }
}
