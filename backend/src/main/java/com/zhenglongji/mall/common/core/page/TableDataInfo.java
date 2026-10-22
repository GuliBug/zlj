package com.zhenglongji.mall.common.core.page;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 分页返回结构 {@code {total, rows}}
 */
@Data
public class TableDataInfo<T> implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private long total;

    private List<T> rows;

    public TableDataInfo() {}

    public TableDataInfo(List<T> rows, long total) {
        this.rows = rows;
        this.total = total;
    }
}
