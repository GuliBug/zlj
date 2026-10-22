package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.BatchShipBody;
import com.zhenglongji.mall.mall.domain.dto.ShipBody;
import com.zhenglongji.mall.mall.service.OrderService;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单（AC-13）
 */
@RestController
@RequestMapping("/admin-api/orders")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminOrderController extends BaseController {

    private final OrderService orderService;

    @GetMapping
    public AjaxResult<TableDataInfo<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean overdueShip,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(orderService.adminPage(status, keyword, overdueShip, page, size));
    }

    @GetMapping("/{no}")
    public AjaxResult<Map<String, Object>> detail(@PathVariable String no) {
        return success(orderService.detail(null, no));
    }

    @PostMapping("/{no}/ship")
    public AjaxResult<Void> ship(@PathVariable String no, @RequestBody ShipBody body) {
        orderService.ship(no, body.company(), body.no());
        return success();
    }

    @PostMapping("/batch-ship")
    public AjaxResult<Map<String, Object>> batchShip(@RequestBody BatchShipBody body) {
        return success(orderService.batchShip(body.rows()));
    }

    /** 订单导出 CSV */
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public String export(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        LocalDate fromDate = from == null || from.isBlank() ? null : LocalDate.parse(from);
        LocalDate toDate = to == null || to.isBlank() ? null : LocalDate.parse(to);
        return orderService.exportCsv(fromDate, toDate);
    }
}
