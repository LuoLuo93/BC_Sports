package com.bcsport.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.ChannelTypeDTO;
import com.bcsport.admin.dto.ChannelTypeQueryDTO;
import com.bcsport.admin.entity.ChannelType;
import com.bcsport.admin.vo.ChannelTypeVO;

import java.util.List;

/**
 * 渠道类型服务接口
 */
public interface ChannelTypeService extends IService<ChannelType> {

    List<ChannelTypeVO> listByTree(ChannelTypeQueryDTO query);

    IPage<ChannelTypeVO> listByPage(ChannelTypeQueryDTO query);

    ChannelTypeVO getChannelTypeVOById(String id);

    void addChannelType(ChannelTypeDTO channelTypeDTO);

    void updateChannelType(ChannelTypeDTO channelTypeDTO);

    void deleteChannelType(String id);

    Long getNextId();
}
