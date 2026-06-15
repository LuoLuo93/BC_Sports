package com.bcsport.admin.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("print_agent")
public class PrintAgent {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("agent_id")
    private String agentId;

    @TableField("agent_name")
    private String agentName;

    @TableField("printers")
    private String printers;

    @TableField("status")
    private Integer status;

    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("ip_address")
    private String ipAddress;
}
