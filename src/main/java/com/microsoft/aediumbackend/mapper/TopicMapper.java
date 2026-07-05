package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.Topic;

import java.util.List;

public interface TopicMapper extends BaseMapper<Topic> {

    void increaseArticleCountBatch(List<Long> existTopicsToUpdate);

    void decreaseArticleCountBatch(List<Long> topicIds);

    void insertIgnoreBatch(List<Topic> topicsToProcess);

    List<Topic> selectTopRelatedTopic(String slug);
}
