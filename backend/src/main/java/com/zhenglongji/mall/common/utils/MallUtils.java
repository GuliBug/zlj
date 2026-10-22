package com.zhenglongji.mall.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通用工具：手机号脱敏、单号生成、核销码、IP
 */
public class MallUtils {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000000);

    private MallUtils() {}

    /** 手机号脱敏：138****1114 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 订单号：yyyyMMddHHmmss + 6 位序列 */
    public static String genOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FORMATTER) + String.format("%06d", SEQ.incrementAndGet() % 1000000);
    }

    /** 售后单号 */
    public static String genAfterSaleNo() {
        return "AS" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + String.format("%04d", SEQ.incrementAndGet() % 10000);
    }

    /** 核销码：基于订单号 + 分钟窗口哈希取 6 位数字 */
    public static String verifyCode(String orderNo, long window) {
        int hash = (orderNo + ":" + window).hashCode();
        return String.format("%06d", Math.floorMod(hash, 1000000));
    }

    /** 当前分钟窗口核销码 */
    public static String currentVerifyCode(String orderNo) {
        return verifyCode(orderNo, System.currentTimeMillis() / 60000);
    }

    /** 当前分钟窗口剩余秒数 */
    public static int verifyCodeExpiresIn() {
        return 60 - (int) (System.currentTimeMillis() / 1000 % 60);
    }

    /** 客户端 IP（审计用） */
    public static String clientIp() {
        var request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static HttpServletRequest currentRequest() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception ignore) {
            // 非 Web 上下文（定时任务等）
        }
        return null;
    }
}
