package com.bcsport.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 渠道性质视图对象
 */
@Data
public class ChannelNatureVO {

    private String id;

    private String parentId;

    private String natureName;

    private String natureCode;

    private Integer sort;

    private Integer status;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private String parentName;

    private String fullPath;

    private Boolean hasChildren;

    private List<ChannelNatureVO> children = new ArrayList<>();
}
