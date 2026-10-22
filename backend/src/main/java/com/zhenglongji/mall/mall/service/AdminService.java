package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.framework.web.service.TokenService;
import com.zhenglongji.mall.mall.domain.AdminUser;
import com.zhenglongji.mall.mall.domain.AfterSale;
import com.zhenglongji.mall.mall.domain.AuditLog;
import com.zhenglongji.mall.mall.domain.LevelRule;
import com.zhenglongji.mall.mall.domain.MallOrder;
import com.zhenglongji.mall.mall.domain.dto.LevelRuleUpdateBody;
import com.zhenglongji.mall.mall.repository.AdminUserRepository;
import com.zhenglongji.mall.mall.repository.AfterSaleRepository;
import com.zhenglongji.mall.mall.repository.AuditLogRepository;
import com.zhenglongji.mall.mall.repository.LevelRuleRepository;
import com.zhenglongji.mall.mall.repository.OrderRepository;
import com.zhenglongji.mall.mall.repository.VerifyRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端服务：登录（5 次失败锁 30 分钟）、看板、审计日志、告警、等级规则、核销记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final TokenService tokenService;
    private final OrderRepository orderRepository;
    private final AfterSaleRepository afterSaleRepository;
    private final AlertService alertService;
    private final AuditLogRepository auditLogRepository;
    private final LevelRuleRepository levelRuleRepository;
    private final VerifyRecordRepository verifyRecordRepository;
    private final AuditService auditService;

    /** 管理端登录：连续 5 次失败锁定 30 分钟 */
    @Transactional
    public Map<String, Object> login(String username, String password) {
        if (username == null || password == null) {
            throw new ServiceException("账号密码不能为空");
        }
        AdminUser admin = adminUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new ServiceException("账号或密码错误"));
        if (admin.getLockUntil() != null && admin.getLockUntil().isAfter(LocalDateTime.now())) {
            long minutes = java.time.Duration.between(LocalDateTime.now(), admin.getLockUntil()).toMinutes() + 1;
            throw new ServiceException("账号已锁定，请 " + minutes + " 分钟后重试");
        }
        if (!password.equals(admin.getPassword())) {
            int fails = (admin.getFailCount() == null ? 0 : admin.getFailCount()) + 1;
            admin.setFailCount(fails);
            if (fails >= 5) {
                admin.setLockUntil(LocalDateTime.now().plusMinutes(30));
                admin.setFailCount(0);
                log.warn("管理员账号 {} 连续 5 次登录失败，锁定 30 分钟", username);
            }
            adminUserRepository.save(admin);
            throw new ServiceException("账号或密码错误");
        }
        admin.setFailCount(0);
        admin.setLockUntil(null);
        adminUserRepository.save(admin);
        String token = tokenService.createAdminToken(admin.getUsername());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        return data;
    }

    /** 经营看板 */
    public Map<String, Object> dashboard() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<MallOrder> orders = orderRepository.findAll();

        long gmvToday = 0;
        int orderCountToday = 0;
        int paidCountToday = 0;
        for (MallOrder o : orders) {
            if (o.getCreatedAt() != null && !o.getCreatedAt().isBefore(dayStart) && o.getCreatedAt().isBefore(dayEnd)) {
                orderCountToday++;
                if (o.getPayTime() != null && !"WAIT_PAY".equals(o.getStatus()) && !"CLOSED".equals(o.getStatus())) {
                    paidCountToday++;
                    gmvToday += o.getPayAmount() == null ? 0 : o.getPayAmount();
                }
            }
        }
        long pendingShip = orders.stream().filter(o -> "PAID".equals(o.getStatus())).count();
        long pendingAftersale = afterSaleRepository.findAll().stream()
                .filter(a -> "APPLYING".equals(a.getStatus())
                        || "REFUNDING".equals(a.getStatus())
                        || "WAIT_RETURN".equals(a.getStatus()))
                .count();

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            long gmv = 0;
            long count = 0;
            for (MallOrder o : orders) {
                if (o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && o.getCreatedAt().isBefore(end)) {
                    count++;
                    if (o.getPayTime() != null && !"WAIT_PAY".equals(o.getStatus()) && !"CLOSED".equals(o.getStatus())) {
                        gmv += o.getPayAmount() == null ? 0 : o.getPayAmount();
                    }
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.toString());
            row.put("gmv", gmv);
            row.put("orderCount", count);
            trend.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gmvToday", gmvToday);
        data.put("orderCountToday", orderCountToday);
        data.put("payRate", orderCountToday == 0 ? 0 : Math.round(paidCountToday * 1000.0 / orderCountToday) / 10.0);
        data.put("pendingShip", pendingShip);
        data.put("pendingAftersale", pendingAftersale);
        data.put("stockAlertCount", alertService.stockAlertCount());
        data.put("salesTrend", trend);
        return data;
    }

    /** 审计日志 */
    public TableDataInfo<Map<String, Object>> auditLogs(String module, int page, int size) {
        List<AuditLog> logs = auditLogRepository.findAll();
        logs.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AuditLog log1 : logs) {
            if (module != null && !module.isBlank() && !module.equals(log1.getModule())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log1.getId());
            row.put("operator", log1.getOperator());
            row.put("module", log1.getModule());
            row.put("action", log1.getAction());
            row.put("detail", log1.getDetail());
            row.put("ip", log1.getIp());
            row.put("createdAt", log1.getCreatedAt());
            rows.add(row);
        }
        return PageUtils.page(rows, page, size);
    }

    /** 告警列表 */
    public TableDataInfo<Map<String, Object>> alerts(int page, int size) {
        List<Map<String, Object>> rows = alertService.list().stream()
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", a.getId());
                    row.put("type", a.getType());
                    row.put("content", a.getContent());
                    row.put("status", a.getStatus());
                    row.put("createdAt", a.getCreatedAt());
                    return row;
                })
                .toList();
        return PageUtils.page(rows, page, size);
    }

    /** 等级规则 */
    public List<Map<String, Object>> levelRules() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LevelRule rule : levelRuleRepository.findAllByOrderByThresholdAsc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("levelCode", rule.getLevelCode());
            row.put("levelName", rule.getLevelName());
            row.put("threshold", rule.getThreshold());
            rows.add(row);
        }
        return rows;
    }

    /** 修改等级规则（留审计） */
    @Transactional
    public void updateLevelRules(List<LevelRuleUpdateBody> bodies) {
        for (LevelRuleUpdateBody body : bodies) {
            LevelRule rule = levelRuleRepository.findAllByOrderByThresholdAsc().stream()
                    .filter(r -> r.getLevelCode().equals(body.levelCode()))
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("等级不存在: " + body.levelCode()));
            Long old = rule.getThreshold();
            rule.setThreshold(body.threshold());
            levelRuleRepository.save(rule);
            auditService.log("membership", "修改等级阈值", "等级 " + rule.getLevelCode() + " " + old + "->" + body.threshold() + " 分");
        }
    }

    /** 核销记录 */
    public TableDataInfo<Map<String, Object>> verifyRecords(Long storeId, int page, int size) {
        List<com.zhenglongji.mall.mall.domain.VerifyRecord> records = storeId == null
                ? verifyRecordRepository.findAll()
                : verifyRecordRepository.findByStoreIdOrderByIdDesc(storeId);
        records.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        List<Map<String, Object>> rows = records.stream()
                .map(r -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", r.getId());
                    row.put("orderNo", r.getOrderNo());
                    row.put("storeName", r.getStoreName());
                    row.put("verifier", r.getVerifier());
                    row.put("verifyTime", r.getVerifyTime());
                    row.put("payAmount", r.getPayAmount());
                    return row;
                })
                .toList();
        return PageUtils.page(rows, page, size);
    }
}
