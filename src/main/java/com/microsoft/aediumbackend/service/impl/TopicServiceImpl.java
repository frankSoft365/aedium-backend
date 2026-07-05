package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.mapper.TopicMapper;
import com.microsoft.aediumbackend.model.entity.Topic;
import com.microsoft.aediumbackend.model.vo.TopicSuggestionVO;
import com.microsoft.aediumbackend.service.TopicService;
import com.microsoft.aediumbackend.utils.SlugUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Resource
    private TopicMapper topicMapper;

    /**
     * 查询topic联想建议
     *
     * @param topicName topicName
     * @return topicName 热度数量 新旧状态
     */
    @Override
    public List<TopicSuggestionVO> getTopicSuggestion(String topicName) {
        if (StringUtils.isBlank(topicName)) {
            return Collections.emptyList();
        }

        String slug = SlugUtils.makeSlug(topicName);

        if (StringUtils.isBlank(slug)) {
            return Collections.emptyList();
        }
        List<TopicSuggestionVO> topicSuggestionList = new ArrayList<>();
        // exist slugs
        List<Topic> list = topicMapper.selectTopRelatedTopic(slug);
        boolean isExactMatchExists = list.stream().anyMatch(topic -> topic.getSlug().equals(slug));
        if (!isExactMatchExists) {
            topicSuggestionList.add(new TopicSuggestionVO(topicName, null, "new"));
        }
        list.sort(Comparator.comparing(Topic::getArticlesCount).reversed());

        if (!list.isEmpty()) {
            list.forEach(topic -> {
                topicSuggestionList.add(new TopicSuggestionVO(topic.getName(), topic.getArticlesCount(), "existing"));
            });
        }
        // TODO 'javv' 匹配 'Javascript'
        return topicSuggestionList;
    }
}
