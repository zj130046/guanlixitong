package com.aics.ticket.ticket;

import com.aics.ticket.common.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;

public class TicketCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String category;
    private String department;
    /** 附件 URL 列表（JSON 数组字符串） */
    private String attachmentUrls;
    private TicketPriority priority = TicketPriority.NORMAL;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(String attachmentUrls) { this.attachmentUrls = attachmentUrls; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
}
