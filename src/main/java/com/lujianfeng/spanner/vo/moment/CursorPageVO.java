package com.lujianfeng.spanner.vo.moment;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CursorPageVO<T> {
    private List<T> records;
    private String nextCursor;
    private Boolean hasMore;
}
