package com.health.report.service;

import com.health.report.domain.AiModel;
import com.health.report.domain.MemberInfo;
import com.health.report.dto.model.AiModelDTO;
import com.health.report.dto.model.FieldParamImport;

import java.util.List;
import java.util.Map;

/**
 * AI模型Service接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface IAiModelService {
    /**
     * 查询AI模型
     *
     * @param id AI模型主键
     * @return AI模型
     */
    AiModel getById(Long id);

    /**
     * 查询AI模型列表
     *
     * @param aiModel 模型信息
     * @return AI模型集合
     */
    List<AiModel> page(AiModel aiModel);

    /**
     * 查询启用的模型列表
     *
     * @return AI模型集合
     */
    List<AiModel> listEnabled();

    /**
     * 查询可用的模型列表
     *
     * @param gender 性别
     * @return AI模型集合
     */
    List<AiModel> listAvailable(Integer gender);

    /**
     * 查询可用的模型列表（包含已填写关键参数数量）
     *
     * @param gender 性别
     * @param collectionId 采集记录ID
     * @return AI模型集合
     */
    List<AiModel> listAvailableWithParams(Integer gender, Long collectionId);

    /**
     * 新增AI模型
     *
     * @param dto AI模型DTO
     * @return 结果
     */
    AiModel create(AiModelDTO dto);

    /**
     * 修改AI模型
     *
     * @param id 模型ID
     * @param dto AI模型DTO
     * @return 结果
     */
    AiModel update(Long id, AiModelDTO dto);

    /**
     * 更新状态
     *
     * @param id 模型ID
     * @param status 状态
     */
    void updateStatus(Long id, int status);

    /**
     * 删除AI模型
     *
     * @param id AI模型主键
     */
    void deleteById(Long id);

    /**
     * 检查参数
     *
     * @param modelIds 多个模型ID
     * @param collectionId 采集记录ID
     * @return 检查结果
     */
    Map<String, Object> checkParams(String modelIds, Long collectionId);


    /**
     * 导入模型数据
     *
     * @param fieldList 模型参数数据列表
     * @return 结果
     */
    public Map<String, Object> importModel(List<FieldParamImport> fieldList,String lang);


}
