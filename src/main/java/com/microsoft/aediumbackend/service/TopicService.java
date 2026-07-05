package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.model.entity.Topic;
import com.microsoft.aediumbackend.model.vo.TopicSuggestionVO;

import java.util.List;

public interface TopicService extends IService<Topic> {
    List<TopicSuggestionVO> getTopicSuggestion(String topicName);
}
