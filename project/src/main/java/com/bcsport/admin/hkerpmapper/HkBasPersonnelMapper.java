package com.bcsport.admin.hkerpmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.hkerp.HkBasPersonnel;
import com.bcsport.admin.entity.hkerp.HkShopStockSportCity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 旧版HK ERP 职员资料 Mapper（HKERP 库）
 * <p>
 * 数据源切换后：HKERP 库只承担「写入 Bas_Personnel」+「部门/店铺映射查询」，
 * 人员名单来源（入职/变更/离职）改由 BC_SPORTS_IHR 库的 HkErpSyncStatusMapper 提供。
 */
@Mapper
public interface HkBasPersonnelMapper extends BaseMapper<HkBasPersonnel> {

    /** 查询最大职员序号（用于生成新 PersonnelID） */
    String queryMaxNum();

    /** 按姓名+手机号查是否已录入（用于判重/在职判断） */
    HkBasPersonnel queryEmployeeOne(@Param("personnelName") String personnelName,
                                    @Param("mobilePhone") String mobilePhone);

    /** 通过部门名称查 ERP 部门ID（Bas_DepartMent） */
    String queryIdByDepartmentName(@Param("departmentName") String departmentName);

    /** 通过部门名称(=店铺名)查 ShopID/StockID/SportCityID（Bas_Shop） */
    HkShopStockSportCity queryShopIdByDepartmentName(@Param("departmentName") String departmentName);

    /** 二次入职判断：按姓名+手机号统计 AllowUsed=0 的记录数 */
    int findPersonnelByNameAndMobileNo(@Param("personnelName") String personnelName,
                                       @Param("mobilePhone") String mobilePhone);

    /** 判断该员工是否已存在且启用（AllowUsed=1） */
    boolean findPersonnel(@Param("personnelName") String personnelName,
                          @Param("mobilePhone") String mobilePhone);

    /** 二次入职：修改 AllowUsed=1、PersonnelStatus=0、ShopID、StockID */
    void updatePersonnelOfAllowUsedAndPersonnelStatus(@Param("personnelName") String personnelName,
                                                       @Param("mobilePhone") String mobilePhone,
                                                       @Param("shopId") String shopId,
                                                       @Param("stockId") String stockId);

    /** 批量录入职员 */
    void insertBatch(@Param("list") List<HkBasPersonnel> list);

    /** 离职处理：修改 AllowUsed=0、PersonnelStatus=2、更新 ModifyDTM */
    void updateAllowUse(@Param("personnelName") String personnelName,
                        @Param("mobilePhone") String mobilePhone);

    /** 查询所有 AllowUsed=0 且 PersonnelStatus=0 且 修改日期满30天的记录（离职收尾，直读 Bas_Personnel） */
    List<HkBasPersonnel> findPersonnelByAllowUseAndStatus();

    /** 离职收尾：将满30天的禁用员工的在职状态改为离职（PersonnelStatus=2） */
    void updateStatus(@Param("personnelName") String personnelName,
                      @Param("mobilePhone") String mobilePhone);

    /** 按部门名称查 ShopID（Bas_Shop） */
    String findShopId(@Param("departmentName") String departmentName);

    /** 变更：更新员工的 ShopID/AllShopName/StockID */
    void updateUserShopId(@Param("staffName") String staffName,
                          @Param("mobileNo") String mobileNo,
                          @Param("shopId") String shopId,
                          @Param("newPhone") String newPhone,
                          @Param("newShopId") String newShopId,
                          @Param("newDepartment") String newDepartment,
                          @Param("stockId") String stockId);

    /** 变更：更新员工手机号（Mobilephone 与 Telphone） */
    void updateUserPh(@Param("staffName") String staffName,
                      @Param("mobileNo") String mobileNo,
                      @Param("newPhone") String newPhone);

    /** 通过 ShopID 查询对应的仓库ID（Bas_Shop, AllowUsed=1） */
    String getStockIdByShopId(@Param("shopId") String shopId);
}
