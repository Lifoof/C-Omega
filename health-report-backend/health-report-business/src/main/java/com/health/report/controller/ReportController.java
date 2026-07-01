package com.health.report.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.health.report.common.exception.ServiceException;
import com.health.report.common.utils.StringUtils;
import com.health.report.domain.Report;
import com.health.report.service.IReportService;
import com.health.report.service.impl.HealthReportPdfServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.enums.BusinessType;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.common.core.page.TableDataInfo;

/**
 * 健康报告Controller
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@RestController
@RequestMapping("/healthReport/report")
public class ReportController extends BaseController
{
    @Autowired
    private IReportService reportService;

    @Autowired
    private HealthReportPdfServiceImpl healthReportPdfService;

    @Value("${ruoyi.profile}")
    private String profile;

    /**
     * 查询健康报告列表
     */

    @GetMapping("/list")
    public TableDataInfo list(Report report)
    {
        startPage();
        List<Report> list = reportService.selectReportList(report);
        return getDataTable(list);
    }

    /**
     * 导出健康报告列表
     */
    @Log(title = "健康报告", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Report report)
    {
        List<Report> list = reportService.selectReportList(report);
        ExcelUtil<Report> util = new ExcelUtil<Report>(Report.class);
        util.exportExcel(response, list, "健康报告数据");
    }

    /**
     * 获取健康报告详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(reportService.selectReportById(id));
    }

    /**
     * 新增健康报告
     */

    @Log(title = "健康报告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Report report)
    {
        return toAjax(reportService.insertReport(report));
    }

    /**
     * 修改健康报告
     */

    @Log(title = "健康报告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Report report)
    {
        return toAjax(reportService.updateReport(report));
    }

    /**
     * 删除健康报告
     */

    @Log(title = "健康报告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(reportService.deleteReportByIds(ids));
    }

    /**
     * 生成健康报告【支持多选多个模型】
     */
    @Log(title = "健康报告", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody Map<String, Object> params) {
        // 1. 获取 collectionId
        Long collectionId = params.get("collectionId") != null ?
                Long.valueOf(params.get("collectionId").toString()) : null;

        // 2. 获取 多模型ID数组 → 转 List<Long>
        Object modelIdsObj = params.get("modelIds");
        List<Long> modelIds = new ArrayList<>();

        if (modelIdsObj instanceof List<?>) {
            modelIds = ((List<?>) modelIdsObj).stream()
                    .map(String::valueOf)
                    .map(Long::valueOf)
                    .toList();
        }

        // 3. 校验
        if (collectionId == null || modelIds.isEmpty()) {
            return AjaxResult.error("采集ID和模型ID不能为空");
        }

        // 4. 批量生成报告
        List<Report> reportList = reportService.generateReportBatch(collectionId, modelIds);

        // 5. 返回批量结果（前端可根据返回列表跳转）
        return AjaxResult.success("报告生成成功", reportList);
    }

    /**
     * 预览报告
     */
    @GetMapping("/{id}/preview")
    public void preview(@PathVariable("id") Long id, HttpServletResponse response)
    {
        try {
            Report report = reportService.selectReportById(id);
            String content = report.getReportContent();

            if (content == null || content.isEmpty()) {
                content = "<html><body><p>暂无报告内容</p></body></html>";
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(content);
        } catch (Exception e) {
            throw new RuntimeException("预览报告失败", e);
        }
    }

    /**
     * 下载报告
     */

    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") Long id,
                         @RequestParam(value = "lang", defaultValue = "zh") String lang,
                         HttpServletResponse response) {
        Report report = reportService.selectReportById(id);
        if (report == null) {
            throw new ServiceException("报告不存在");
        }

        String pdfPath;
        File file;

        // 根据语言选择对应的PDF路径
        if ("en".equals(lang)) {
            // 英文版本
            if (!StringUtils.hasText(report.getFilePathEn())) {
                throw new ServiceException("英文版报告未生成");
            }
            pdfPath = report.getFilePathEn();
        } else {
            // 中文版本
            if (!StringUtils.hasText(report.getFilePath())) {
                throw new ServiceException("中文版报告未生成");
            }
            pdfPath = report.getFilePath();
        }

        String realPath = pdfPath.replace("/profile", profile);
        file = new File(realPath);

        if (!file.exists()) {
            throw new ServiceException("PDF文件不存在");
        }

        // 设置响应头，强制下载
        response.setContentType("application/pdf");
        String filename = "en".equals(lang) ?
                "report_" + report.getReportNo() + "_en.pdf" :
                file.getName();
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setContentLengthLong(file.length());

        // 输出文件
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new ServiceException("文件下载异常");
        }
    }

}
