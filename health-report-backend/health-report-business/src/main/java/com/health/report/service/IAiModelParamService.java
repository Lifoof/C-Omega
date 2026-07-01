package com.health.report.service;

import java.util.List;
import com.health.report.domain.AiModelParam;

/**
 * 模型参数关联（关键+全部参数共用）Service接口
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
public interface IAiModelParamService 
{
    /**
     * 查询模型参数关联（关键+全部参数共用）
     * 
     * @param id 模型参数关联（关键+全部参数共用）主键
     * @return 模型参数关联（关键+全部参数共用）
     */
    public AiModelParam selectAiModelParamById(Long id);

    /**
     * 查询模型参数关联（关键+全部参数共用）列表
     * 
     * @param aiModelParam 模型参数关联（关键+全部参数共用）
     * @return 模型参数关联（关键+全部参数共用）集合
     */
    public List<AiModelParam> selectAiModelParamList(AiModelParam aiModelParam);

    /**
     * 新增模型参数关联（关键+全部参数共用）
     * 
     * @param aiModelParam 模型参数关联（关键+全部参数共用）
     * @return 结果
     */
    public int insertAiModelParam(AiModelParam aiModelParam);

    /**
     * 修改模型参数关联（关键+全部参数共用）
     * 
     * @param aiModelParam 模型参数关联（关键+全部参数共用）
     * @return 结果
     */
    public int updateAiModelParam(AiModelParam aiModelParam);

    /**
     * 批量删除模型参数关联（关键+全部参数共用）
     * 
     * @param ids 需要删除的模型参数关联（关键+全部参数共用）主键集合
     * @return 结果
     */
    public int deleteAiModelParamByIds(Long[] ids);

    /**
     * 删除模型参数关联（关键+全部参数共用）信息
     * 
     * @param id 模型参数关联（关键+全部参数共用）主键
     * @return 结果
     */
    public int deleteAiModelParamById(Long id);
}
