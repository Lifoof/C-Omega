package com.health.report.controller;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.excel.EasyExcel;
import com.health.report.domain.MemberInfo;
import com.health.report.dto.model.FieldParamImport;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.enums.BusinessType;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.service.ICollectionFieldConfigService;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 指标管理Controller
 * 
 * @author ruoyi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/healthReport/collectionFieldConfig")
public class CollectionFieldConfigController extends BaseController
{
    @Autowired
    private ICollectionFieldConfigService collectionFieldConfigService;

    /**
     * 查询指标管理列表
     */
    @GetMapping("/list")
    public TableDataInfo list(CollectionFieldConfig collectionFieldConfig)
    {
        startPage();
        List<CollectionFieldConfig> list = collectionFieldConfigService.selectCollectionFieldConfigList(collectionFieldConfig);
        return getDataTable(list);
    }

    @Log(title = "指标管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectionFieldConfig collectionFieldConfig,
                       @RequestParam(required = false, defaultValue = "zh") String languageType)
    {
        List<CollectionFieldConfig> list = collectionFieldConfigService.selectCollectionFieldConfigList(collectionFieldConfig);

        boolean isEnglish = "en".equalsIgnoreCase(languageType);
        String sheetName = isEnglish ? "Field Config Data" : "指标管理数据";

        // 使用 EasyExcel 导出，支持动态表头
        List<List<String>> headers = new ArrayList<>();
        if (isEnglish) {
            headers.add(List.of("Category"));
            headers.add(List.of("Category (EN)"));
            headers.add(List.of("Field Name"));
            headers.add(List.of("Field Name (EN)"));
            headers.add(List.of("Unit"));
            headers.add(List.of("Unit (EN)"));
            headers.add(List.of("Reference Range"));
            headers.add(List.of("Reference Range (EN)"));
            headers.add(List.of("Alias"));
            headers.add(List.of("Alias (EN)"));
            headers.add(List.of("Remark"));
            headers.add(List.of("Remark (EN)"));
        } else {
            headers.add(List.of("归属类别"));
            headers.add(List.of("归属类别英文"));
            headers.add(List.of("指标名称"));
            headers.add(List.of("指标名称英文"));
            headers.add(List.of("指标单位"));
            headers.add(List.of("指标单位英文"));
            headers.add(List.of("参考值/范围"));
            headers.add(List.of("参考值/范围英文"));
            headers.add(List.of("别名"));
            headers.add(List.of("别名英文"));
            headers.add(List.of("备注"));
            headers.add(List.of("备注英文"));
        }

        // 准备数据
        List<List<Object>> data = new ArrayList<>();
        for (CollectionFieldConfig config : list) {
            List<Object> row = new ArrayList<>();
            row.add(config.getCategoryName());
            row.add(config.getCategoryNameEn());
            row.add(config.getFieldName());
            row.add(config.getFieldNameEn());
            row.add(config.getUnit());
            row.add(config.getUnitEn());
            row.add(config.getRefRangeText());
            row.add(config.getRefRangeTextEn());
            row.add(config.getAliasName());
            row.add(config.getAliasNameEn());
            row.add(config.getRemark());
            row.add(config.getRemarkEn());
            data.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = isEnglish
                    ? "Field_Config_" + System.currentTimeMillis() + ".xlsx"
                    : "指标配置_" + System.currentTimeMillis() + ".xlsx";
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            EasyExcel.write(response.getOutputStream())
                    .head(headers)
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 获取指标管理详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(collectionFieldConfigService.selectCollectionFieldConfigById(id));
    }

    /**
     * 新增指标管理
     */
    @Log(title = "指标管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollectionFieldConfig collectionFieldConfig)
    {
        return toAjax(collectionFieldConfigService.insertCollectionFieldConfig(collectionFieldConfig));
    }

    /**
     * 修改指标管理
     */
    @Log(title = "指标管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectionFieldConfig collectionFieldConfig)
    {
        return toAjax(collectionFieldConfigService.updateCollectionFieldConfig(collectionFieldConfig));
    }

    /**
     * 删除指标管理
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        try {
            return toAjax(collectionFieldConfigService.deleteCollectionFieldConfigByIds(ids));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "指标管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "zh") String lang)
    {
        try {
            ExcelUtil<CollectionFieldConfig> util = new ExcelUtil<CollectionFieldConfig>(CollectionFieldConfig.class);
            List<CollectionFieldConfig> fieldList = util.importExcel(file.getInputStream());
            String msg = collectionFieldConfigService.importCollectionFieldConfig(fieldList,lang);
            return AjaxResult.success(msg);
        } catch (Exception e) {
            return AjaxResult.error("导入失败：" + e.getMessage());
        }
    }
}
