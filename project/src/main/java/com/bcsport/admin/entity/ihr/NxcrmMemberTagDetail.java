package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("NXCRM_MEMBER_TAG_DETAIL")
public class NxcrmMemberTagDetail {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @TableField("NAS_OUID")
    private String nasOuid;

    @TableField("TAG_NAME")
    private String tagName;

    @TableField("TAG_VALUE_NAME")
    private String tagValueName;

    @TableField("SYNC_TIME")
    private LocalDateTime syncTime;
}
