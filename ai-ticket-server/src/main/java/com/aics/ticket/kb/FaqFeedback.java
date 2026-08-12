package com.aics.ticket.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * FAQ 用户反馈记录
 */
@TableName("faq_feedback")
public class FaqFeedback {

    @TableId(type = IdType.AUTO)
    public Long id;

    /** 关联的 FAQ 条目 ID */
    public Long entryId;

    /** 用户 ID（可为空表示匿名） */
    public Long userId;

    /** true=有帮助, false=无帮助 */
    public Boolean helpful;

    public LocalDateTime createdAt;
}
