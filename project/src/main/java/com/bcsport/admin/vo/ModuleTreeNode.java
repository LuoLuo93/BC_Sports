package com.bcsport.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 操作日志模块筛选树节点：分组节点只有 label+children(不可选)，叶子节点 value 对应日志表 module 字段
 */
@Data
public class ModuleTreeNode {

    private String label;

    /** 仅叶子节点有值 */
    private String value;

    /** 分组节点不可选中(仅展开)，避免把分组名当模块名去过滤 */
    private Boolean disabled;

    private List<ModuleTreeNode> children;

    public static ModuleTreeNode leaf(String module) {
        ModuleTreeNode n = new ModuleTreeNode();
        n.setLabel(module);
        n.setValue(module);
        return n;
    }

    public static ModuleTreeNode group(String label, List<ModuleTreeNode> children) {
        ModuleTreeNode n = new ModuleTreeNode();
        n.setLabel(label);
        // el-tree-select 以 value 为 node-key，要求全树唯一；分组不可选(disabled)，
        // 加前缀避免与叶子模块名或其它分组的 null 键冲突
        n.setValue("group:" + label);
        n.setDisabled(true);
        n.setChildren(children);
        return n;
    }
}
