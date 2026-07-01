package com.health.report.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.core.page.TableDataInfo;
import com.health.report.common.enums.BusinessType;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.domain.CollectionRecord;
import com.health.report.dto.collection.*;
import com.health.report.service.ICollectionConfigService;
import com.health.report.service.ICollectionFieldConfigService;
import com.health.report.service.ICollectionRecordService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * 采集记录Controller
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@RestController
@RequestMapping("/healthReport/collectionRecord")
public class CollectionRecordController extends BaseController {

    @Autowired
    private ICollectionRecordService collectionRecordService;

    @Autowired
    private ICollectionConfigService collectionConfigService;

    /**
     * 查询采集记录列表
     */
    @GetMapping("/list")
    public TableDataInfo list(CollectionRecord collectionRecord) {
        startPage();
        List<CollectionRecord> list = collectionRecordService.page(collectionRecord);
        return getDataTable(list);
    }

    /**
     * 获取采集记录详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(collectionRecordService.getById(id));
    }

    /**
     * 获取采集记录详情（包含分类数据）
     */
    @GetMapping("/detail/{id}")
    public AjaxResult getDetail(@PathVariable("id") Long id) {
        return success(collectionRecordService.getDetail(id));
    }

    /**
     * 新增采集记录
     */

    @Log(title = "采集记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollectionRecord collectionRecord) {
        return success(collectionRecordService.create(collectionRecord));
    }

    /**
     * 保存分类条目数据
     */

