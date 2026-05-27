package com.bcsport.admin.entity.ihr;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("NXCRM_TAG_INFO")
public class NxcrmTagInfo {

    @TableId("TAG_CODE")
    private String tagCode;

    @TableField("TAG_NAME")
    private String tagName;

    @TableField("TAG_FOLDER_ID")
    private String tagFolderId;

    @TableField("PARENT_FOLDER_ID")
    private String parentFolderId;

    @TableField("TAG_FOLDER_NAME")
    private String tagFolderName;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("HAS_VALUE")
    private Integer hasValue;

    @TableField("CREATE_TYPE")
    private String createType;

    @TableField("VALUE_DATA_TYPE")
    private String valueDataType;

    @TableField("VALUE_UNIT")
    private String valueUnit;

    @TableField("DISPLAY_ORDER")
    private Integer displayOrder;

    @TableField("IS_SYSTEM")
    private Integer isSystem;

    @TableField("TAG_OBJECT_TYPE")
    private Integer tagObjectType;

    @TableField("TAG_MASTER_TYPE")
    private Integer tagMasterType;

    @TableField("GROUP_ID")
    private Long groupId;

    @TableField("ENTITY_CODE")
    private Integer entityCode;

    @TableField("TAG_CREATE_TIME")
    private LocalDateTime tagCreateTime;

    @TableField("TAG_UPDATE_TIME")
    private LocalDateTime tagUpdateTime;

    @TableField("SYNC_TIME")
    private LocalDateTime syncTime;

    @TableField(exist = false)
    private List<NxcrmTagValue> tagValueList;
}
