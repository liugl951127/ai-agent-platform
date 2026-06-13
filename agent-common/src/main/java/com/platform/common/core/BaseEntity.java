package com.platform.common.core;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    /**
     * 租户 ID(多租户隔离)
     * <p>
     * 由 TenantSqlInterceptor 在 INSERT 时自动填充,SELECT/UPDATE/DELETE 自动追加 WHERE 条件
     * 如果业务表不需要多租户,可以不加这个字段(SQL 不会受影响,只是不会隔离)
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
