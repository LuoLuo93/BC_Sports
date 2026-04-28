package com.bcsport.admin.entity.qywx;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 群聊统计数据实体类
 */
@Data
public class VxGroupchatYesterday implements Serializable {

    private static final long serialVersionUID = 1L;

    private String owner;

    private String newChatCnt;

    private String chatTotal;

    private String chatHasMsg;

    private String newMemberCnt;

    private String memberTotal;

    private String memberHasMsg;

    private String msgTotal;

    private String starttime;

    private Date createtime;

}
