package com.microsoft.aediumbackend.service.impl.comment;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentThreadDTO;
import com.microsoft.aediumbackend.model.dto.notification.response.ReplyNotificationVO;
import com.microsoft.aediumbackend.model.dto.notificationPush.request.NotificationPushRequest;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;
import com.microsoft.aediumbackend.service.NotificationService;
import com.microsoft.aediumbackend.service.RedisCacheService;
import com.microsoft.aediumbackend.service.impl.CommentServiceImpl;
import com.microsoft.aediumbackend.service.impl.notification.NotificationPushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.microsoft.aediumbackend.service.impl.CommentServiceImpl.COMMENTS_ROOT_FIRST;
import static com.microsoft.aediumbackend.service.impl.CommentServiceImpl.COMMENT_FIRST_PAGE_TTL;

@Service
@Slf4j
public class CommentPostProcessService {

    @Resource
    private NotificationPushService notificationPushService;
    @Resource
    private NotificationService notificationService;
    @Resource
    private RedisCacheService redisCacheService;
    @Resource
    private CommentServiceImpl commentService;

    @Value("${cache.comments.enabled:true}")
    private boolean cacheEnabled;


    @Async("commentTaskExecutor")
    public void handlePostCommentTasks(Comment comment, Long articleId, Notification notification) {
        try {
            if (comment.getRootId() == null) {
                if (cacheEnabled) {
                    String key = COMMENTS_ROOT_FIRST + articleId;
                    CursorPage<CommentThreadDTO> firstPage = commentService.getRootCommentsCore(articleId, CursorPageRequest.getFirstReq());
                    redisCacheService.set(key, firstPage, COMMENT_FIRST_PAGE_TTL);
                }
            }

            // 向在线用户推送这条通知
            if (notification != null) {
                ReplyNotificationVO replyNotificationVO = notificationService.toReplyNotificationVO(notification);
                Long recipientId = replyNotificationVO.getRecipientId();
                Long unreadReplyCount = notificationService.getUnreadCountByType(recipientId, NotificationQueryType.REPLY.getValue());
                NotificationPushRequest<ReplyNotificationVO> request = new NotificationPushRequest<>(
                        recipientId,
                        NotificationQueryType.REPLY.getValue(),
                        unreadReplyCount,
                        replyNotificationVO
                );
                notificationPushService.pushUnreadCount(request);
            }
        } catch (Exception e) {
            // MUST catch here — an uncaught exception in an @Async void method
            // is only logged by Spring's default handler, never propagates to the caller,
            // and the caller has ALREADY returned its response by this point anyway
            log.error("评论添加的消息推送：Post-comment processing failed for comment {}", comment.getId(), e);
        }
    }
}