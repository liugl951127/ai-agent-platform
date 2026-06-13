package com.platform.common.audit;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志记录(内存模型,各服务可自由选择持久化方式)
 * <p>
 * 这里默认用 JSON 写到日志文件,业务也可以直接 INSERT 到 sys_audit_log 表
 */
@Data
public class AuditLogRecord {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String resourceType;
    private String resourceId;
    private String method;
    private String url;
    private String ip;
    private String ua;
    private String requestArgs;
    private String responseData;
    private Integer costMs;
    private Integer status;       // 1=成功 0=失败
    private String errorMsg;
    private LocalDateTime createTime;
}
