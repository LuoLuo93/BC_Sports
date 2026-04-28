package com.bcsport.admin.task.ihr;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.dto.ihr.*;
import com.bcsport.admin.entity.ihr.*;
import lombok.extern.slf4j.Slf4j;

/**
 * IHR 实体转换器 (带长度检查)
 */
@Slf4j
public class IhrEntityConverter {

    private IhrEntityConverter() {
    }

    private static String truncate(String value, int maxLength, String fieldName) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }
        if (value.length() > maxLength) {
            log.warn("字段 {} 超长: 实际长度={}, 最大允许={}, 值前50字符={}",
                    fieldName, value.length(), maxLength,
                    value.substring(0, Math.min(50, value.length())));
            return value.substring(0, maxLength);
        }
        return value;
    }

    public static IhrEmployeeDetail toDetailEntity(IhrEmployeeDetailDTO dto) {
        if (dto == null) {
            return null;
        }
        IhrEmployeeDetail e = new IhrEmployeeDetail();
        e.setId(truncate(dto.getId(), 50, "id"));
        e.setAge(truncate(dto.getAge(), 10, "age"));
        e.setCreatedDate(truncate(dto.getCreatedDate(), 20, "createdDate"));
        e.setCompanyId(truncate(dto.getCompanyId(), 50, "companyId"));
        e.setQqNo(truncate(dto.getQqNo(), 50, "qqNo"));
        e.setWechatNo(truncate(dto.getWeChatNo(), 50, "weChatNo"));
        e.setStaffNo(truncate(dto.getStaffNo(), 50, "staffNo"));
        e.setStaffName(truncate(dto.getStaffName(), 100, "staffName"));
        e.setIdCardNo(truncate(dto.getIdCardNo(), 50, "idCardNo"));
        e.setIdCardType(truncate(dto.getIdCardType(), 20, "idCardType"));
        e.setBirthday(truncate(dto.getBirthday(), 20, "birthday"));
        e.setSex(truncate(dto.getSex(), 10, "sex"));
        e.setMobileNo(truncate(dto.getMobileNo(), 20, "mobileNo"));
        e.setEmail(truncate(dto.getEmail(), 100, "email"));
        e.setWorkPhone(truncate(dto.getWorkPhone(), 20, "workPhone"));
        e.setWorkEmail(truncate(dto.getWorkEmail(), 100, "workEmail"));
        e.setMarryStatus(truncate(dto.getMarryStatus(), 50, "marryStatus"));
        e.setHighestEducation(truncate(dto.getHighestEducation(), 50, "highestEducation"));
        e.setNationality(truncate(dto.getNationality(), 50, "nationality"));
        e.setNativePlace(truncate(dto.getNativePlace(), 100, "nativePlace"));
        e.setNativePlaceProvinceCode(truncate(dto.getNativePlaceProvinceCode(), 20, "nativePlaceProvinceCode"));
        e.setNativePlaceCityCode(truncate(dto.getNativePlaceCityCode(), 20, "nativePlaceCityCode"));
        e.setPoliticalStatus(truncate(dto.getPoliticalStatus(), 50, "politicalStatus"));
        e.setNickName(truncate(dto.getNickName(), 100, "nickName"));
        e.setBloodType(truncate(dto.getBloodType(), 10, "bloodType"));
        e.setSpouseName(truncate(dto.getSpouseName(), 50, "spouseName"));
        e.setChildName(truncate(dto.getChildName(), 50, "childName"));
        e.setEmergencyContactName(truncate(dto.getEmergencyContactName(), 50, "emergencyContactName"));
        e.setEmergencyContactMobile(truncate(dto.getEmergencyContactMobile(), 20, "emergencyContactMobile"));
        e.setStaffStatus(truncate(dto.getStaffStatus(), 20, "staffStatus"));
        e.setStaffType(truncate(dto.getStaffType(), 20, "staffType"));
        e.setLeaveDate(truncate(dto.getLeaveDate(), 20, "leaveDate"));
        e.setProbationEndDate(truncate(dto.getProbationEndDate(), 20, "probationEndDate"));
        e.setEnrollInDate(truncate(dto.getEnrollInDate(), 20, "enrollInDate"));
        e.setProbationLength(truncate(dto.getProbationLength(), 20, "probationLength"));
        e.setContractBeginDate(truncate(dto.getContractBeginDate(), 20, "contractBeginDate"));
        e.setContractEndDate(truncate(dto.getContractEndDate(), 20, "contractEndDate"));
        e.setPositiveDate(truncate(dto.getPositiveDate(), 20, "positiveDate"));
        e.setHukouType(truncate(dto.getHukouType(), 20, "hukouType"));
        e.setHukouAddress(truncate(dto.getHukouAddress(), 200, "hukouAddress"));
        e.setHukouProvinceCode(truncate(dto.getHukouProvinceCode(), 20, "hukouProvinceCode"));
        e.setHukouCityCode(truncate(dto.getHukouCityCode(), 20, "hukouCityCode"));
        e.setLivingAddress(truncate(dto.getLivingAddress(), 200, "livingAddress"));
        e.setLivingProvinceCode(truncate(dto.getLivingProvinceCode(), 20, "livingProvinceCode"));
        e.setLivingCityCode(truncate(dto.getLivingCityCode(), 20, "livingCityCode"));
        e.setDepartmentId(truncate(dto.getDepartmentId(), 50, "departmentId"));
        e.setDepartmentName(truncate(dto.getDepartmentName(), 200, "departmentName"));
        e.setCorporationName(truncate(dto.getCorporationName(), 200, "corporationName"));
        e.setCorporationId(truncate(dto.getCorporationId(), 50, "corporationId"));
        e.setPositionName(truncate(dto.getPositionName(), 100, "positionName"));
        e.setPositionId(truncate(dto.getPositionId(), 50, "positionId"));
        e.setJobTitleName(truncate(dto.getJobTitleName(), 100, "jobTitleName"));
        e.setJobTitleId(truncate(dto.getJobTitleId(), 50, "jobTitleId"));
        e.setSupervisorName(truncate(dto.getSupervisorName(), 100, "supervisorName"));
        e.setSupervisorId(truncate(dto.getSupervisorId(), 50, "supervisorId"));
        e.setPositionLevelId(truncate(dto.getPositionLevelId(), 50, "positionLevelId"));
        e.setPositionLevelName(truncate(dto.getPositionLevelName(), 100, "positionLevelName"));
        e.setLastName(truncate(dto.getLastName(), 50, "lastName"));
        e.setFirstName(truncate(dto.getFirstName(), 50, "firstName"));
        e.setLegalName(truncate(dto.getLegalName(), 100, "legalName"));
        e.setWorkPlace(truncate(dto.getWorkPlace(), 200, "workPlace"));
        e.setContractType(truncate(dto.getContractType(), 50, "contractType"));
        e.setIsDeleted(truncate(dto.getIsDeleted(), 10, "isDeleted"));
        e.setDeleteDate(truncate(dto.getDeleteDate(), 20, "deleteDate"));
        e.setQuitType(truncate(dto.getQuitType(), 50, "quitType"));
        e.setLastWorkDay(truncate(dto.getLastWorkDay(), 20, "lastWorkDay"));
        e.setStaffOrigin(truncate(dto.getStaffOrigin(), 50, "staffOrigin"));
        e.setInitialWorkYears(truncate(dto.getInitialWorkYears(), 10, "initialWorkYears"));
        e.setEnrollWorkYears(truncate(dto.getEnrollWorkYears(), 10, "enrollWorkYears"));
        e.setFirstLevelDepartmentId(truncate(dto.getFirstLevelDepartmentId(), 50, "firstLevelDepartmentId"));
        e.setFirstLevelDepartmentName(truncate(dto.getFirstLevelDepartmentName(), 200, "firstLevelDepartmentName"));
        e.setStaffRemark(truncate(dto.getStaffRemark(), 500, "staffRemark"));
        e.setQuitReason(truncate(dto.getQuitReason(), 500, "quitReason"));
        e.setQuitReasonType(truncate(dto.getQuitReasonType(), 50, "quitReasonType"));
        e.setQuitRemindStaff(truncate(dto.getQuitRemindStaff(), 10, "quitRemindStaff"));
        e.setLunarBirthdayYear(truncate(dto.getLunarBirthdayYear(), 10, "lunarBirthdayYear"));
        e.setLunarBirthdayMonth(truncate(dto.getLunarBirthdayMonth(), 10, "lunarBirthdayMonth"));
        e.setLunarBirthdayDay(truncate(dto.getLunarBirthdayDay(), 10, "lunarBirthdayDay"));
        e.setNextSolarBirthday(truncate(dto.getNextSolarBirthday(), 20, "nextSolarBirthday"));
        e.setReinstateNumber(truncate(dto.getReinstateNumber(), 10, "reinstateNumber"));
        e.setCompanySiteId(truncate(dto.getCompanySiteId(), 50, "companySiteId"));
        e.setCompanySiteName(truncate(dto.getCompanySiteName(), 200, "companySiteName"));
        e.setCountry(truncate(dto.getCountry(), 50, "country"));
        e.setIsProbation(truncate(dto.getIsProbation(), 10, "isProbation"));
        e.setProbationStatus(truncate(dto.getProbationStatus(), 20, "probationStatus"));
        return e;
    }

    public static IhrEmployeeFlexAttr toFlexAttrEntity(String id, IhrFlexAttrDTO dto) {
        if (dto == null) {
            return null;
        }
        IhrEmployeeFlexAttr fa = new IhrEmployeeFlexAttr();
        fa.setId(truncate(id, 50, "id"));
        fa.setDCodeType1(truncate(dto.getD_CODE_TYPE_1(), 200, "D_CODE_TYPE_1"));
        fa.setDCodeType2(truncate(dto.getD_CODE_TYPE_2(), 200, "D_CODE_TYPE_2"));
        fa.setDCodeType3(truncate(dto.getD_CODE_TYPE_3(), 200, "D_CODE_TYPE_3"));
        fa.setDCodeType4(truncate(dto.getD_CODE_TYPE_4(), 200, "D_CODE_TYPE_4"));
        fa.setDBoolean0(truncate(dto.getD_BOOLEAN_0(), 200, "D_BOOLEAN_0"));
        fa.setDCodeType5(truncate(dto.getD_CODE_TYPE_5(), 200, "D_CODE_TYPE_5"));
        fa.setDCodeType6(truncate(dto.getD_CODE_TYPE_6(), 200, "D_CODE_TYPE_6"));
        fa.setDDate2(truncate(dto.getD_DATE_2(), 200, "D_DATE_2"));
        fa.setDCodeType7(truncate(dto.getD_CODE_TYPE_7(), 200, "D_CODE_TYPE_7"));
        fa.setDCodeType8(truncate(dto.getD_CODE_TYPE_8(), 200, "D_CODE_TYPE_8"));
        fa.setDCodeType9(truncate(dto.getD_CODE_TYPE_9(), 200, "D_CODE_TYPE_9"));
        return fa;
    }

    public static IhrEmployeeSubset04 toSubset04Entity(IhrEmployeeSubset04DTO dto) {
        if (dto == null) {
            return null;
        }
        IhrEmployeeSubset04 s = new IhrEmployeeSubset04();
        s.setStaffId(truncate(dto.getStaffId(), 50, "staffId"));
        s.setDCodeType0(truncate(dto.getD_CODE_TYPE_0(), 200, "D_CODE_TYPE_0"));
        s.setDCodeType1(truncate(dto.getD_CODE_TYPE_1(), 200, "D_CODE_TYPE_1"));
        s.setDCodeType2(truncate(dto.getD_CODE_TYPE_2(), 200, "D_CODE_TYPE_2"));
        s.setDCodeType3(truncate(dto.getD_CODE_TYPE_3(), 200, "D_CODE_TYPE_3"));
        s.setDCodeType4(truncate(dto.getD_CODE_TYPE_4(), 200, "D_CODE_TYPE_4"));
        s.setDCodeType5(truncate(dto.getD_CODE_TYPE_5(), 200, "D_CODE_TYPE_5"));
        s.setDCodeType6(truncate(dto.getD_CODE_TYPE_6(), 200, "D_CODE_TYPE_6"));
        s.setDString0(truncate(dto.getD_STRING_0(), 200, "D_STRING_0"));
        s.setDString1(truncate(dto.getD_STRING_1(), 200, "D_STRING_1"));
        return s;
    }

    public static IhrEmployee toBasicEntity(IhrEmployeeBasicDTO dto) {
        if (dto == null) {
            return null;
        }
        IhrEmployee e = new IhrEmployee();
        e.setId(IdWorker.getId());
        e.setStaffId(truncate(dto.getStaffId(), 50, "staffId"));
        e.setStaffNo(truncate(dto.getStaffNo(), 50, "staffNo"));
        e.setStaffName(truncate(dto.getStaffName(), 100, "staffName"));
        e.setNickName(truncate(dto.getNickName(), 100, "nickName"));
        e.setMobileNo(truncate(dto.getMobileNo(), 20, "mobileNo"));
        e.setEmail(truncate(dto.getEmail(), 100, "email"));
        e.setStaffStatus(truncate(dto.getStaffStatus(), 20, "staffStatus"));
        e.setIdCardType(truncate(dto.getIdCardType(), 20, "idCardType"));
        e.setIdCardNo(truncate(dto.getIdCardNo(), 50, "idCardNo"));
        e.setDepartmentName(truncate(dto.getDepartmentName(), 200, "departmentName"));
        e.setPositionName(truncate(dto.getPositionName(), 100, "positionName"));
        e.setSex(truncate(dto.getSex(), 10, "sex"));
        e.setStaffType(truncate(dto.getStaffType(), 20, "staffType"));
        e.setMarryStatus(truncate(dto.getMarryStatus(), 20, "marryStatus"));
        e.setHighestEducation(truncate(dto.getHighestEducation(), 50, "highestEducation"));
        e.setWorkPlace(truncate(dto.getWorkPlace(), 200, "workPlace"));
        e.setBirthday(truncate(dto.getBirthday(), 20, "birthday"));
        e.setContractBeginDate(truncate(dto.getContractBeginDate(), 20, "contractBeginDate"));
        e.setContractEndDate(truncate(dto.getContractEndDate(), 20, "contractEndDate"));
        e.setEnrollInDate(truncate(dto.getEnrollInDate(), 20, "enrollInDate"));
        e.setProbationEndDate(truncate(dto.getProbationEndDate(), 20, "probationEndDate"));
        e.setCreatedDate(truncate(dto.getCreatedDate(), 20, "createdDate"));
        e.setLastUpdateDate(truncate(dto.getLastUpdateDate(), 20, "lastUpdateDate"));
        return e;
    }
}
