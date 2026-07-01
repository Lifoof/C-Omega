package com.health.report.service;

import com.health.report.common.core.domain.entity.SysUser;
import com.health.report.domain.MemberInfo;
import com.health.report.dto.member.MemberInfoDTO;

import java.util.List;

/**
 * 会员信息Service接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface IMemberInfoService {
    /**
     * 查询会员信息
     *
     * @param id 会员信息主键
     * @return 会员信息
     */
    MemberInfo getById(Long id);

    /**
     * 查询会员信息列表
     *
     * @param memberInfo 会员信息
     * @return 会员信息集合
     */
     List<MemberInfo> page(MemberInfo memberInfo);

    /**
     * 新增会员信息
     *
     * @param memberInfo 会员信息
     * @return 结果
     */
    MemberInfo create(MemberInfo memberInfo);

    /**
     * 修改会员信息
     *
     * @param memberInfo 会员信息
     * @return 结果
     */
    MemberInfo update(MemberInfo memberInfo);

    /**
     * 删除会员信息
     *
     * @param id 会员信息主键
     */
    void deleteById(Long id);

    /**
     * 导入会员数据
     *
     * @param memberList 会员数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param lang 语言，默认zh
     * @return 结果
     */
    public String importMember(List<MemberInfo> memberList, Boolean isUpdateSupport,String lang);
}
