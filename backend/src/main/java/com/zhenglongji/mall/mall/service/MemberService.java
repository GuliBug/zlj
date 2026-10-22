package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.constant.Constants;
import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.common.utils.MallUtils;
import com.zhenglongji.mall.framework.web.service.TokenService;
import com.zhenglongji.mall.mall.domain.LevelRule;
import com.zhenglongji.mall.mall.domain.Member;
import com.zhenglongji.mall.mall.domain.PointsLog;
import com.zhenglongji.mall.mall.repository.LevelRuleRepository;
import com.zhenglongji.mall.mall.repository.MemberRepository;
import com.zhenglongji.mall.mall.repository.PointsLogRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员服务：登录注册、积分、等级
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LevelRuleRepository levelRuleRepository;
    private final PointsLogRepository pointsLogRepository;
    private final TokenService tokenService;

    /** 手机号验证码登录，新手机号自动注册 */
    @Transactional
    public Map<String, Object> login(String phone, String smsCode) {
        if (phone == null || phone.isBlank()) {
            throw new ServiceException("手机号不能为空");
        }
        if (!Constants.SMS_CODE.equals(smsCode)) {
            throw new ServiceException("验证码错误");
        }
        Member member = memberRepository.findByPhone(phone).orElseGet(() -> {
            Member m = new Member();
            m.setPhone(phone);
            m.setPoints(0);
            m.setTotalPaid(0L);
            m.setLevelCode("V0");
            m.setCreatedAt(LocalDateTime.now());
            return memberRepository.save(m);
        });
        String token = tokenService.createMemberToken(member.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("memberId", member.getId());
        data.put("phone", phone);
        data.put("levelName", levelName(member.getLevelCode()));
        data.put("points", member.getPoints());
        return data;
    }

    public Member getById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new ServiceException("会员不存在"));
    }

    /** 会员中心信息 */
    public Map<String, Object> me(Long memberId) {
        Member member = getById(memberId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", member.getId());
        data.put("phone", MallUtils.maskPhone(member.getPhone()));
        data.put("levelCode", member.getLevelCode());
        data.put("levelName", levelName(member.getLevelCode()));
        data.put("points", member.getPoints());
        data.put("totalPaid", member.getTotalPaid());
        LevelRule next = levelRuleRepository.findAllByOrderByThresholdAsc().stream()
                .filter(r -> r.getThreshold() != null
                        && r.getThreshold() > (member.getTotalPaid() == null ? 0 : member.getTotalPaid()))
                .findFirst()
                .orElse(null);
        data.put("nextLevelName", next == null ? null : next.getLevelName());
        data.put(
                "nextLevelGap",
                next == null ? null : next.getThreshold() - (member.getTotalPaid() == null ? 0 : member.getTotalPaid()));
        return data;
    }

    /** 积分变动（change 可正可负；扣减不足时扣至 0），全留痕 */
    @Transactional
    public int changePoints(Member member, int change, String reason, String relatedNo) {
        int balance = member.getPoints() == null ? 0 : member.getPoints();
        int actual = change;
        if (change < 0 && balance + change < 0) {
            actual = -balance;
        }
        int after = balance + actual;
        member.setPoints(after);
        memberRepository.save(member);

        PointsLog pointsLog = new PointsLog();
        pointsLog.setMemberId(member.getId());
        pointsLog.setChange(actual);
        pointsLog.setBalanceAfter(after);
        pointsLog.setReason(reason);
        pointsLog.setRelatedNo(relatedNo);
        pointsLog.setCreatedAt(LocalDateTime.now());
        pointsLogRepository.save(pointsLog);
        return after;
    }

    /** 支付成功：发积分（实付 1 元 = 1 积分）+ 累计实付 + 等级自动升级（只升不降） */
    @Transactional
    public void onPaid(Member member, int payAmount, String orderNo) {
        int points = (int) Math.round(payAmount / 100.0);
        if (points > 0) {
            changePoints(member, points, "订单消费回馈", orderNo);
        }
        member.setTotalPaid((member.getTotalPaid() == null ? 0 : member.getTotalPaid()) + payAmount);
        upgradeLevel(member);
        memberRepository.save(member);
    }

    /** 按累计实付自动升级，只升不降 */
    public void upgradeLevel(Member member) {
        List<LevelRule> rules = levelRuleRepository.findAllByOrderByThresholdAsc();
        long paid = member.getTotalPaid() == null ? 0 : member.getTotalPaid();
        String target = member.getLevelCode() == null ? "V0" : member.getLevelCode();
        for (LevelRule rule : rules) {
            if (rule.getThreshold() != null && paid >= rule.getThreshold()) {
                target = rule.getLevelCode();
            }
        }
        if (rank(rules, target) > rank(rules, member.getLevelCode())) {
            log.info("会员 {} 等级 {} -> {}", member.getPhone(), member.getLevelCode(), target);
            member.setLevelCode(target);
        }
    }

    private int rank(List<LevelRule> rules, String levelCode) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getLevelCode().equals(levelCode)) {
                return i;
            }
        }
        return -1;
    }

    public String levelName(String levelCode) {
        return levelRuleRepository.findAllByOrderByThresholdAsc().stream()
                .filter(r -> r.getLevelCode().equals(levelCode))
                .map(LevelRule::getLevelName)
                .findFirst()
                .orElse(levelCode);
    }

    public TableDataInfo<Map<String, Object>> pointsLogs(Long memberId, int page, int size) {
        List<Map<String, Object>> rows = pointsLogRepository.findByMemberIdOrderByIdDesc(memberId).stream()
                .map(pl -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", pl.getId());
                    row.put("change", pl.getChange());
                    row.put("balanceAfter", pl.getBalanceAfter());
                    row.put("reason", pl.getReason());
                    row.put("relatedNo", pl.getRelatedNo());
                    row.put("createdAt", pl.getCreatedAt());
                    return row;
                })
                .toList();
        return PageUtils.page(rows, page, size);
    }

    /** 管理端会员列表（手机号脱敏） */
    public TableDataInfo<Map<String, Object>> adminPage(String keyword, int page, int size) {
        List<Map<String, Object>> rows = memberRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .filter(m -> keyword == null || keyword.isBlank() || m.getPhone().contains(keyword.trim()))
                .map(m -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", m.getId());
                    row.put("phone", MallUtils.maskPhone(m.getPhone()));
                    row.put("levelCode", m.getLevelCode());
                    row.put("levelName", levelName(m.getLevelCode()));
                    row.put("points", m.getPoints());
                    row.put("totalPaid", m.getTotalPaid());
                    row.put("createdAt", m.getCreatedAt());
                    return row;
                })
                .toList();
        return PageUtils.page(rows, page, size);
    }
}
