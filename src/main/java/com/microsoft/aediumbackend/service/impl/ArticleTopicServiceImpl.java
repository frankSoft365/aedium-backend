package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.mapper.ArticleTopicMapper;
import com.microsoft.aediumbackend.model.entity.ArticleTopic;
import com.microsoft.aediumbackend.service.ArticleTopicService;
import org.springframework.stereotype.Service;

@Service
public class ArticleTopicServiceImpl extends ServiceImpl<ArticleTopicMapper, ArticleTopic> implements ArticleTopicService {
}
