package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.entity.ihr.EzrVipInfo;
import com.nascent.ecrp.opensdk.domain.customer.CardReceiveInfo;
import com.nascent.ecrp.opensdk.domain.customer.CustomerGradeUpdateInfo;
import com.nascent.ecrp.opensdk.domain.customer.CustomerSaveInfo;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * EZR 会员信息 → 南讯 CRM CustomerSaveInfo 转换器。
 * 逻辑保持与原 interfaceForYZ 项目一致。
 */
public final class EzrVipConverter {

    private EzrVipConverter() {}

    /**
     * 转换为南讯会员保存对象。
     * 规则：手机号为空 → 跳过（返回 null）。
     */
    public static CustomerSaveInfo toCustomerSaveInfo(EzrVipInfo source) {
        if (source == null || !StringUtils.hasText(source.getBindMobile())) {
            return null;
        }

        CustomerSaveInfo target = new CustomerSaveInfo();

        // 关键：nasOuid 直接使用手机号（业务约定，单表来源 NXVipInfo.bindMobile）
        target.setNasOuid(source.getBindMobile());

        // 姓名（缺省"匿名"）
        target.setCustomerName(StringUtils.hasText(source.getCustomerName()) ? source.getCustomerName() : "匿名");

        // 生日
        if (StringUtils.hasText(source.getBirthday())) {
            target.setBirthday(source.getBirthday());
        }

        // 会员领卡信息（platform=11 表示有赞渠道）
        target.setSubPlatform(11);

        // M5 修复：inMemberTime 只取决于 cardReceiveTime 是否存在，与 cardReceivePlatform 解耦。
        // 历史数据可能存在 platform 缺失但入会时间已知的情况，不应一并丢弃。
        if (source.getCardReceiveTime() != null) {
            target.setInMemberTime(source.getCardReceiveTime());

            // 仅当领卡渠道也存在时，才构建领卡明细列表
            if (source.getCardReceivePlatform() != null) {
                List<CardReceiveInfo> cardReceiveInfoList = new ArrayList<>();
                CardReceiveInfo cardReceiveInfo = new CardReceiveInfo();
                cardReceiveInfo.setCardReceivePlatform(43); // 43：有赞会员（原 42 为 POS 注册）
                cardReceiveInfo.setCardType(5);             // 5：手机卡
                cardReceiveInfo.setCardReceiveTime(source.getCardReceiveTime());
                cardReceiveInfo.setCardReceiveShopType(source.getCardReceiveShopType());
                cardReceiveInfoList.add(cardReceiveInfo);
                target.setCardReceiveInfoList(cardReceiveInfoList);
            }
        }

        if (source.getSgRecruitTime() != null) {
            target.setSgRecruitTime(source.getSgRecruitTime());
        }

        target.setMobile(source.getBindMobile());
        target.setBindMobile(source.getBindMobile());

        if (StringUtils.hasText(source.getCity())) {
            target.setCity(source.getCity());
        }

        if (source.getSex() != null) {
            target.setSex(convertSex(source.getSex()));
        }

        if (source.getUnionid() != null) {
            target.setUnionId(source.getUnionid());
        }

        if (source.getPlatform() != null) {
            target.setPlatform(source.getPlatform());
        }

        // 导购
        if (source.getOutSgExclusiveGuideId() != null) {
            target.setOutSgExclusiveGuideId(source.getOutSgExclusiveGuideId());
            target.setOutSgRecruitGuideId(source.getOutSgExclusiveGuideId());
        }

        // 店铺
        if (source.getOutSgExclusiveShopId() != null) {
            target.setOutSgExclusiveShopId(source.getOutSgExclusiveShopId());
            target.setOutSgRecruitShopId(source.getOutSgExclusiveShopId());
        }

        return target;
    }

    private static Integer convertSex(Integer sex) {
        if (sex == null) return null;
        switch (sex) {
            case 0: return 0;
            case 1: return 1;
            default: return 0;
        }
    }

    /**
     * 转换为南讯会员等级更新对象。
     * 规则：手机号或等级为空 → 跳过（返回 null）。
     * nasOuid 直接使用 bindMobile（业务约定）。
     */
    public static CustomerGradeUpdateInfo toGradeUpdateInfo(EzrVipInfo source) {
        if (source == null
            || !StringUtils.hasText(source.getBindMobile())
            || !StringUtils.hasText(source.getGrade())) {
            return null;
        }
        Integer level = mapGradeToLevel(source.getGrade());
        if (level == null) {
            return null;
        }
        CustomerGradeUpdateInfo info = new CustomerGradeUpdateInfo();
        info.setNasOuid(source.getBindMobile());
        info.setGrade(level);
        info.setRemark("历史等级初始化");
        return info;
    }

    /**
     * EZR 等级名称 → 南讯等级编号。
     */
    public static Integer mapGradeToLevel(String grade) {
        if (grade == null) return null;
        switch (grade) {
            case "BC新会员":  return 1;
            case "BC探索客":  return 2;
            case "BC旅行家":  return 3;
            case "BC冒险王":  return 4;
            default:         return null;
        }
    }
}
