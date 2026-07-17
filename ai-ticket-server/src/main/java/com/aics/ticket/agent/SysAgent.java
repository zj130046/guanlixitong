package com.aics.ticket.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_agent")
public class SysAgent {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String username;
    public String passwordHash;
    public String realName;
    public String onlineStatus;
    public Long groupId;
    public String role;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