    @Log(title = "采集记录", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/categoryItem")
    public AjaxResult saveCategoryDataItem(@PathVariable("id") Long id, @RequestBody CategorySaveDTO dto) {
        collectionRecordService.saveCategoryDataItem(id, dto);
        return success();
    }

    @Log(title = "采集记录关键参数", businessType = BusinessType.UPDATE)
    @PostMapping("/saveBatchCategoryData")
    public AjaxResult saveBatchCategoryData(@RequestBody BatchCategorySaveDTO dto) {
        collectionRecordService.saveBatchCategoryData(dto);
        return success();
    }

    /**
     * 完成采集记录
     */

    @Log(title = "采集记录", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/complete")
    public AjaxResult complete(@PathVariable("id") Long id) {
        collectionRecordService.complete(id);
        return success();
    }

    /**
     * 删除采集记录
     */

    @Log(title = "采集记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        collectionRecordService.deleteById(id);
        return success();
    }

    /**
     * 下载体检信息采集模板
     */
    @PostMapping("/templateDownload")
    public void templateDownload(@RequestParam(required = false, defaultValue = "zh") String languageType,
                                 HttpServletResponse response) throws IOException {
        // 判断是否为英文
        final boolean isEnglish = "en".equalsIgnoreCase(languageType);

        // 1. 【动态获取】体检指标列（这里模拟，实际从数据库/配置接口获取）
        List<String> dynamicCheckItems = isEnglish
                ? collectionConfigService.getCollectionConfigNameEn()
                : collectionConfigService.getCollectionConfigName();

        // 获取指标字段配置信息（用于别名提示）
        List<CollectionFieldConfig> fieldConfigs = collectionConfigService.getCollectionFieldConfigList();
        // 构建字段名到别名的映射
        Map<String, String> fieldAliasMap = fieldConfigs.stream()
                .filter(f -> f.getAliasName() != null && !f.getAliasName().isEmpty())
                .collect(Collectors.toMap(
                        f -> isEnglish ? f.getFieldNameEn() : f.getFieldName(),
                        f -> isEnglish ? f.getAliasNameEn() : f.getAliasName(),
                        (a, b) -> a // 处理重复键
                ));

        // 2. 固定基础列（带*必填）
        List<String> baseHeaders = new ArrayList<>();
        if (isEnglish) {
            baseHeaders.add("User NO");
            baseHeaders.add("Name");
            baseHeaders.add("Gender");
            baseHeaders.add("Checkup Date");
        } else {
            baseHeaders.add("用户编号");
            baseHeaders.add("用户姓名");
            baseHeaders.add("性别");
            baseHeaders.add("体检日期");
        }

        // 3. 最终表头 = 基础列 + 动态体检列
        List<List<String>> finalHead = new ArrayList<>();
        // 先加基础列
        for (String header : baseHeaders) {
            finalHead.add(List.of(header));
        }
        // 再加动态体检列
        for (String item : dynamicCheckItems) {
            finalHead.add(List.of(item));
        }

        // 4. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 设置文件名
        String fileName = isEnglish ? "Physical_Examination_Template" : "体检信息采集模板";
        response.setHeader("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(fileName + ".xlsx", "UTF-8"));

        // 5. 样式处理器：给前4列（必填项）设置红色背景+红色加粗字体
        CellWriteHandler styleHandler = new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                // 只处理表头行（第1行，索引0）
                if (!context.getRowIndex().equals(0)) return;

                WriteCellData<?> cellData = context.getFirstCellData();
                WriteCellStyle style = cellData.getOrCreateStyle();
                Integer colIndex = context.getColumnIndex();

                // 前4列（用户编号、用户姓名、性别、体检日期）设置样式
                // 前4列（必填项）：淡红色背景 + 白色字体 + 居中 + 行高适配
                if (colIndex < 4) {
                    // 淡红色背景（和导入模板一致）
                    style.setFillForegroundColor(IndexedColors.RED.getIndex());
                    style.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
                    // 白色字体（和导入模板一致）
                    WriteFont font = new WriteFont();
                    font.setColor(IndexedColors.WHITE.getIndex());
                    font.setBold(true); // 加粗，和导入模板一致
                    font.setFontHeightInPoints((short) 12);
                    style.setWriteFont(font);
                    // 居中对齐
                    style.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    style.setVerticalAlignment(VerticalAlignment.CENTER);
                } else {
                    // 动态列：灰色背景 + 黑色字体（和导入模板一致）
                    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    style.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
                    WriteFont font = new WriteFont();
                    font.setColor(IndexedColors.BLACK.getIndex());
                    font.setBold(true);
                    font.setFontHeightInPoints((short) 12);
                    style.setWriteFont(font);
                    style.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    style.setVerticalAlignment(VerticalAlignment.CENTER);
                }
            }
        };

        // 6. 数据验证处理器（性别下拉框、必填提示等）- 性能优化版
        CellWriteHandler validationHandler = new CellWriteHandler() {
            private boolean initialized = false;

            @Override
            public void afterCellCreate(CellWriteHandlerContext context) {
                // 只在首次调用时执行，避免重复添加验证
                if (initialized) return;

                Sheet sheet = context.getWriteWorkbookHolder().getWorkbook().getSheetAt(0);
                XSSFSheet xssfSheet = (XSSFSheet) sheet;
                XSSFDataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);

                // 多语言提示文本
                String userTitle = isEnglish ? "Required" : "必填提示";
                String userText = isEnglish ? "Please enter User NO (required). This is the User NO from User Info." : "请输入用户编号（必填）。此编号为用户信息中的用户编号";
                String userErrorTitle = isEnglish ? "Input Error" : "输入错误";
                String userErrorText = isEnglish ? "User NO cannot be empty" : "用户编号不能为空，请输入用户编号";

                String nameTitle = isEnglish ? "Required" : "必填提示";
                String nameText = isEnglish ? "Please enter Name (required)" : "请输入用户姓名（必填）";
                String nameErrorTitle = isEnglish ? "Input Error" : "输入错误";
                String nameErrorText = isEnglish ? "Name cannot be empty" : "用户姓名不能为空，请输入用户姓名";

                String genderTitle = isEnglish ? "Required" : "必填提示";
                String genderText = isEnglish ? "Please select Gender" : "请选择性别";
                String genderErrorTitle = isEnglish ? "Input Error" : "输入错误";
                String genderErrorText = isEnglish ? "Gender must be selected from the dropdown: Male/Female" : "性别只能从下拉列表选择「男」或「女」";
                String genderPromptTitle = isEnglish ? "Gender Selection" : "性别选择";
                String genderPromptText = isEnglish ? "Please select from dropdown: Male/Female" : "请从下拉列表中选择：男/女";

                String dateTitle = isEnglish ? "Date Format" : "日期格式提示";
                String dateText = isEnglish ? "Please enter correct date format, e.g.: 1999-01-01" : "请输入正确的日期格式，例如：1999-01-01";
                String dateErrorTitle = isEnglish ? "Format Error" : "格式错误";
                String dateErrorText = isEnglish ? "Date format must be yyyy-MM-dd and must be a valid date" : "日期格式必须为 yyyy-MM-dd，且为有效日期";

                // ==========================
                // 0. 用户编号必填
                // ==========================
                CellRangeAddressList userRange = new CellRangeAddressList(1, 100, 0, 0);
                XSSFDataValidationConstraint userConstraint = (XSSFDataValidationConstraint) helper.createCustomConstraint("TRIM(A2)<>\"\"");
                XSSFDataValidation userValidation = (XSSFDataValidation) helper.createValidation(userConstraint, userRange);
                userValidation.setShowPromptBox(true);
                userValidation.createPromptBox(userTitle, userText);
                userValidation.setShowErrorBox(true);
                userValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                userValidation.createErrorBox(userErrorTitle, userErrorText);
                xssfSheet.addValidationData(userValidation);

                // ==========================
                // 1. 用户姓名必填
                // ==========================
                CellRangeAddressList nameRange = new CellRangeAddressList(1, 100, 1, 1);
                XSSFDataValidationConstraint nameConstraint = (XSSFDataValidationConstraint) helper.createCustomConstraint("TRIM(B2)<>\"\"");
                XSSFDataValidation nameValidation = (XSSFDataValidation) helper.createValidation(nameConstraint, nameRange);
                nameValidation.setShowPromptBox(true);
                nameValidation.createPromptBox(nameTitle, nameText);
                nameValidation.setShowErrorBox(true);
                nameValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                nameValidation.createErrorBox(nameErrorTitle, nameErrorText);
                xssfSheet.addValidationData(nameValidation);

                // 2. 性别列下拉框
                CellRangeAddressList genderRange = new CellRangeAddressList(1, 100, 2, 2);
                String[] genderOptions = isEnglish ? new String[]{"Male", "Female"} : new String[]{"男", "女"};
                XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint) helper.createExplicitListConstraint(genderOptions);
                XSSFDataValidation validation = (XSSFDataValidation) helper.createValidation(constraint, genderRange);

                validation.setSuppressDropDownArrow(true);
                validation.setShowErrorBox(true);
                validation.setShowPromptBox(true);
                validation.createPromptBox(genderTitle, genderText);
                validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation.createErrorBox(genderErrorTitle, genderErrorText);
                validation.createPromptBox(genderPromptTitle, genderPromptText);
                xssfSheet.addValidationData(validation);

                // 3. 体检日期格式校验
                CellRangeAddressList dateRange = new CellRangeAddressList(1, 100, 3, 3);
                String formula = "AND(ISNUMBER(D2), D2>=1, D2<=73049)";
                XSSFDataValidationConstraint constraintData = (XSSFDataValidationConstraint) helper.createCustomConstraint(formula);
                XSSFDataValidation dateValidation = (XSSFDataValidation) helper.createValidation(constraintData, dateRange);

                dateValidation.setShowPromptBox(true);
                dateValidation.createPromptBox(dateTitle, dateText);
                dateValidation.setShowErrorBox(true);
                dateValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                dateValidation.createErrorBox(dateErrorTitle, dateErrorText);
                dateValidation.setSuppressDropDownArrow(true);
                dateValidation.setShowPromptBox(true);

                xssfSheet.addValidationData(dateValidation);

                // 4. 为动态指标列添加别名提示（批量添加，性能优化）
                String aliasTitle = isEnglish ? "Alias Information" : "指标别名信息";
                for (int i = 0; i < dynamicCheckItems.size(); i++) {
                    int colIndex = 4 + i;
                    String fieldName = dynamicCheckItems.get(i);
                    String aliasName = fieldAliasMap.get(fieldName);

                    if (aliasName != null && !aliasName.isEmpty()) {
                        CellRangeAddressList aliasRange = new CellRangeAddressList(1, 100, colIndex, colIndex);
                        XSSFDataValidationConstraint aliasConstraint = (XSSFDataValidationConstraint) helper.createCustomConstraint("TRUE");
                        XSSFDataValidation aliasValidation = (XSSFDataValidation) helper.createValidation(aliasConstraint, aliasRange);

                        aliasValidation.setShowPromptBox(true);
                        aliasValidation.createPromptBox(aliasTitle, aliasName);
                        aliasValidation.setShowErrorBox(false);

                        xssfSheet.addValidationData(aliasValidation);
                    }
                }

                // 标记已初始化，后续不再执行
                initialized = true;
            }
        };

        // 7. 列宽处理器：设置每列宽度
        CellWriteHandler columnWidthHandler = new CellWriteHandler() {
            private boolean done = false;

            @Override
            public void afterCellCreate(CellWriteHandlerContext context) {
                if (done) return;
                Sheet sheet = context.getWriteSheetHolder().getSheet();
                sheet.setColumnWidth(0, 20 * 256);
                sheet.setColumnWidth(1, 15 * 256);
                sheet.setColumnWidth(2, 10 * 256);
                sheet.setColumnWidth(3, 15 * 256);

                int totalCols = baseHeaders.size() + dynamicCheckItems.size();
                for (int i = 4; i < totalCols; i++) {
                    sheet.setColumnWidth(i, 20 * 256);
                }
                done = true;
            }
        };

        // 8. 写入Excel（空数据，只生成表头）
        try {
            EasyExcel.write(response.getOutputStream())
                    .head(finalHead)
                    .inMemory(Boolean.TRUE)
                    .registerWriteHandler(styleHandler)
                    .registerWriteHandler(validationHandler)
                    .registerWriteHandler(columnWidthHandler)
                    .sheet(isEnglish ? "Physical Examination" : "体检信息")
                    .doWrite(new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
