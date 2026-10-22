package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.utils.MallUtils;
import com.zhenglongji.mall.framework.security.LoginHelper;
import com.zhenglongji.mall.mall.domain.AuditLog;
import com.zhenglongji.mall.mall.repository.AuditLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务（不可删改）
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String module, String action, String detail) {
        String operator;
        try {
            operator = LoginHelper.getAdminName();
        } catch (Exception e) {
            operator = "system";
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setOperator(operator);
        auditLog.setModule(module);
        auditLog.setAction(action);
        auditLog.setDetail(detail);
        auditLog.setIp(MallUtils.clientIp());
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}
