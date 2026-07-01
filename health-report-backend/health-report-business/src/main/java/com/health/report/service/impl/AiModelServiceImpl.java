package com.health.report.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.health.report.common.exception.BusinessException;
import com.health.report.common.exception.ServiceException;
import com.health.report.common.result.ResultCode;
import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.SecurityUtils;
import com.health.report.common.utils.StringUtils;
import com.health.report.domain.*;
import com.health.report.dto.model.AiModelDTO;
import com.health.report.dto.model.FieldParamImport;
import com.health.report.mapper.*;
import com.health.report.service.IAiModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.health.report.common.utils.SecurityUtils.isAdmin;

/**
 * AI模型Service业务层处理
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@Service
public class AiModelServiceImpl implements IAiModelService {

    @Autowired
    private AiModelMapper aiModelMapper;
    @Autowired
    private CollectionRecordMapper recordMapper;
    @Autowired
    private CollectionDataItemMapper dataItemMapper;
    @Autowired
    private CollectionCategoryConfigMapper collectionCategoryConfigMapper;
    @Autowired
    private CollectionFieldConfigMapper collectionFieldConfigMapper;
    @Autowired
    private AiModelParamMapper aiModelParamMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public AiModel getById(Long id) {
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new ServiceException("AI模型不存在");
        }
        return model;
    }

    @Override
    public List<AiModel> page(AiModel aiModel) {
        // 简化处理，实际应该分页
        return aiModelMapper.selectList(aiModel);
    }

    @Override
    public List<AiModel> listEnabled() {
        AiModel query = new AiModel();
        query.setStatus(1);
        return aiModelMapper.selectList(query);
    }

    @Override
    public List<AiModel> listAvailable(Integer gender) {
        AiModel query = new AiModel();
        query.setApplicableGender(gender);
        query.setStatus(1);
        return aiModelMapper.selectList(query);
    }

    @Override
    public List<AiModel> listAvailableWithParams(Integer gender, Long collectionId) {
        // 1. 查询符合条件的模型列表
        AiModel query = new AiModel();
        query.setApplicableGender(gender);
        query.setStatus(1);
        List<AiModel> models = aiModelMapper.selectList(query);

        if (collectionId == null || models.isEmpty()) {
            return models;
        }

        // 2. 查询该采集记录已填写的所有字段编码
        List<String> filledFieldCodes = dataItemMapper.selectFilledFieldCodes(collectionId);
        if (filledFieldCodes.isEmpty()) {
            // 没有填写任何参数，所有模型的 filledKeyParams 都为 0
            models.forEach(m -> m.setFilledKeyParams(0));
            return models;
        }

        // 3. 遍历每个模型，计算已填写的关键参数数量
        for (AiModel model : models) {
            // 查询该模型的关键参数字段编码
            List<String> requiredFieldCodes = aiModelParamMapper.selectRequiredFieldCodesByModelId(model.getId());
            if (requiredFieldCodes.isEmpty()) {
                model.setFilledKeyParams(0);
                continue;
            }

            // 计算已填写的关键参数数量（交集）
            int filledCount = (int) requiredFieldCodes.stream()
                    .filter(filledFieldCodes::contains)
                    .count();
            model.setFilledKeyParams(filledCount);
        }

        return models;
    }

    @Override
    @Transactional
    public AiModel create(AiModelDTO dto) {
        if (!isAdmin()) throw new BusinessException(ResultCode.FORBIDDEN);
        // 处理优先级顺延
        Integer newPriority = dto.getPriority() != null ? dto.getPriority() : 1;
        aiModelMapper.batchUpdatePriorityIncrement(newPriority);
        // 1. 插入模型主表
        AiModel e = new AiModel();
        e.setModelName(dto.getModelName());
        e.setModelNameEn(dto.getModelNameEn());
        e.setCategory(dto.getCategory());
        e.setApplicableGender(dto.getApplicableGender() != null ? dto.getApplicableGender() : 0);
        e.setDescription(dto.getDescription());
        e.setRequiredParams(dto.getRequiredParams());
        e.setRequiredParamCount(dto.getRequiredParamCount() != null ? dto.getRequiredParamCount() : 0);
        e.setAllParams(dto.getAllParams());
        e.setAllParamCount(dto.getAllParamCount() != null ? dto.getAllParamCount() : 0);
        e.setParamCount(dto.getParamCount() != null ? dto.getParamCount() : 0);
        e.setCallCount(0);
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        e.setCreateBy(SecurityUtils.getLoginUser().getUsername());
        e.setCreateTime(DateUtils.getNowDate());
        e.setPyPath(dto.getPyPath());
        e.setPklPath(dto.getPklPath());
        e.setPriority(newPriority);
        aiModelMapper.insert(e);

        // ===================== 保存【必填参数】requiredParams =====================
        String requiredParamsJson = dto.getRequiredParams();
        if (requiredParamsJson != null && !requiredParamsJson.isEmpty()) {
            List<String> requiredCodeList;
            try {
                requiredCodeList = new ObjectMapper().readValue(requiredParamsJson, new TypeReference<List<String>>() {});
            } catch (Exception ex) {
                throw new BusinessException("必填参数格式错误");
            }

            List<Long> requiredIds = collectionFieldConfigMapper.selectIdsByFieldCodes(requiredCodeList);
            if (requiredIds.size() != requiredCodeList.size()) {
                throw new BusinessException("存在无效必填参数");
            }

            List<AiModelParam> paramList = new ArrayList<>();
            for (int i = 0; i < requiredIds.size(); i++) {
                AiModelParam p = new AiModelParam();
                p.setModelId(e.getId());
                p.setParamId(requiredIds.get(i));
                p.setParamType("required");
                p.setSort(i + 1);
                p.setCreateTime(new Date());
                p.setUpdateTime(new Date());
                paramList.add(p);
            }
            if (!paramList.isEmpty()) {
                aiModelParamMapper.batchInsert(paramList);
            }
        }

        // ===================== 保存【全部参数】allParams =====================
        String allParamsJson = dto.getAllParams();
        if (allParamsJson != null && !allParamsJson.isEmpty()) {
            List<String> allCodeList;
            try {
                allCodeList = new ObjectMapper().readValue(allParamsJson, new TypeReference<List<String>>() {});
            } catch (Exception ex) {
                throw new BusinessException("全部参数格式错误");
            }

            List<Long> allIds = collectionFieldConfigMapper.selectIdsByFieldCodes(allCodeList);
            if (allIds.size() != allCodeList.size()) {
                throw new BusinessException("存在无效全部参数");
            }

            List<AiModelParam> paramList = new ArrayList<>();
            for (int i = 0; i < allIds.size(); i++) {
                AiModelParam p = new AiModelParam();
                p.setModelId(e.getId());
                p.setParamId(allIds.get(i));
                p.setParamType("all"); // 全部参数标记
                p.setSort(i + 1);
                p.setCreateTime(new Date());
                p.setUpdateTime(new Date());
                paramList.add(p);
            }
            if (!paramList.isEmpty()) {
                aiModelParamMapper.batchInsert(paramList);
            }
        }

        return e;
    }

    @Override
    @Transactional
    public AiModel update(Long id, AiModelDTO dto) {
        if (!isAdmin()) throw new BusinessException(ResultCode.FORBIDDEN);

        // 1. 更新模型主表
        AiModel e = getById(id);
        Integer oldPriority = e.getPriority();
        Integer newPriority = dto.getPriority() != null ? dto.getPriority() : 1;

        // 处理优先级顺延（只有当优先级发生变化时才需要顺延）
        if (!newPriority.equals(oldPriority)) {
            // 先将当前模型的优先级设为 null，避免冲突
            e.setPriority(null);
            aiModelMapper.updateById(e);
            // 然后顺延其他模型的优先级
            aiModelMapper.batchUpdatePriorityIncrement(newPriority);
        }
        e.setModelName(dto.getModelName());
        e.setModelNameEn(dto.getModelNameEn());
        e.setCategory(dto.getCategory());
        e.setApplicableGender(dto.getApplicableGender() != null ? dto.getApplicableGender() : 0);
        e.setDescription(dto.getDescription());
        e.setRequiredParams(dto.getRequiredParams());
        e.setRequiredParamCount(dto.getRequiredParamCount() != null ? dto.getRequiredParamCount() : 0);
        e.setAllParams(dto.getAllParams());
        e.setAllParamCount(dto.getAllParamCount() != null ? dto.getAllParamCount() : 0);
        e.setParamCount(dto.getParamCount() != null ? dto.getParamCount() : 0);
        e.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        e.setPyPath(dto.getPyPath());
        e.setPklPath(dto.getPklPath());
        e.setUpdateBy(SecurityUtils.getLoginUser().getUsername());
        e.setUpdateTime(new Date());
        aiModelMapper.updateById(e);

        // ===================== 🔥 关键：先删除该模型的所有旧参数 =====================
        aiModelParamMapper.deleteByModelId(id);

        // ===================== 重新保存【必填参数】requiredParams =====================
        String requiredParamsJson = dto.getRequiredParams();
        if (requiredParamsJson != null && !requiredParamsJson.isEmpty()) {
            List<String> requiredCodeList;
            try {
                requiredCodeList = new ObjectMapper().readValue(requiredParamsJson, new TypeReference<List<String>>() {});
            } catch (Exception ex) {
                throw new BusinessException("必填参数格式错误");
            }

            List<Long> requiredIds = collectionFieldConfigMapper.selectIdsByFieldCodes(requiredCodeList);
            if (requiredIds.size() != requiredCodeList.size()) {
                throw new BusinessException("存在无效必填参数");
            }

            List<AiModelParam> paramList = new ArrayList<>();
            for (int i = 0; i < requiredIds.size(); i++) {
                AiModelParam p = new AiModelParam();
                p.setModelId(e.getId());
                p.setParamId(requiredIds.get(i));
                p.setParamType("required");
                p.setSort(i + 1);
                p.setCreateTime(new Date());
                p.setUpdateTime(new Date());
                paramList.add(p);
            }
            if (!paramList.isEmpty()) {
                aiModelParamMapper.batchInsert(paramList);
            }
        }

        // ===================== 重新保存【全部参数】allParams =====================
        String allParamsJson = dto.getAllParams();
        if (allParamsJson != null && !allParamsJson.isEmpty()) {
            List<String> allCodeList;
            try {
                allCodeList = new ObjectMapper().readValue(allParamsJson, new TypeReference<List<String>>() {});
            } catch (Exception ex) {
                throw new BusinessException("全部参数格式错误");
            }

            List<Long> allIds = collectionFieldConfigMapper.selectIdsByFieldCodes(allCodeList);
            if (allIds.size() != allCodeList.size()) {
                throw new BusinessException("存在无效全部参数");
            }

            List<AiModelParam> paramList = new ArrayList<>();
            for (int i = 0; i < allIds.size(); i++) {
                AiModelParam p = new AiModelParam();
                p.setModelId(e.getId());
                p.setParamId(allIds.get(i));
                p.setParamType("all");
                p.setSort(i + 1);
                p.setCreateTime(new Date());
                p.setUpdateTime(new Date());
                paramList.add(p);
            }
            if (!paramList.isEmpty()) {
                aiModelParamMapper.batchInsert(paramList);
            }
        }

        return e;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, int status) {
        AiModel model = getById(id);
        model.setStatus(status);
        model.setUpdateBy(SecurityUtils.getLoginUser().getUsername());
        model.setUpdateTime(DateUtils.getNowDate());
        aiModelMapper.updateById(model);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        getById(id);
        final int i = aiModelMapper.deleteById(id);
        if (i >0) {
            aiModelParamMapper.deleteByModelId(id);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> checkParams(String modelIds, Long collectionId) {
        // 1. 校验采集记录
        CollectionRecord record = recordMapper.selectById(collectionId);
        if (record == null) {
            throw new BusinessException(ResultCode.COLLECTION_NOT_FOUND);
        }

        // 2. 一次性查询所有已填写的字段编码
        List<String> filledCodes = dataItemMapper.selectFilledFieldCodes(collectionId);
        int totalFields = collectionFieldConfigMapper.selectCountAllEnabled();
        // 3. 整体完整度（采集记录自身的完整度）
        double overallCompleteness = 0.0;
        if (totalFields > 0 && record.getCompleteness() != null) {
            overallCompleteness = record.getCompleteness().doubleValue();
        }

        // ====================== 核心改动 ======================
        // 4. 合并所有模型的【全部必填关键参数】（自动去重）
        Set<String> allRequiredCodes = new HashSet<>();

        // 5. 合并所有模型的【缺失参数】
        Set<String> allMissingCodes = new HashSet<>();
        List<Long> modelIdList = Arrays.stream(modelIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toList();
        for (Long modelId : modelIdList) {
            AiModel model = aiModelMapper.selectById(modelId);
            if (model == null) continue;

            List<String> requiredCodes = parseRequiredParams(model.getRequiredParams());
            allRequiredCodes.addAll(requiredCodes);

            // 找出当前模型缺失的
            List<String> missing = requiredCodes.stream()
                    .filter(code -> !filledCodes.contains(code))
                    .toList();

            allMissingCodes.addAll(missing);
        }

        // 6. 总统计（所有模型合并后）
        int totalKeyParams = allRequiredCodes.size();               // 所有模型关键参数总数（去重）
        int missingKeyParams = allMissingCodes.size();             // 所有模型缺失总数
        int filledKeyParams = totalKeyParams - missingKeyParams;   // 已填写数量

        // 7. 合并后的关键参数完整度
        double keyCompleteness = totalKeyParams == 0 ? 100.0
                : (double) filledKeyParams / totalKeyParams * 100;

        // ====================== 统一返回一个结果 ======================
        Map<String, Object> result = new HashMap<>();
        result.put("totalKeyParams", totalKeyParams);          // 所有模型关键参数总数（去重）
        result.put("filledKeyParams", filledKeyParams);        // 已填写关键参数
        result.put("missingKeyParams", missingKeyParams);      // 缺失关键参数数量
        result.put("missingRequired", new ArrayList<>(allMissingCodes)); // 缺失的字段列表
        result.put("keyCompleteness", Math.round(keyCompleteness * 100) / 100.0); // 整体关键参数完整度
        result.put("overallCompleteness", overallCompleteness); // 采集记录整体完整度

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importModel(List<FieldParamImport> fieldList, String lang) {
        Map<String, Object> result = new HashMap<>();

        // 1. 空数据直接返回
        if (CollectionUtils.isEmpty(fieldList)) {
            String msg = "en".equals(lang) ? "Import failed: Data cannot be empty" : "导入失败：数据不能为空";
            result.put("msg", msg);
            result.put("total", 0);
            result.put("success", 0);
            result.put("exist", 0);
            result.put("categories", new ArrayList<>());
            return result;
        }

        // ====================== 【严格校验】去空格 + 非空校验 ======================
        // 规则：任何一行 指标名称 为空 → 整个导入失败，返回行号
        for (int i = 0; i < fieldList.size(); i++) {
            FieldParamImport item = fieldList.get(i);
            if (item == null) {
                String rowMsg = "en".equals(lang)
                        ? "Import failed: Row " + (i + 1) + " data is empty"
                        : "导入失败：第" + (i + 1) + "行数据为空";
                result.put("msg", rowMsg);
                result.put("total", fieldList.size());
                result.put("success", 0);
                result.put("exist", 0);
                result.put("categories", new ArrayList<>());
                return result;
            }

            // 去空格 - 只处理指标名称
            String fieldName = trim(item.getFieldName());
            item.setFieldName(fieldName);

            // 空值 → 直接失败，不执行任何DB操作
            if (StringUtils.isBlank(fieldName)) {
                String fieldMsg = "en".equals(lang)
                        ? "Import failed: Row " + (i + 1) + " [Indicator Name] is not filled"
                        : "导入失败：第" + (i + 1) + "行【指标名称】未填写";
                result.put("msg", fieldMsg);
                result.put("total", fieldList.size());
                result.put("success", 0);
                result.put("exist", 0);
                result.put("categories", new ArrayList<>());
                return result;
            }
        }

        // ====================== 校验通过，才执行后续DB操作 ======================

        // 1. 提取所有指标名称
        Set<String> fieldNameSet = fieldList.stream()
                .map(FieldParamImport::getFieldName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 2. 查询已存在的参数（根据指标名称）
        List<CollectionFieldConfig> existFields = collectionFieldConfigMapper.listByFieldNames(fieldNameSet);

        // 3. 检查是否有不存在的指标
        Set<String> existFieldNames = existFields.stream()
                .map(CollectionFieldConfig::getFieldParamName)
                .collect(Collectors.toSet());

        List<String> notFoundFields = fieldList.stream()
                .map(FieldParamImport::getFieldName)
                .filter(name -> !existFieldNames.contains(name))
                .toList();

        if (!notFoundFields.isEmpty()) {
            String notFoundMsg = "en".equals(lang)
                    ? "Import failed: The following indicators do not exist - " + String.join(", ", notFoundFields)
                    : "导入失败：以下指标不存在 - " + String.join(", ", notFoundFields);
            result.put("msg", notFoundMsg);
            result.put("total", fieldList.size());
            result.put("success", 0);
            result.put("exist", 0);
            result.put("categories", new ArrayList<>());
            return result;
        }

        // 4. 组装返回格式 - 按原有分类组织
        // 获取所有涉及的分类编码
        Set<String> categoryCodes = existFields.stream()
                .map(CollectionFieldConfig::getCategoryCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 查询分类信息
        List<CollectionCategoryConfig> categoriesList = collectionCategoryConfigMapper.listByCategoryCodes(categoryCodes);
        Map<String, CollectionCategoryConfig> categoryMap = categoriesList.stream()
                .filter(c -> c.getCategoryCode() != null && !c.getCategoryCode().isEmpty())
                .collect(Collectors.toMap(CollectionCategoryConfig::getCategoryCode, c -> c, (existing, replacement) -> existing));


        // 按分类分组
        Map<String, List<CollectionFieldConfig>> fieldGroupMap = existFields.stream()
                .collect(Collectors.groupingBy(f -> f.getCategoryCode() != null ? f.getCategoryCode() : "DEFAULT"));

        // 构建返回结构
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, List<CollectionFieldConfig>> entry : fieldGroupMap.entrySet()) {
            String categoryCode = entry.getKey();
            List<CollectionFieldConfig> fields = entry.getValue();

            // 按排序号排序
            fields.sort(Comparator.comparingInt(CollectionFieldConfig::getSortOrder));

            CollectionCategoryConfig category = categoryMap.get(categoryCode);
            if (category != null) {
                Map<String, Object> categoryVO = new HashMap<>();
                categoryVO.put("id", category.getId());
                categoryVO.put("categoryCode", category.getCategoryCode());
                categoryVO.put("categoryName", category.getCategoryName());
                categoryVO.put("categoryNameEn", category.getCategoryNameEn());
                categoryVO.put("genderScope", category.getGenderScope());
                categoryVO.put("icon", category.getIcon());
                categoryVO.put("sortOrder", category.getSortOrder());
                categoryVO.put("status", category.getStatus());
                categoryVO.put("fields", fields);
                categories.add(categoryVO);
            } else {
                // 分类不存在的情况，放入默认分类
                Map<String, Object> defaultCategory = new HashMap<>();
                defaultCategory.put("id", 0);
                defaultCategory.put("categoryCode", categoryCode);
                // 未分类 中英文
                String uncategorized = "en".equals(lang) ? "Uncategorized" : "未分类";
                defaultCategory.put("categoryName", uncategorized);
                defaultCategory.put("genderScope", 0);
                defaultCategory.put("icon", null);
                defaultCategory.put("sortOrder", 0);
                defaultCategory.put("status", 1);
                defaultCategory.put("fields", fields);
                categories.add(defaultCategory);
            }
        }

        // 按分类排序号排序
        categories.sort((a, b) -> {
            Integer sortA = (Integer) a.get("sortOrder");
            Integer sortB = (Integer) b.get("sortOrder");
            return Integer.compare(sortA != null ? sortA : 0, sortB != null ? sortB : 0);
        });

        // 成功提示
        String successMsg = "en".equals(lang)
                ? "Import successful: Total " + fieldList.size() + " rows"
                : "导入成功：共" + fieldList.size() + "行";
        result.put("msg", successMsg);
        result.put("total", fieldList.size());
        result.put("success", existFields.size());
        result.put("exist", 0);
        result.put("categories", categories);

        return result;
    }

    // 安全去空格
    private String trim(String str) {
        return str == null ? null : str.trim();
    }

    private List<String> parseRequiredParams(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
