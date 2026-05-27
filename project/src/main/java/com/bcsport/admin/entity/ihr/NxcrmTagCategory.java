package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nxcrm_tag_category")
public class NxcrmTagCategory {

    @TableId("ID")
    private String id;

    @TableField("PID")
    private String pid;

    @TableField("NAME")
    private String name;

    @TableField("PID_FULL_PATH")
    private String pidFullPath;

    @TableField("CREATE_TIME")
    private LocalDateTime createTime;

    @TableField("UPDATE_TIME")
    private LocalDateTime updateTime;
}
