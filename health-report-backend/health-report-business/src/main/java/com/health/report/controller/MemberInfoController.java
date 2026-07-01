package com.health.report.controller;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.core.page.TableDataInfo;
import com.health.report.common.enums.BusinessType;
import com.health.report.domain.MemberInfo;
import com.health.report.listener.MemberInfoImportListener;
import com.health.report.service.IMemberInfoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;

/**
 * 会员信息Controller
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@RestController
@RequestMapping("/healthReport/memberInfo")
public class MemberInfoController extends BaseController {

    @Autowired
    private IMemberInfoService memberInfoService;

    /**
     * 查询会员信息列表
     */
    @GetMapping("/list")
    public TableDataInfo list(MemberInfo memberInfo) {
        startPage();
        List<MemberInfo> list = memberInfoService.page(memberInfo);
        return getDataTable(list);
    }

    /**
     * 获取会员信息详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(memberInfoService.getById(id));
    }

    /**
     * 新增会员信息
     */
    @Log(title = "个人基本信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MemberInfo memberInfo) {
        return success(memberInfoService.create(memberInfo));
    }

    /**
     * 修改会员信息
     */
    @Log(title = "个人基本信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MemberInfo memberInfo) {
        return success(memberInfoService.update(memberInfo));
    }

    /**
     * 删除会员信息
     */
    @Log(title = "个人基本信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        memberInfoService.deleteById(id);
        return success();
    }

    @PostMapping("/importTemplate")
    public void importTemplate(@RequestParam(required = false, defaultValue = "zh") String languageType,
                               HttpServletResponse response) throws IOException {
        // 判断是否为英文
        final boolean isEnglish = "en".equalsIgnoreCase(languageType);

        // 1. 定义表头（根据截图：姓名、关系、性别、出生日期、联系方式、地址、备注）
        List<String> headers = new ArrayList<>();
        if (isEnglish) {
            headers.add("Name");
            headers.add("Gender");
            headers.add("Birth Date");
            headers.add("Relationship");
            headers.add("Contact");
        } else {
            headers.add("姓名");
            headers.add("性别");
            headers.add("出生日期");
            headers.add("关系");
            headers.add("联系方式");
        }

        // 2. 转换表头格式
        List<List<String>> finalHead = new ArrayList<>();
        for (String header : headers) {
            finalHead.add(List.of(header));
        }

        // 3. 响应头设置
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 4. 样式处理器：必填项红色背景+白色加粗字体，其他灰色背景
        CellWriteHandler styleHandler = new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                // 只处理表头行（第1行，索引0）
                if (!context.getRowIndex().equals(0)) return;

                WriteCellData<?> cellData = context.getFirstCellData();
                WriteCellStyle style = cellData.getOrCreateStyle();
                Integer colIndex = context.getColumnIndex();

                // 必填项（姓名、性别、出生日期）：红色背景 + 白色字体 + 加粗
                if (colIndex == 0 || colIndex == 1|| colIndex == 2) {
                    style.setFillForegroundColor(IndexedColors.RED.getIndex());
                    style.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
                    WriteFont font = new WriteFont();
                    font.setColor(IndexedColors.WHITE.getIndex());
                    font.setBold(true);
                    font.setFontHeightInPoints((short) 12);
                    style.setWriteFont(font);
                    style.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    style.setVerticalAlignment(VerticalAlignment.CENTER);
                } else {
                    // 其他列：灰色背景 + 黑色字体 + 加粗
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

        // 5. 下拉框和日期校验处理器
        CellWriteHandler validationHandler = new CellWriteHandler() {
            @Override
            public void afterCellCreate(CellWriteHandlerContext context) {
                Sheet sheet = context.getWriteWorkbookHolder().getWorkbook().getSheetAt(0);
                XSSFSheet xssfSheet = (XSSFSheet) sheet;

                XSSFDataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);

                // 多语言提示文本
                String nameTitle = isEnglish ? "Required" : "必填提示";
                String nameText = isEnglish ? "Please enter Name (required)" : "请输入姓名（必填）";
                String nameErrorTitle = isEnglish ? "Input Error" : "输入错误";
                String nameErrorText = isEnglish ? "Name cannot be empty" : "姓名不能为空，请输入姓名";

                String relationshipOptions = isEnglish ? "Self,Family,Friend" : "本人,家人,朋友";
                String relationshipErrorTitle = isEnglish ? "Input Error" : "输入错误";
                String relationshipErrorText = isEnglish ? "Relationship must be selected from the dropdown" : "关系只能从下拉列表选择「本人」「家人」或「朋友」";
                String relationshipPromptTitle = isEnglish ? "Relationship Selection" : "关系选择";
                String relationshipPromptText = isEnglish ? "Please select from dropdown: Self/Family/Friend" : "请从下拉列表中选择：本人/家人/朋友";

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
                // 0. 姓名必填（正确版本）
                // ==========================
                CellRangeAddressList nameRange = new CellRangeAddressList(1, 100, 0, 0);
                // 姓名不能为空的约束
                XSSFDataValidationConstraint nameConstraint = (XSSFDataValidationConstraint) helper.createCustomConstraint("TRIM(A2)<>\"\"");
                XSSFDataValidation nameValidation = (XSSFDataValidation) helper.createValidation(nameConstraint, nameRange);
                // 标题 + 提示
                nameValidation.setShowPromptBox(true);
                nameValidation.createPromptBox(nameTitle, nameText);
                // 错误提示（不填就报错）
                nameValidation.setShowErrorBox(true);
                nameValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                nameValidation.createErrorBox(nameErrorTitle, nameErrorText);
                xssfSheet.addValidationData(nameValidation);

                // 1. 关系列：B列（索引2），从第2行到第1000行添加下拉
                CellRangeAddressList relationshipRange = new CellRangeAddressList(1, 100, 3, 3);
                String[] relationshipValues = isEnglish ? new String[]{"Self", "Family", "Friend"} : new String[]{"本人", "家人", "朋友"};
                XSSFDataValidationConstraint relationshipConstraint = (XSSFDataValidationConstraint) helper.createExplicitListConstraint(relationshipValues);
                XSSFDataValidation relationshipValidation = (XSSFDataValidation) helper.createValidation(relationshipConstraint, relationshipRange);
                relationshipValidation.setSuppressDropDownArrow(true);
                relationshipValidation.setShowErrorBox(true);
                relationshipValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                relationshipValidation.createErrorBox(relationshipErrorTitle, relationshipErrorText);
                relationshipValidation.createPromptBox(relationshipPromptTitle, relationshipPromptText);
                xssfSheet.addValidationData(relationshipValidation);

                // 2. 性别列：C列（索引1），从第2行到第1000行添加下拉
                CellRangeAddressList genderRange = new CellRangeAddressList(1, 100, 1, 1);
                String[] genderValues = isEnglish ? new String[]{"Male", "Female"} : new String[]{"男", "女"};
                XSSFDataValidationConstraint genderConstraint = (XSSFDataValidationConstraint) helper.createExplicitListConstraint(genderValues);
                XSSFDataValidation genderValidation = (XSSFDataValidation) helper.createValidation(genderConstraint, genderRange);
                genderValidation.setSuppressDropDownArrow(true);
                genderValidation.setShowErrorBox(true);

                // 启用提示框并设置内容
                genderValidation.setShowPromptBox(true);
                genderValidation.createPromptBox(genderTitle, genderText);
                genderValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                genderValidation.createErrorBox(genderErrorTitle, genderErrorText);
                genderValidation.createPromptBox(genderPromptTitle, genderPromptText);
                xssfSheet.addValidationData(genderValidation);

                // 3. 出生日期格式校验：D列（索引3）
                CellRangeAddressList dateRange = new CellRangeAddressList(1, 100, 2, 2);
                String formula = "AND(ISNUMBER(C2), C2>=1, C2<=73049)";

                XSSFDataValidationConstraint constraintData = (XSSFDataValidationConstraint) helper.createCustomConstraint(formula);

                XSSFDataValidation dateValidation = (XSSFDataValidation) helper.createValidation(constraintData, dateRange);

                // 启用提示框并设置内容
                dateValidation.setShowPromptBox(true);
                dateValidation.createPromptBox(dateTitle, dateText);
                // 3. 设置提示
                dateValidation.setShowErrorBox(true);
                dateValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                dateValidation.createErrorBox(dateErrorTitle, dateErrorText);

                // 4. 解决WPS/Excel下拉箭头不显示的问题（可选）
                dateValidation.setSuppressDropDownArrow(true);
                dateValidation.setShowPromptBox(true);

                xssfSheet.addValidationData(dateValidation);
            }
        };

        // 6. 列宽处理器
        CellWriteHandler columnWidthHandler = new CellWriteHandler() {
            private boolean done = false;

            @Override
            public void afterCellCreate(CellWriteHandlerContext context) {
                if (done) return;
                Sheet sheet = context.getWriteSheetHolder().getSheet();
                // 设置每列宽度
                sheet.setColumnWidth(0, 15 * 256); // 姓名
                sheet.setColumnWidth(1, 15 * 256); // 性别
                sheet.setColumnWidth(2, 15 * 256); // 关系
                sheet.setColumnWidth(3, 20 * 256); // 出生日期
                sheet.setColumnWidth(4, 20 * 256); // 联系方式
                done = true;
            }
        };

        // 7. 写入Excel（空数据，只生成表头）
        EasyExcel.write(response.getOutputStream())
                .head(finalHead)
                .inMemory(Boolean.TRUE)
                .registerWriteHandler(styleHandler)
                .registerWriteHandler(validationHandler)
                .registerWriteHandler(columnWidthHandler)
                .sheet(isEnglish ? "User Info" : "用户信息")
                .doWrite(new ArrayList<>());
    }


    @Log(title = "个人基本信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "zh") String lang) throws Exception
    {
        // 使用 EasyExcel 读取，支持中英文表头
        List<MemberInfo> memberList = new ArrayList<>();

        EasyExcel.read(file.getInputStream(), new MemberInfoImportListener(memberList))
                .sheet()
                .doRead();

        String message = memberInfoService.importMember(memberList, false,lang);
        return success(message);
    }

    @Log(title = "个人基本信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemberInfo memberInfo,
                       @RequestParam(required = false, defaultValue = "zh") String languageType)
    {
        List<MemberInfo> list = memberInfoService.page(memberInfo);

        boolean isEnglish = "en".equalsIgnoreCase(languageType);
        String sheetName = isEnglish ? "User Info" : "用户信息";

        // 使用 EasyExcel 导出，支持动态表头
        List<List<String>> headers = new ArrayList<>();
        if (isEnglish) {
            headers.add(List.of("Member No"));
            headers.add(List.of("Name"));
            headers.add(List.of("Relationship"));
            headers.add(List.of("Gender"));
            headers.add(List.of("Birth Date"));
            headers.add(List.of("Contact"));
            headers.add(List.of("Country"));
            headers.add(List.of("City"));
        } else {
            headers.add(List.of("用户编号"));
            headers.add(List.of("姓名"));
            headers.add(List.of("关系"));
            headers.add(List.of("性别"));
            headers.add(List.of("出生日期"));
            headers.add(List.of("联系方式"));
            headers.add(List.of("国家"));
            headers.add(List.of("城市"));
        }

        // 准备数据
        List<List<Object>> data = new ArrayList<>();
        for (MemberInfo member : list) {
            List<Object> row = new ArrayList<>();
            row.add(member.getMemberNo());
            row.add(member.getName());
            row.add(convertRelationship(member.getRelationship(), isEnglish));
            row.add(convertGender(member.getGender(), isEnglish));
            // 格式化日期为 yyyy-MM-dd
            row.add(formatBirthDate(member.getBirthDate()));
            row.add(member.getContact());
            row.add(convertCountry(member.getCountry(), isEnglish));
            row.add(member.getCity());
            data.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = isEnglish
                    ? "User_Info_" + System.currentTimeMillis() + ".xlsx"
                    : "用户信息_" + System.currentTimeMillis() + ".xlsx";
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
     * 转换关系显示
     */
    private String convertRelationship(Integer relationship, boolean isEnglish) {
        if (relationship == null) return "";
        if (isEnglish) {
            return switch (relationship) {
                case 1 -> "Self";
                case 2 -> "Family";
                case 3 -> "Friend";
                default -> "";
            };
        } else {
            return switch (relationship) {
                case 1 -> "本人";
                case 2 -> "家人";
                case 3 -> "朋友";
                default -> "";
            };
        }
    }

    /**
     * 转换性别显示
     */
    private String convertGender(Integer gender, boolean isEnglish) {
        if (gender == null) return "";
        if (isEnglish) {
            return gender == 1 ? "Male" : "Female";
        } else {
            return gender == 1 ? "男" : "女";
        }
    }

    /**
     * 格式化出生日期
     */
    private String formatBirthDate(java.util.Date birthDate) {
        if (birthDate == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(birthDate);
    }

    /**
     * 转换国家代码为显示名称
     */
    private String convertCountry(String countryCode, boolean isEnglish) {
        if (countryCode == null || countryCode.isEmpty()) return "";
        return switch (countryCode) {
            case "CN" -> isEnglish ? "China" : "中国";
            case "US" -> isEnglish ? "United States" : "美国";
            case "JP" -> isEnglish ? "Japan" : "日本";
            case "KR" -> isEnglish ? "South Korea" : "韩国";
            case "GB" -> isEnglish ? "United Kingdom" : "英国";
            case "DE" -> isEnglish ? "Germany" : "德国";
            case "FR" -> isEnglish ? "France" : "法国";
            case "AU" -> isEnglish ? "Australia" : "澳大利亚";
            case "CA" -> isEnglish ? "Canada" : "加拿大";
            case "SG" -> isEnglish ? "Singapore" : "新加坡";
            default -> countryCode;
        };
    }
}
