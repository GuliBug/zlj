package com.zhenglongji.mall.common.core.page;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 分页工具：page 从 1 起；演示数据量小，管理端列表统一内存过滤后分页
 */
public class PageUtils {

    private PageUtils() {}

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        return PageRequest.of(page - 1, size, sort);
    }

    public static PageRequest ofDesc(int page, int size, String prop) {
        return of(page, size, Sort.by(Sort.Direction.DESC, prop));
    }

    /** 内存分页 */
    public static <T> TableDataInfo<T> page(List<T> list, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        int from = Math.min((page - 1) * size, list.size());
        int to = Math.min(from + size, list.size());
        return new TableDataInfo<>(list.subList(from, to), list.size());
    }
}
