package com.health.report.service.impl;

import com.health.report.common.core.domain.entity.SysUser;
import com.health.report.common.exception.ServiceException;
import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.SecurityUtils;
import com.health.report.common.utils.StringUtils;
import com.health.report.common.utils.bean.BeanValidators;
import com.health.report.domain.MemberInfo;
import com.health.report.dto.member.MemberInfoDTO;
import com.health.report.mapper.MemberInfoMapper;
import com.health.report.mapper.ReportMapper;
import com.health.report.service.IMemberInfoService;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.UUID;

/**
 * 会员信息Service业务层处理
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@Service
@Slf4j
public class MemberInfoServiceImpl implements IMemberInfoService {

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    protected Validator validator;

    @Override
    public MemberInfo getById(Long id) {
        MemberInfo member = memberInfoMapper.selectById(id);
        if (member == null) {
            throw new ServiceException("用户不存在");
        }
        // 检查权限
        if (!isAdmin() && !member.getUserId().equals(getCurrentUserId())) {
            throw new ServiceException("无权访问此会员");
        }
        return member;
    }

    @Override
    public List<MemberInfo> page(MemberInfo memberInfo) {
        if (!isAdmin()) {
            memberInfo.setUserId(getCurrentUserId());
        }
        return memberInfoMapper.selectList(memberInfo);
    }

    @Override
    @Transactional
    public MemberInfo create(MemberInfo memberInfo) {
        memberInfo.setUserId(getCurrentUserId());
        memberInfo.setMemberNo("MEM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        memberInfo.setCreateBy(SecurityUtils.getLoginUser().getUsername());
        memberInfo.setCreateTime(DateUtils.getNowDate());
        memberInfoMapper.insert(memberInfo);
        return memberInfo;
    }

    @Override
    @Transactional
    public MemberInfo update(MemberInfo memberInfo) {
        memberInfo.setUpdateBy(SecurityUtils.getLoginUser().getUsername());
        memberInfo.setUpdateTime(DateUtils.getNowDate());
        memberInfoMapper.updateById(memberInfo);
        return memberInfo;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        memberInfoMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importMember(List<MemberInfo> memberList, Boolean isUpdateSupport, String lang) {
        if (StringUtils.isNull(memberList) || memberList.isEmpty()) {
            String msg = "en".equals(lang) ? "Import user data cannot be empty!" : "导入用户数据不能为空！";
            throw new ServiceException(msg);
        }

        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        for (int i = 0; i < memberList.size(); i++) {
            MemberInfo member = memberList.get(i);
            int row = i + 2;

            try {
                // 行号前缀
                String rowPrefix = "en".equals(lang) ? "Row " + row + ": " : "第" + row + "行：";

                // 姓名必填
                if (StringUtils.isBlank(member.getName())) {
                    String msg = "en".equals(lang) ? "Name cannot be empty" : "姓名不能为空";
                    throw new IllegalArgumentException(rowPrefix + msg);
                }
                // 性别必填
                if (StringUtils.isNull(member.getGender())) {
                    String msg = "en".equals(lang) ? "Gender cannot be empty" : "性别不能为空";
                    throw new IllegalArgumentException(rowPrefix + msg);
                }
                // 出生日期必填
                if (StringUtils.isNull(member.getBirthDate())) {
                    String msg = "en".equals(lang) ? "Birth date cannot be empty" : "出生日期不能为空";
                    throw new IllegalArgumentException(rowPrefix + msg);
                }

                // 日期格式验证
                sdf.parse(sdf.format(member.getBirthDate()));
                BeanValidators.validateWithException(validator, member);

                if (StringUtils.isNull(member.getRelationship())) {
                    member.setRelationship(1);
                }

                member.setMemberNo("MEM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
                member.setUserId(SecurityUtils.getLoginUser().getUserId());
                member.setCreateBy(SecurityUtils.getLoginUser().getUsername());

                memberInfoMapper.insert(member);
                successNum++;

                // 成功提示
                String successTip = "en".equals(lang)
                        ? successNum + ". User " + member.getName() + " imported successfully"
                        : successNum + "、用户 " + member.getName() + " 导入成功";

                successMsg.append("<br/><br/>").append(successTip);
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/><br/>").append(failureNum).append("、").append(e.getMessage());
                log.error("导入失败：", e);
            }
        }

        if (failureNum > 0) {
            String failTitle = "en".equals(lang)
                    ? "<br/>Sorry, import failed! Total " + failureNum + " errors:"
                    : "<br/>很抱歉，导入失败！共 " + failureNum + " 条数据错误：";
            failureMsg.insert(0, failTitle);
            failureMsg.append("<br/>");
            throw new ServiceException(failureMsg.toString());
        } else {
            String successTitle = "en".equals(lang)
                    ? "<br/>Congratulations, all imported successfully! Total " + successNum + " items:"
                    : "<br/>恭喜您，全部导入成功！共 " + successNum + " 条：";
            successMsg.insert(0, successTitle);
            successMsg.append("<br/>");
        }

        return successMsg.toString();
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getLoginUser().getUserId();
    }

    private boolean isAdmin() {
        return SecurityUtils.isAdmin(getCurrentUserId());
    }
}
