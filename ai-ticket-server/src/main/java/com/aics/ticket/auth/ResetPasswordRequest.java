package com.aics.ticket.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码重置请求
 * <p>用户提供账号和旧密码进行验证，然后设置新密码。</p>
 */
public class ResetPasswordRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度 6-64 位")
    private String newPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
