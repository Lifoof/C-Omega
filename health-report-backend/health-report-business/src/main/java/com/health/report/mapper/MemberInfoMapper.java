package com.health.report.mapper;

import com.health.report.domain.MemberInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会员信息Mapper接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface MemberInfoMapper {
    /**
     * 查询会员信息
     *
     * @param id 会员信息主键
     * @return 会员信息
     */
    MemberInfo selectById(Long id);

    /**
     * 查询会员信息列表
     *
     * @param memberInfo 会员信息
     * @return 会员信息集合
     */
    List<MemberInfo> selectList(MemberInfo memberInfo);

    /**
     * 新增会员信息
     *
     * @param memberInfo 会员信息
     * @return 结果
     */
    int insert(MemberInfo memberInfo);

    /**
     * 修改会员信息
     *
     * @param memberInfo 会员信息
     * @return 结果
     */
    int updateById(MemberInfo memberInfo);

    /**
     * 删除会员信息
     *
     * @param id 会员信息主键
     * @return 结果
     */
    int deleteById(Long id);

    /**
     * 批量删除会员信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteBatchIds(@Param("ids") Long[] ids);

    /**
     * 查询会员信息数量
     *
     * @param memberInfo 会员信息查询条件
     * @return 会员信息数量
     */
    long selectCount(MemberInfo memberInfo);

    /**
     * 通过会员编号查询会员（过滤已删除）
     * @param queryMember 查询条件（memberNo + isDeleted）
     * @return 会员列表（最多1条）
     */
    List<MemberInfo> selectByMemberNo(MemberInfo queryMember);
}
