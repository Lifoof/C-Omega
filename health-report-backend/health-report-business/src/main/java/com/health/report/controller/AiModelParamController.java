package com.health.report.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.enums.BusinessType;
import com.health.report.domain.AiModelParam;
import com.health.report.service.IAiModelParamService;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.common.core.page.TableDataInfo;

/**
 * 模型参数关联（关键+全部参数共用）Controller
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
@RestController
@RequestMapping("/health.report/aiModelParam")
public class AiModelParamController extends BaseController
{
    @Autowired
    private IAiModelParamService aiModelParamService;

    /**
     * 查询模型参数关联（关键+全部参数共用）列表
     */
    @PreAuthorize("@ss.hasPermi('health.report:aiModelParam:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiModelParam aiModelParam)
    {
        startPage();
        List<AiModelParam> list = aiModelParamService.selectAiModelParamList(aiModelParam);
        return getDataTable(list);
    }

    /**
     * 导出模型参数关联（关键+全部参数共用）列表
     */
    @PreAuthorize("@ss.hasPermi('health.report:aiModelParam:export')")
    @Log(title = "模型参数关联（关键+全部参数共用）", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiModelParam aiModelParam)
    {
        List<AiModelParam> list = aiModelParamService.selectAiModelParamList(aiModelParam);
        ExcelUtil<AiModelParam> util = new ExcelUtil<AiModelParam>(AiModelParam.class);
        util.exportExcel(response, list, "模型参数关联（关键+全部参数共用）数据");
    }

    /**
     * 获取模型参数关联（关键+全部参数共用）详细信息
     */
    @PreAuthorize("@ss.hasPermi('health.report:aiModelParam:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(aiModelParamService.selectAiModelParamById(id));
    }

    /**
     * 新增模型参数关联（关键+全部参数共用）
     */
    @PreAuthorize("@ss.hasPermi('health.report:aiModelParam:add')")
    @Log(title = "模型参数关联（关键+全部参数共用）", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelParam aiModelParam)
    {
        return toAjax(aiModelParamService.insertAiModelParam(aiModelParam));
    }

    /**
     * 修改模型参数关联（关键+全部参数共用）
     */
    @PreAuthorize("@ss.hasPermi('health.report:aiModelParam:edit')")
    @Log(title = "模型参数关联（关键+全部参数共用）", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModelParam aiModelParam)
    {
        return toAjax(aiModelParamService.updateAiModelParam(aiModelParam));
    }

    /**
     * 删除模型参数关联（关键+全部参数共用）
     */
    @PreAuthorize("@ss.hasPermi('health.report:aiModelParam:remove')")
    @Log(title = "模型参数关联（关键+全部参数共用）", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiModelParamService.deleteAiModelParamByIds(ids));
    }
}
