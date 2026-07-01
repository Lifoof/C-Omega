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
import com.health.report.domain.JsonRecord;
import com.health.report.service.IJsonRecordService;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.common.core.page.TableDataInfo;

/**
 * JSON记录Controller
 * 
 * @author ruoyi
 * @date 2026-04-09
 */
@RestController
@RequestMapping("/healthReport/jsonRecord")
public class JsonRecordController extends BaseController
{
    @Autowired
    private IJsonRecordService jsonRecordService;

    /**
     * 查询JSON记录列表
     */
    @PreAuthorize("@ss.hasPermi('healthReport:jsonRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(JsonRecord jsonRecord)
    {
        startPage();
        List<JsonRecord> list = jsonRecordService.selectJsonRecordList(jsonRecord);
        return getDataTable(list);
    }

    /**
     * 导出JSON记录列表
     */
    @PreAuthorize("@ss.hasPermi('healthReport:jsonRecord:export')")
    @Log(title = "JSON记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, JsonRecord jsonRecord)
    {
        List<JsonRecord> list = jsonRecordService.selectJsonRecordList(jsonRecord);
        ExcelUtil<JsonRecord> util = new ExcelUtil<JsonRecord>(JsonRecord.class);
        util.exportExcel(response, list, "JSON记录数据");
    }

    /**
     * 获取JSON记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('healthReport:jsonRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(jsonRecordService.selectJsonRecordById(id));
    }

    /**
     * 新增JSON记录
     */
    @PreAuthorize("@ss.hasPermi('healthReport:jsonRecord:add')")
    @Log(title = "JSON记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody JsonRecord jsonRecord)
    {
        return toAjax(jsonRecordService.insertJsonRecord(jsonRecord));
    }

    /**
     * 修改JSON记录
     */
    @PreAuthorize("@ss.hasPermi('healthReport:jsonRecord:edit')")
    @Log(title = "JSON记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody JsonRecord jsonRecord)
    {
        return toAjax(jsonRecordService.updateJsonRecord(jsonRecord));
    }

    /**
     * 删除JSON记录
     */
    @PreAuthorize("@ss.hasPermi('healthReport:jsonRecord:remove')")
    @Log(title = "JSON记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(jsonRecordService.deleteJsonRecordByIds(ids));
    }
}
