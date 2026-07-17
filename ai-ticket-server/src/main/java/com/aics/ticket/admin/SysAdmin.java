package com.aics.ticket.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_admin")
public class SysAdmin {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String username;
    public String passwordHash;
    public String realName;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
