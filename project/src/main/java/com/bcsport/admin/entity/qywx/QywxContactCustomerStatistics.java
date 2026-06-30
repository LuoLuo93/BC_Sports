package com.bcsport.admin.entity.qywx;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 联系客户统计数据实体类
 */
@Data
public class QywxContactCustomerStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userid;

    private String statTime;

    private String chatCnt;

    private String messageCnt;

    private String avgReplyTime;

    private String replyPercentage;

    private String negativeFeedbackCnt;

    private String newApplyCnt;

    private String newContactCnt;

    private Date createTime;
}
