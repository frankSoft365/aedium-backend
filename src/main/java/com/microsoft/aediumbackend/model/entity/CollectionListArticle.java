package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏列表-文章关联（复合主键 list_id + article_id）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("collection_list_article")
public class CollectionListArticle {

    /**
     * 复合主键之一：MyBatis-Plus 要求至少一个 @TableId，这里标记为 INPUT（无自增）
     * 实际增删均通过自定义 SQL / 条件构造器按 (listId, articleId) 处理
     */
    @TableId(type = IdType.INPUT)
    private Long listId;

    private Long articleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
