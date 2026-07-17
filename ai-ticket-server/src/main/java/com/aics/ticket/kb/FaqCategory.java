package com.aics.ticket.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("faq_category")
public class FaqCategory {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String name;
    public Long parentId;
    public Integer sortOrder;
    public LocalDateTime createdAt;
}
