package com.aics.ticket.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("agent_group")
public class AgentGroup {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String name;
    public String scene;
    public Long leaderAgentId;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
