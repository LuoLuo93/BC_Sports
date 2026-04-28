package com.bcsport.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.ChannelNatureDTO;
import com.bcsport.admin.dto.ChannelNatureQueryDTO;
import com.bcsport.admin.entity.ChannelNature;
import com.bcsport.admin.vo.ChannelNatureVO;

import java.util.List;

/**
 * 渠道性质服务接口
 */
public interface ChannelNatureService extends IService<ChannelNature> {

    List<ChannelNatureVO> listByTree(ChannelNatureQueryDTO query);

    IPage<ChannelNatureVO> listByPage(ChannelNatureQueryDTO query);

    ChannelNatureVO getChannelNatureVOById(String id);

    void addChannelNature(ChannelNatureDTO channelNatureDTO);

    void updateChannelNature(ChannelNatureDTO channelNatureDTO);

    void deleteChannelNature(String id);

    Long getNextId();
}
