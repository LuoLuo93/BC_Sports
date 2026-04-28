package com.bcsport.admin.entity.qywx;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 群发消息记录实体类
 */
@Data
public class VxMassMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String msgid;

    private String createType;

    private String creator;

    private String createTime;

    private String content;

    private Date insertDate;

}
