package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.entity.ihr.NxcrmTagCategory;
import com.bcsport.admin.ihrmapper.NxcrmTagCategoryMapper;
import com.nascent.ecrp.opensdk.domain.customer.tag.CustomerTagCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component("nxcrmTagCategorySyncTask")
public class NxcrmTagCategorySyncTask {

    @Resource
    private NxCrmApiClient nxCrmApiClient;

    @Resource
    private NxcrmTagCategoryMapper tagCategoryMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<CustomerTagCategory> syncTagCategories() {
        log.info("=== 开始执行: 南讯CRM同步标签分类 ===");
        try {
            List<CustomerTagCategory> categories = nxCrmApiClient.getTagCategories();
            if (categories != null && !categories.isEmpty()) {
                tagCategoryMapper.delete(null);
                for (CustomerTagCategory cat : categories) {
                    NxcrmTagCategory entity = new NxcrmTagCategory();
                    entity.setId(cat.getId());
                    entity.setPid(cat.getPid());
                    entity.setName(cat.getName());
                    entity.setPidFullPath(cat.getPidFullPath());
                    tagCategoryMapper.insert(entity);
                }
                log.info("标签分类同步完成, 共{}条", categories.size());
            }
            log.info("=== 完成执行: 南讯CRM同步标签分类 ===");
            return categories;
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM同步标签分类 ===", e);
            throw e;
        }
    }
}
