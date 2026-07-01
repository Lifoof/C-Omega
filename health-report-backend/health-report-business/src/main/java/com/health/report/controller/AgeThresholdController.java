package com.health.report.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.listener.DynamicThresholdImportListener;
import com.health.report.service.ICollectionFieldConfigService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.enums.BusinessType;
import com.health.report.domain.AgeThreshold;
import com.health.report.service.IAgeThresholdService;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.excel.enums.CellExtraTypeEnum;

/**
 * 指标年龄阈值Controller
 * 
 * @author ruoyi
 * @date 2026-04-21
 */
@RestController
@RequestMapping("/healthReport/ageThreshold")
public class AgeThresholdController extends BaseController
{
    @Autowired
    private IAgeThresholdService ageThresholdService;

    @Autowired
    private ICollectionFieldConfigService collectionFieldConfigService;

    /**
     * 查询指标年龄阈值列表
     */
    @GetMapping("/list")
    public TableDataInfo list(AgeThreshold ageThreshold)
    {
        startPage();
        List<AgeThreshold> list = ageThresholdService.selectAgeThresholdList(ageThreshold);
        return getDataTable(list);
    }

    /**
     * 导出指标年龄阈值列表
     */
    @Log(title = "指标年龄阈值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AgeThreshold ageThreshold)
    {
        List<AgeThreshold> list = ageThresholdService.selectAgeThresholdList(ageThreshold);
        ExcelUtil<AgeThreshold> util = new ExcelUtil<AgeThreshold>(AgeThreshold.class);
        util.exportExcel(response, list, "指标年龄阈值数据");
    }

    /**
     * 获取指标年龄阈值详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(ageThresholdService.selectAgeThresholdById(id));
    }

    /**
     * 新增指标年龄阈值
     */
    @Log(title = "指标年龄阈值", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AgeThreshold ageThreshold)
    {
        return toAjax(ageThresholdService.insertAgeThreshold(ageThreshold));
    }

    /**
     * 修改指标年龄阈值
     */
    @Log(title = "指标年龄阈值", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AgeThreshold ageThreshold)
    {
        return toAjax(ageThresholdService.updateAgeThreshold(ageThreshold));
    }

    /**
     * 删除指标年龄阈值
     */
    @Log(title = "指标年龄阈值", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(ageThresholdService.deleteAgeThresholdByIds(ids));
    }

    @Log(title = "年龄指标阈值导入", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "zh") String lang) throws IOException {
        // 1. 构建唯一映射：Excel表头指标中文名 -> 数据库fieldCode业务编码
        List<CollectionFieldConfig> fieldList = collectionFieldConfigService.selectCollectionFieldConfigList(null);
        Map<String, String> nameToCodeMap = fieldList.stream()
                .collect(Collectors.toMap(
                        CollectionFieldConfig::getFieldParamName,
                        CollectionFieldConfig::getFieldCode,
                        (oldVal, newVal) -> oldVal
                ));

        // 2. EasyExcel极简读取，无任何多余配置、无额外API
        EasyExcel.read(file.getInputStream(),
                        new DynamicThresholdImportListener(ageThresholdService, nameToCodeMap))
                .headRowNumber(0)
                .sheet()
                .doRead();

        // 返回提示 中英文
        String msg = "en".equals(lang) ? "Excel imported successfully" : "Excel导入成功";
        return AjaxResult.success(msg);
    }
}
