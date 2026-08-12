package com.aics.ticket.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * FAQ 标签（对应 faq_tag 表）
 */
@TableName("faq_tag")
public class FaqTag {

    @TableId(type = IdType.AUTO)
    public Long id;
    public String name;
}
