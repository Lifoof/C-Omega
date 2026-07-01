package com.health.report.mapper;

import com.health.report.domain.CollectionRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 采集记录Mapper接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface CollectionRecordMapper {
    /**
     * 查询采集记录
     *
     * @param id 采集记录主键
     * @return 采集记录
     */
    CollectionRecord selectById(Long id);

    /**
     * 查询采集记录列表
     *
     * @param record 采集记录
     * @return 采集记录集合
     */
    List<CollectionRecord> selectList(CollectionRecord record);

    /**
     * 新增采集记录
     *
     * @param record 采集记录
     * @return 结果
     */
    int insert(CollectionRecord record);

    /**
     * 修改采集记录
     *
     * @param record 采集记录
     * @return 结果
     */
    int updateById(CollectionRecord record);

    /**
     * 删除采集记录
     *
     * @param id 采集记录主键
     * @return 结果
     */
    int deleteById(Long id);

    /**
     * 批量删除采集记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteBatchIds(@Param("ids") Long[] ids);

    /**
     * 查询采集记录数量
     *
     * @param record 采集记录查询条件
     * @return 采集记录数量
     */
    long selectCount(CollectionRecord record);

     CollectionRecord selectByMemberIdAndCheckupDate(@Param("memberId") Long memberId,
                                                     @Param("checkupDate") LocalDate checkupDate);
}
