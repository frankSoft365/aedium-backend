package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.CollectionListArticle;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface CollectionListArticleMapper extends BaseMapper<CollectionListArticle> {

    /**
     * 插入列表-文章关系，重复时忽略（复合主键冲突即视为已收藏）
     * 返回受影响行数：1=新插入，0=已存在被忽略
     */
    @Insert("INSERT IGNORE INTO collection_list_article (list_id, article_id) VALUES (#{listId}, #{articleId})")
    int insertIgnore(@Param("listId") Long listId, @Param("articleId") Long articleId);

    /**
     * 删除某条列表-文章关系
     * 返回受影响行数：1=已删除，0=原本不存在
     */
    @Delete("DELETE FROM collection_list_article WHERE list_id = #{listId} AND article_id = #{articleId}")
    int deleteRelation(@Param("listId") Long listId, @Param("articleId") Long articleId);
}
