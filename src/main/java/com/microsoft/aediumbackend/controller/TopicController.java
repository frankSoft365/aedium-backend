package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.model.dto.topic.TopicNameRequest;
import com.microsoft.aediumbackend.model.vo.TopicSuggestionVO;
import com.microsoft.aediumbackend.service.TopicService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/topic")
public class TopicController {
    @Resource
    private TopicService topicService;

    @PostMapping("/getSuggestion")
    public Result<List<TopicSuggestionVO>> getTopicSuggestion(@RequestBody TopicNameRequest topicNameRequest) {
        List<TopicSuggestionVO> suggestionList = topicService.getTopicSuggestion(topicNameRequest.getName());
        return Result.success(suggestionList);
    }
}
