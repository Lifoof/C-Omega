package com.health.report.mapper;

import com.health.report.domain.AiModel;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AI模型Mapper接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface AiModelMapper {
    /**
     * 查询AI模型
     *
     * @param id AI模型主键
     * @return AI模型
     */
    AiModel selectById(Long id);

    /**
     * 查询AI模型列表
     *
     * @param aiModel AI模型
     * @return AI模型集合
     */
    List<AiModel> selectList(AiModel aiModel);

    /**
     * 新增AI模型
     *
     * @param aiModel AI模型
     * @return 结果
     */
    int insert(AiModel aiModel);

    /**
     * 修改AI模型
     *
     * @param aiModel AI模型
     * @return 结果
     */
    int updateById(AiModel aiModel);

    /**
     * 删除AI模型
     *
     * @param id AI模型主键
     * @return 结果
     */
    int deleteById(Long id);

    /**
     * 批量删除AI模型
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteBatchIds(@Param("ids") Long[] ids);

    /**
     * 查询AI模型数量
     *
     * @param aiModel AI模型查询条件
     * @return AI模型数量
     */
    long selectCount(AiModel aiModel);

    /**
     * 批量根据ID查询模型
     */
    List<AiModel> selectBatchIds(@Param("idList") List<Long> idList);

    /**
     * 批量更新优先级（优先级+1）
     *
     * @param priority 起始优先级
     * @return 结果
     */
    int batchUpdatePriorityIncrement(@Param("priority") Integer priority);


    void incrementCallCount(@Param("id") Long id);



}
