package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.model.vo.ArticleVO;

import java.math.BigDecimal;
import java.util.List;

public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 查询所有文章列表
     */
    List<ArticleListItemVO> getArticleList();

    /**
     * 查询某个用户的文章列表
     */
    List<ArticleListItemVO> getUserArticleList(Long userId);

    ArticleVO getArticleById(Long id);

    int updateArticleInfo(Long id, String content, String title, String subtitle, String coverImage, BigDecimal coverFocusY);
}
