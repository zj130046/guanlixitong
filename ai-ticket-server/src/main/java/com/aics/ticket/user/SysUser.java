package com.aics.ticket.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String username;
    public String passwordHash;
    public String phone;
    public String email;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
