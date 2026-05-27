package com.bcsport.admin.service.nxcrm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.nxcrm.NxcrmMemberTagDetailQueryDTO;
import com.bcsport.admin.dto.nxcrm.NxcrmTagListQueryDTO;
import com.bcsport.admin.entity.ihr.NxcrmMemberTagDetail;
import com.bcsport.admin.entity.ihr.NxcrmTagInfo;
import com.bcsport.admin.entity.ihr.NxcrmTagValue;
import com.bcsport.admin.ihrmapper.NxcrmMemberTagDetailMapper;
import com.bcsport.admin.ihrmapper.NxcrmTagInfoMapper;
import com.bcsport.admin.ihrmapper.NxcrmTagValueMapper;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.NxcrmMemberTagDetailVO;
import com.bcsport.admin.vo.NxcrmTagInfoVO;
import com.bcsport.admin.vo.NxcrmTagValueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NxcrmMemberTagService {

    @Autowired
    private NxcrmMemberTagDetailMapper memberTagDetailMapper;

    @Autowired
    private NxcrmTagInfoMapper tagInfoMapper;

    @Autowired
    private NxcrmTagValueMapper tagValueMapper;

    public PageResult<NxcrmMemberTagDetailVO> pageMemberTags(PageQuery pageQuery, NxcrmMemberTagDetailQueryDTO queryDTO) {
        LambdaQueryWrapper<NxcrmMemberTagDetail> wrapper = new LambdaQueryWrapper<NxcrmMemberTagDetail>()
            .orderByDesc(NxcrmMemberTagDetail::getSyncTime);
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getNasOuid())) {
                wrapper.like(NxcrmMemberTagDetail::getNasOuid, queryDTO.getNasOuid());
            }
            if (StringUtils.hasText(queryDTO.getTagName())) {
                wrapper.like(NxcrmMemberTagDetail::getTagName, queryDTO.getTagName());
            }
        }
        Page<NxcrmMemberTagDetail> page = memberTagDetailMapper.selectPage(pageQuery.toPage(), wrapper);
        return BeanCopyUtils.copyPage(PageResult.of(page), NxcrmMemberTagDetailVO.class);
    }

    public PageResult<NxcrmTagInfoVO> pageTagList(PageQuery pageQuery, NxcrmTagListQueryDTO queryDTO) {
        LambdaQueryWrapper<NxcrmTagInfo> wrapper = new LambdaQueryWrapper<NxcrmTagInfo>()
            .orderByAsc(NxcrmTagInfo::getDisplayOrder);
        if (queryDTO != null && StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword();
            wrapper.and(w -> w.like(NxcrmTagInfo::getTagName, keyword)
                .or().like(NxcrmTagInfo::getTagCode, keyword)
                .or().like(NxcrmTagInfo::getTagFolderName, keyword));
        }
        Page<NxcrmTagInfo> page = tagInfoMapper.selectPage(pageQuery.toPage(), wrapper);
        PageResult<NxcrmTagInfoVO> result = BeanCopyUtils.copyPage(PageResult.of(page), NxcrmTagInfoVO.class);

        // 填充标签值列表
        List<NxcrmTagInfoVO> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            List<String> tagCodes = records.stream()
                .map(NxcrmTagInfoVO::getTagCode).distinct().collect(Collectors.toList());
            Map<String, List<NxcrmTagValue>> valueMap = tagValueMapper.selectList(
                new LambdaQueryWrapper<NxcrmTagValue>().in(NxcrmTagValue::getTagCode, tagCodes)
                .orderByAsc(NxcrmTagValue::getDisplayOrder)
            ).stream().collect(Collectors.groupingBy(NxcrmTagValue::getTagCode));

            for (NxcrmTagInfoVO vo : records) {
                List<NxcrmTagValue> values = valueMap.get(vo.getTagCode());
                if (values != null) {
                    vo.setTagValueList(BeanCopyUtils.copyList(values, NxcrmTagValueVO.class));
                }
            }
        }
        return result;
    }
}
