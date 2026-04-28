package com.bcsport.admin.entity.qywx;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企业微信朋友圈实体
 */
@Data
public class QywxMoment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String momentId;

    private String creator;

    private String createTime;

    private String createType;

    private String content;

    private Date createTimeDate;

}
