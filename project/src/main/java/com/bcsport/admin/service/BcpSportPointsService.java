package com.bcsport.admin.service;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.bcp.BcpSportPoints;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.vo.SportPointsBoardVO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

public interface BcpSportPointsService {

    /**
     * 分页查询运动积分
     */
    PageResult<BcpSportPoints> page(PageQuery pageQuery, String sporter);

    /**
     * Excel 导入运动积分
     */
    Map<String, Object> importFromExcel(MultipartFile file) throws Exception;

    /**
     * 导入日志分页
     */
    PageResult<SysImportLog> logPage(PageQuery pageQuery);

    /**
     * 手工编辑一条运动积分（改名需不与现有运动员重名）
     */
    void updateSportPoints(Long id, String sporter, BigDecimal points);

    /**
     * 移动端榜单：无关键字=前 limit 名 + 全表统计；有关键字=姓名模糊匹配（带绝对名次）
     */
    SportPointsBoardVO rankBoard(String keyword, int limit);

    /**
     * 管理员代传自定义头像（存 uploads/avatar 子目录，返回 /images/avatar/xxx 访问地址）
     */
    String saveAvatar(Long id, MultipartFile file);

    /**
     * 清除自定义头像（同时删磁盘文件，移动端回退动物emoji）
     */
    void clearAvatar(Long id);

    /**
     * 管理员代传移动端顶部头图（存 uploads/hero，写 sys_config mobile.rankHeroUrl，返回访问地址）
     */
    String saveHeroImage(MultipartFile file);

    /**
     * 清除头图恢复默认蓝色渐变（同时删磁盘文件）
     */
    void clearHeroImage();

    /**
     * 当前头图URL（空=未设置，移动端回退默认蓝色渐变）
     */
    String getHeroUrl();
}
