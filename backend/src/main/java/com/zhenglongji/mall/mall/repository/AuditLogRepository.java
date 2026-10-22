package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
