package com.lujianfeng.spanner.vo.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResultVO<T> {
    private List<T> records;
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private boolean hasMore;
}
