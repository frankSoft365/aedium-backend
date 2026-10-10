package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.UserReadingHistory;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.model.vo.ArticleVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 查询所有文章列表 按照修改时间倒序 排除删除的文章
     */
    List<ArticleListItemVO> findAllArticlesCursor(
            @Param("lastUpdateTime") LocalDateTime lastUpdateTime,
            @Param("lastId") Long lastId,
            @Param("size") Integer size
    );

    /**
     * 内部调用
     * 根据id集合查询文章预览列表
     */
    List<ArticleListItemVO> selectArticleListItemVOByIds(List<Long> articleIds);

    /**
     * 查询某个用户的文章列表
     */
    List<ArticleListItemVO> getUserArticleList(Long userId);

    ArticleVO getArticleById(Long id);

    int updateArticleInfo(Long id, String content, String title, String subtitle, String coverImage, BigDecimal coverFocusY);

    /**
     * 根据ID列表获取文章简要信息
     */
    List<ArticleBriefDTO> getArticleBriefByIds(Set<Long> articleIds);

    /**
     * 根据ID列表批量获取文章列表项（含作者信息，过滤已删除文章）
     */
    List<ArticleListItemVO> getArticleListByIds(@Param("articleIds") List<Long> articleIds);

    @Update("UPDATE article SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE article SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);
}
