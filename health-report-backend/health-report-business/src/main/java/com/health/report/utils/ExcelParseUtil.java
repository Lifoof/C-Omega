package com.health.report.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.health.report.dto.collection.excel.CollectionRecordExcelDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel解析工具类（基于EasyExcel，支持按行解析采集数据）
 */
@Slf4j
@Component
public class ExcelParseUtil {

    /**
     * 按行解析Excel采集数据（一行对应一条采集记录）
     * @param file 上传的Excel文件（仅支持.xlsx格式）
     * @param sheetName 要解析的sheet名称（需与Excel中完全一致）
     * @return 单行采集记录DTO列表（每行对应一个CollectionRecordExcelDTO）
     * @throws IOException 文件读取异常
     */
    public List<CollectionRecordExcelDTO> parseRowData(MultipartFile file, String sheetName) throws IOException {
        // 校验文件合法性
        validateExcelFile(file);

        // 初始化结果列表
        List<CollectionRecordExcelDTO> rowDataList = new ArrayList<>();

        // 获取文件输入流
        try (InputStream inputStream = file.getInputStream()) {
            // EasyExcel 读取 Excel：按行解析（监听器模式）
            EasyExcel.read(inputStream, new AnalysisEventListener<Map<Integer, Object>>() {
                        // 存储 Excel 表头（第一行数据，key=列索引，value=表头名称）
                        private List<String> headerList;
                        // 记录当前解析的行号（用于日志定位）
                        private int currentRowNum = 0;

                        /**
                         * 读取表头（第一行）
                         * @param headMap 表头映射（列索引→表头名称）
                         * @param context 解析上下文
                         */
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                            currentRowNum++; // 表头占 1 行
                            // 转换表头为有序列表（按列索引排序）
                            headerList = new ArrayList<>(headMap.size());
                            for (int i = 0; i < headMap.size(); i++) {
                                String headerName = headMap.get(i);
                                // 表头名称去空格（避免 Excel 中表头前后空格导致匹配失败）
                                headerList.add(StringUtils.hasText(headerName) ? headerName.trim() : "未知列_" + i);
                            }
                            log.info("解析 Excel 表头完成，共{}列：{}", headerList.size(), headerList);
                        }

                        /**
                         * 读取每行数据（从第二行开始，每行对应一条采集记录）
                         * @param rowData 行数据（列索引→单元格值）
                         * @param context 解析上下文
                         */
                        @Override
                        public void invoke(Map<Integer, Object> rowData, AnalysisContext context) {
                            currentRowNum++;
                            log.debug("正在解析 Excel 第{}行数据：{}", currentRowNum, rowData);

                            // 1. 初始化单行 DTO 和字段值映射
                            CollectionRecordExcelDTO rowDTO = new CollectionRecordExcelDTO();
                            Map<String, String> fieldValues = new HashMap<>();

                            // 2. 遍历表头，匹配每行单元格数据
                            for (int colIndex = 0; colIndex < headerList.size(); colIndex++) {
                                String headerName = headerList.get(colIndex); // 表头名称（如"会员编号"）
                                Object cellValue = rowData.get(colIndex);     // 单元格值（可能为 null）
                                // 单元格值转换为字符串（去空格，null 转为空字符串）
                                String cellValueStr = cellValue != null ? cellValue.toString().trim() : "";

                                // 3. 匹配核心字段（会员信息、体检基础信息）
                                matchCoreField(headerName, cellValueStr, rowDTO);

                                // 4. 非核心字段归为"采集字段值"（后续映射为明细表数据）
                                if (!isCoreHeader(headerName)) {
                                    if (StringUtils.hasText(cellValueStr)) {
                                        fieldValues.put(headerName, cellValueStr);
                                    }
                                }
                            }

                            // 5. 设置采集字段值并添加到结果列表
                            rowDTO.setFieldValues(fieldValues);
                            rowDTO.setRowNum(currentRowNum);
                            rowDataList.add(rowDTO);
                            log.debug("第{}行解析完成：{}", currentRowNum, rowDTO);
                        }

                        /**
                         * 所有行解析完成后回调
                         * @param context 解析上下文
                         */
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            log.info("Excel解析完成，共解析{}行数据（含1行表头，实际数据{}行）",
                                    currentRowNum, rowDataList.size());
                        }

                        /**
                         * 匹配核心字段（会员编号、姓名、性别等）
                         * @param headerName 表头名称
                         * @param cellValueStr 单元格值（字符串）
                         * @param rowDTO 单行DTO（用于存储核心字段值）
                         */
                        private void matchCoreField(String headerName, String cellValueStr, CollectionRecordExcelDTO rowDTO) {
                            switch (headerName) {
                                case "用户编号":
                                case "User NO":
                                    rowDTO.setMemberNo(cellValueStr);
                                    break;
                                case "用户姓名":
                                case "Name":
                                    rowDTO.setMemberName(cellValueStr);
                                    break;
                                case "性别":
                                case "Gender":
                                    // 性别转换：中文“男”→1，“女”→2；英文"Male"→1，"Female"→2；其他→0（未知）
                                    if ("男".equals(cellValueStr) || "Male".equalsIgnoreCase(cellValueStr)) {
                                        rowDTO.setMemberGender(1);
                                    } else if ("女".equals(cellValueStr) || "Female".equalsIgnoreCase(cellValueStr)) {
                                        rowDTO.setMemberGender(2);
                                    } else if (StringUtils.hasText(cellValueStr)) {
                                        log.warn("性别字段值异常：{}，已设为未知（0）", cellValueStr);
                                        rowDTO.setMemberGender(0);
                                    }
                                    break;
                                case "体检日期":
                                case "Checkup Date":
                                    // 日期转换：支持多种格式（YYYY-MM-DD、YYYY/M/D、YYYY/M/DD、YYYY/MM/D等）
                                    rowDTO.setCheckupDate(parseDate(cellValueStr));
                                    break;
                                // 其他核心字段可在此处扩展（如“年龄”“联系方式”等）
                                default:
                                    // 非核心字段，不处理（后续归为采集字段值）
                                    break;
                            }
                        }

                        /**
                         * 判断是否为核心表头（核心字段需单独处理，非核心字段归为采集字段）
                         * @param headerName 表头名称
                         * @return true=核心表头，false=非核心表头
                         */
                        private boolean isCoreHeader(String headerName) {
                            List<String> coreHeaders = List.of("用户编号", "用户姓名", "性别", "体检日期",
                                    "User NO", "Name", "Gender", "Checkup Date");
                            return coreHeaders.contains(headerName);
                        }
                    })
                    // 指定要解析的sheet（按名称匹配，避免sheet索引变动导致解析错误）
                    .sheet(sheetName)
                    // 禁止自动关闭输入流（try-with-resources会自动关闭）
                    .autoTrim(false)
                    // 开始解析
                    .doRead();
        } catch (IOException e) {
            log.error("Excel文件读取异常：{}", e.getMessage(), e);
            throw new IOException("Excel解析失败：" + e.getMessage(), e);
        }

        // 过滤空行（仅保留核心字段或采集字段有值的行）
        return filterEmptyRows(rowDataList);
    }

    /**
     * 校验Excel文件合法性（格式、大小）
     * @param file 上传的MultipartFile
     * @throws IOException 校验失败异常
     */
    private void validateExcelFile(MultipartFile file) throws IOException {
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new IOException("上传的Excel文件为空，无法解析");
        }

        // 2. 校验文件格式（仅支持.xlsx，不支持.xls）
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw new IOException("文件格式错误：仅支持.xlsx格式（当前文件：" + fileName + "）");
        }

        // 3. 校验文件大小（限制10MB，可根据业务调整）
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IOException("文件过大：当前文件" + (file.getSize() / 1024 / 1024) + "MB，最大支持10MB");
        }

        log.info("Excel文件校验通过：文件名={}，大小={}KB", fileName, file.getSize() / 1024);
    }

    /**
     * 过滤空行（核心字段和采集字段均为空的行视为空行，不返回）
     * @param rowDataList 原始解析结果
     * @return 过滤后的非空行列表
     */
    private List<CollectionRecordExcelDTO> filterEmptyRows(List<CollectionRecordExcelDTO> rowDataList) {
        if (CollectionUtils.isEmpty(rowDataList)) {
            return rowDataList;
        }

        List<CollectionRecordExcelDTO> nonEmptyRows = new ArrayList<>();
        for (CollectionRecordExcelDTO rowDTO : rowDataList) {
            // 核心字段非空 或 采集字段非空 → 视为非空行
            boolean hasCoreData = StringUtils.hasText(rowDTO.getMemberNo())
                    || StringUtils.hasText(rowDTO.getMemberName())
                    || rowDTO.getCheckupDate() != null;
            boolean hasFieldData = !CollectionUtils.isEmpty(rowDTO.getFieldValues());

            if (hasCoreData || hasFieldData) {
                nonEmptyRows.add(rowDTO);
            } else {
                log.debug("过滤空行：核心字段和采集字段均为空，已跳过");
            }
        }

        log.info("Excel空行过滤完成：原始{}行，过滤后{}行", rowDataList.size(), nonEmptyRows.size());
        return nonEmptyRows;
    }

    /**
     * （可选）按行解析Excel的重载方法（默认解析第一个sheet）
     * @param file 上传的Excel文件
     * @return 单行采集记录DTO列表
     * @throws IOException 文件读取异常
     */
    public List<CollectionRecordExcelDTO> parseRowData(MultipartFile file) throws IOException {
        // 默认解析第一个sheet（sheet索引从0开始）
        return parseRowData(file, 0);
    }

    /**
     * （可选）按行解析Excel的重载方法（按sheet索引解析）
     * @param file 上传的Excel文件
     * @param sheetIndex sheet索引（0表示第一个sheet）
     * @return 单行采集记录DTO列表
     * @throws IOException 文件读取异常
     */
    public List<CollectionRecordExcelDTO> parseRowData(MultipartFile file, int sheetIndex) throws IOException {
        // 校验文件合法性
        validateExcelFile(file);

        List<CollectionRecordExcelDTO> rowDataList = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, Map.class, new AnalysisEventListener<Map>() {
                        private List<String> headerList;
                        private int currentRowNum = 0;

                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                            currentRowNum++;
                            headerList = new ArrayList<>(headMap.size());
                            for (int i = 0; i < headMap.size(); i++) {
                                String headerName = headMap.get(i);
                                headerList.add(StringUtils.hasText(headerName) ? headerName.trim() : "未知列_" + i);
                            }
                            log.info("解析Excel第{}个sheet表头完成：{}", sheetIndex + 1, headerList);
                        }

                        @Override
                        public void invoke(Map rowData, AnalysisContext context) {
                            currentRowNum++;
                            CollectionRecordExcelDTO rowDTO = new CollectionRecordExcelDTO();
                            Map<String, String> fieldValues = new HashMap<>();

                            for (int colIndex = 0; colIndex < headerList.size(); colIndex++) {
                                String headerName = headerList.get(colIndex);
                                Object cellValue = rowData.get(colIndex);
                                String cellValueStr = cellValue != null ? cellValue.toString().trim() : "";

                                matchCoreField(headerName, cellValueStr, rowDTO);
                                if (!isCoreHeader(headerName) && StringUtils.hasText(cellValueStr)) {
                                    fieldValues.put(headerName, cellValueStr);
                                }
                            }

                            rowDTO.setFieldValues(fieldValues);
                            rowDataList.add(rowDTO);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            log.info("Excel第{}个sheet解析完成，共{}行数据", sheetIndex + 1, rowDataList.size());
                        }

                        private void matchCoreField(String headerName, String cellValueStr, CollectionRecordExcelDTO rowDTO) {
                            // 同上面的matchCoreField方法实现（可抽取为公共方法，避免重复代码）
                            switch (headerName) {
                                case "用户编号":
                                case "User NO":
                                    rowDTO.setMemberNo(cellValueStr);
                                    break;
                                case "用户姓名":
                                case "Name":
                                    rowDTO.setMemberName(cellValueStr);
                                    break;
                                case "性别":
                                case "Gender":
                                    if ("男".equals(cellValueStr) || "Male".equalsIgnoreCase(cellValueStr)) rowDTO.setMemberGender(1);
                                    else if ("女".equals(cellValueStr) || "Female".equalsIgnoreCase(cellValueStr)) rowDTO.setMemberGender(2);
                                    else rowDTO.setMemberGender(0);
                                    break;
                                case "体检日期":
                                case "Checkup Date":
                                    rowDTO.setCheckupDate(parseDate(cellValueStr));
                                    break;
                                default:
                                    break;
                            }
                        }

                        private boolean isCoreHeader(String headerName) {
                            return List.of("用户编号", "用户姓名", "性别", "体检日期",
                                    "User NO", "Name", "Gender", "Checkup Date").contains(headerName);
                        }
                    })
                    .sheet(sheetIndex)
                    .autoTrim(false)
                    .doRead();
        } catch (IOException e) {
            log.error("Excel按索引解析异常：{}", e.getMessage(), e);
            throw new IOException("Excel解析失败：" + e.getMessage(), e);
        }

        return filterEmptyRows(rowDataList);
    }

    /**
     * 解析日期字符串，支持多种格式
     * @param dateStr 日期字符串
     * @return LocalDate 对象，解析失败返回 null
     */
    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        String trimmed = dateStr.trim();
        // 支持的日期格式
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),      // 1987-09-28
                DateTimeFormatter.ofPattern("yyyy/M/d"),        // 1987/9/28
                DateTimeFormatter.ofPattern("yyyy/M/dd"),       // 1987/9/28
                DateTimeFormatter.ofPattern("yyyy/MM/d"),       // 1987/09/8
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),      // 1987/09/28
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),      // 1987.09.28
                DateTimeFormatter.ofPattern("yyyy.MM.d"),       // 1987.09.8
                DateTimeFormatter.ofPattern("yyyy.M.dd"),       // 1987.9.28
                DateTimeFormatter.ofPattern("yyyy.M.d")         // 1987.9.8
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        log.warn("日期格式无法解析：{}，已设为null", dateStr);
        return null;
    }
}