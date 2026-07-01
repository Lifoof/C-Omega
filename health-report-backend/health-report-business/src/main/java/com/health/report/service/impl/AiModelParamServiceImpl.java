package com.health.report.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.health.report.mapper.AiModelParamMapper;
import com.health.report.domain.AiModelParam;
import com.health.report.service.IAiModelParamService;

/**
 * 模型参数关联（关键+全部参数共用）Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
@Service
public class AiModelParamServiceImpl implements IAiModelParamService 
{
    @Autowired
    private AiModelParamMapper aiModelParamMapper;

    /**
     * 查询模型参数关联（关键+全部参数共用）
     * 
     * @param id 模型参数关联（关键+全部参数共用）主键
     * @return 模型参数关联（关键+全部参数共用）
     */
    @Override
    public AiModelParam selectAiModelParamById(Long id)
    {
        return aiModelParamMapper.selectAiModelParamById(id);
    }

    /**
     * 查询模型参数关联（关键+全部参数共用）列表
     * 
     * @param aiModelParam 模型参数关联（关键+全部参数共用）
     * @return 模型参数关联（关键+全部参数共用）
     */
    @Override
    public List<AiModelParam> selectAiModelParamList(AiModelParam aiModelParam)
    {
        return aiModelParamMapper.selectAiModelParamList(aiModelParam);
    }

    /**
     * 新增模型参数关联（关键+全部参数共用）
     * 
     * @param aiModelParam 模型参数关联（关键+全部参数共用）
     * @return 结果
     */
    @Override
    public int insertAiModelParam(AiModelParam aiModelParam)
    {
        aiModelParam.setCreateTime(new Date());
        return aiModelParamMapper.insertAiModelParam(aiModelParam);
    }

    /**
     * 修改模型参数关联（关键+全部参数共用）
     * 
     * @param aiModelParam 模型参数关联（关键+全部参数共用）
     * @return 结果
     */
    @Override
    public int updateAiModelParam(AiModelParam aiModelParam)
    {
        aiModelParam.setUpdateTime(new Date());
        return aiModelParamMapper.updateAiModelParam(aiModelParam);
    }

    /**
     * 批量删除模型参数关联（关键+全部参数共用）
     * 
     * @param ids 需要删除的模型参数关联（关键+全部参数共用）主键
     * @return 结果
     */
    @Override
    public int deleteAiModelParamByIds(Long[] ids)
    {
        return aiModelParamMapper.deleteAiModelParamByIds(ids);
    }

    /**
     * 删除模型参数关联（关键+全部参数共用）信息
     * 
     * @param id 模型参数关联（关键+全部参数共用）主键
     * @return 结果
     */
    @Override
    public int deleteAiModelParamById(Long id)
    {
        return aiModelParamMapper.deleteAiModelParamById(id);
    }
}
