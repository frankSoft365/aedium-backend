package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.model.dto.activity.request.UserActivitiesQuery;
import com.microsoft.aediumbackend.model.dto.activity.response.UserActivity;
import com.microsoft.aediumbackend.service.activity.ActivityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/list")
    public Result<CursorPage<UserActivity>> userActivitiesQuery(
            @Valid @RequestBody UserActivitiesQuery query
            ) {
        return Result.success(activityService.getUserActivityList(query));
    }
}

