package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.Member;
import com.zhenglongji.mall.mall.domain.dto.LevelRuleUpdateBody;
import com.zhenglongji.mall.mall.domain.dto.PointsBody;
import com.zhenglongji.mall.mall.service.AdminService;
import com.zhenglongji.mall.mall.service.AuditService;
import com.zhenglongji.mall.mall.service.MemberService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端会员与等级规则（AC-15）
 */
@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminMemberController extends BaseController {

    private final MemberService memberService;
    private final AdminService adminService;
    private final AuditService auditService;

    @GetMapping("/members")
    public AjaxResult<TableDataInfo<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(memberService.adminPage(keyword, page, size));
    }

    /** 人工调整积分（原因必填、留审计） */
    @PostMapping("/members/{id}/points")
    @Transactional
    public AjaxResult<Void> adjustPoints(@PathVariable Long id, @RequestBody PointsBody body) {
        if (body.change() == null || body.change() == 0) {
            throw new ServiceException("积分变动值非法");
        }
        if (body.reason() == null || body.reason().isBlank()) {
            throw new ServiceException("调整原因必填");
        }
        Member member = memberService.getById(id);
        int before = member.getPoints() == null ? 0 : member.getPoints();
        int after = memberService.changePoints(member, body.change(), "人工调整: " + body.reason(), null);
        auditService.log("membership", "调整积分", "会员[" + id + "] " + before + "->" + after + "，原因: " + body.reason());
        return success();
    }

    @GetMapping("/level-rules")
    public AjaxResult<List<Map<String, Object>>> levelRules() {
        return success(adminService.levelRules());
    }

    @PutMapping("/level-rules")
    public AjaxResult<Void> updateLevelRules(@RequestBody List<LevelRuleUpdateBody> bodies) {
        adminService.updateLevelRules(bodies);
        return success();
    }
}
