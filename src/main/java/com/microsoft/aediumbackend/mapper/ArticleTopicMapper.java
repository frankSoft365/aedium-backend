package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.ArticleTopic;
import com.microsoft.aediumbackend.model.vo.TopicInArticleVO;

import java.util.List;

public interface ArticleTopicMapper extends BaseMapper<ArticleTopic> {

    List<TopicInArticleVO> getTopicsOfArticleById(Long articleId);
}
