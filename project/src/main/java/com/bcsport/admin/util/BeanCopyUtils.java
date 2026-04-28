package com.bcsport.admin.util;

import com.bcsport.admin.common.PageResult;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对象属性拷贝工具类
 */
public class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    /**
     * 拷贝单个对象
     */
    public static <S, T> T copy(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("属性拷贝失败", e);
        }
    }

    /**
     * 拷贝集合
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> copy(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 拷贝分页对象
     */
    public static <S, T> PageResult<T> copyPage(PageResult<S> sourcePage, Class<T> targetClass) {
        if (sourcePage == null) {
            return null;
        }
        PageResult<T> targetPage = new PageResult<>();
        targetPage.setTotal(sourcePage.getTotal());
        targetPage.setPages(sourcePage.getPages());
        targetPage.setPageNum(sourcePage.getPageNum());
        targetPage.setPageSize(sourcePage.getPageSize());
        
        List<T> targetRecords = copyList(sourcePage.getRecords(), targetClass);
        targetPage.setRecords(targetRecords);
        return targetPage;
    }
}
