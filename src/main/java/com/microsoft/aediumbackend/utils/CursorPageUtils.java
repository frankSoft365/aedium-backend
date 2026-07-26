package com.microsoft.aediumbackend.utils;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * 游标分页工具类
 */
public class CursorPageUtils {
    
    /**
     * 游标信息
     */
    @Getter
    public static class CursorInfo {
        private final boolean hasMore;
        private final LocalDateTime nextCursorCreatedAt;
        private final Long nextCursorId;

        public CursorInfo(boolean hasMore, LocalDateTime nextCursorCreatedAt, Long nextCursorId) {
            this.hasMore = hasMore;
            this.nextCursorCreatedAt = nextCursorCreatedAt;
            this.nextCursorId = nextCursorId;
        }
    }
    
    /**
     * 提取游标信息并裁剪列表
     *
     * @param dataList   查询的数据列表（已包含hasMore判断的额外一条）
     * @param pageSize   每页大小
     * @param createdAtExtractor 提取创建时间的函数
     * @param idExtractor 提取ID的函数
     * @param <T>        数据类型
     * @return 游标信息
     */
    public static <T> CursorInfo extract(List<T> dataList, int pageSize, 
                                         Function<T, LocalDateTime> createdAtExtractor, 
                                         Function<T, Long> idExtractor) {
        boolean hasMore = dataList.size() > pageSize;
        if (hasMore) {
            dataList.subList(pageSize, dataList.size()).clear();
        }

        T lastItem = dataList.get(dataList.size() - 1);
        LocalDateTime nextCursorCreatedAt = createdAtExtractor.apply(lastItem);
        Long nextCursorId = idExtractor.apply(lastItem);

        return new CursorInfo(hasMore, nextCursorCreatedAt, nextCursorId);
    }
}
