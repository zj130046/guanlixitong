package com.aics.ticket.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("faq_entry")
public class FaqEntry {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long categoryId;
    public String question;
    public String answer;
    public String keywords;
    public Integer enabled;
    public String questionVector; // 向量 JSON 字符串，用于语义匹配
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
