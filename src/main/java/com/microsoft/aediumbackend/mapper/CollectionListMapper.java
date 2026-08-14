package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.dto.collection.ListCoverDTO;
import com.microsoft.aediumbackend.model.entity.CollectionList;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CollectionListMapper extends BaseMapper<CollectionList> {

    /**
     * 批量查询多个列表的前 N 个文章封面（按收藏时间倒序取每个列表的前 limit 个）
     * 使用 ROW_NUMBER() 窗口函数实现分组 Top-N（MySQL 8.0+），MP 无法替代
     */
    List<ListCoverDTO> findListCovers(@Param("listIds") List<Long> listIds,
                                      @Param("limit") int limit);
}
