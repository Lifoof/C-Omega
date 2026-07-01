package com.health.report.controller;

import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.core.page.TableDataInfo;
import com.health.report.common.enums.BusinessType;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.domain.AiModel;
import com.health.report.dto.model.AiModelDTO;
import com.health.report.dto.model.FieldParamImport;
import com.health.report.service.IAiModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 模型Controller
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@RestController
@RequestMapping("/healthReport/aiModel")
public class AiModelController extends BaseController {

    @Autowired
    private IAiModelService aiModelService;

    /**
     * 查询模型列表
     */
    @GetMapping("/list")
    public TableDataInfo list(AiModel aiModel) {
        startPage();
        List<AiModel> list = aiModelService.page(aiModel);
        return getDataTable(list);
    }

    /**
     * 查询启用的模型列表
     */
    @GetMapping("/enabled")
    public AjaxResult listEnabled() {
        return success(aiModelService.listEnabled());
    }

    /**
     * 查询可用的模型列表
     */
    @GetMapping("/available")
    public AjaxResult listAvailable(@RequestParam(required = false) Integer gender) {
        return success(aiModelService.listAvailable(gender));
    }

    /**
     * 查询可用的模型列表（包含已填写关键参数数量）
     */
    @GetMapping("/availableWithParams")
    public AjaxResult listAvailableWithParams(@RequestParam(required = false) Integer gender,
                                              @RequestParam Long collectionId) {
        return success(aiModelService.listAvailableWithParams(gender, collectionId));
    }

    /**
     * 获取模型详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(aiModelService.getById(id));
    }

    /**
     * 新增模型
     */
    @Log(title = "模型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelDTO dto) {
        return success(aiModelService.create(dto));
    }

    /**
     * 修改模型
     */
    @Log(title = "模型", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable("id") Long id, @RequestBody AiModelDTO dto) {
        return success(aiModelService.update(id, dto));
    }

    /**
     * 更新状态
     */
    @Log(title = "模型", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        int status = body.get("status") instanceof Number ? ((Number) body.get("status")).intValue() : 1;
        aiModelService.updateStatus(id, status);
        return success();
    }

    /**
     * 检查参数
     */
    @GetMapping("/check-params/{collectionId}")
    public AjaxResult checkParams(String modelIds, @PathVariable Long collectionId) {
        return success(aiModelService.checkParams(modelIds, collectionId));
    }

    /**
     * 删除模型
     */
    @Log(title = "模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        aiModelService.deleteById(id);
        return success();
    }

    @Log(title = "模型", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "zh") String lang) throws Exception
    {
        ExcelUtil<FieldParamImport> util = new ExcelUtil<FieldParamImport>(FieldParamImport.class);
        List<FieldParamImport> fieldList = util.importExcel(file.getInputStream());
        Map<String, Object> result = aiModelService.importModel(fieldList,lang);
        return success(result);
    }
}
