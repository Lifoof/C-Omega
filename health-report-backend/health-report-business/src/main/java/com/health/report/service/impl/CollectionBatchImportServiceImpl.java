package com.health.report.service.impl;

import com.health.report.domain.CollectionDataItem;
import com.health.report.domain.CollectionFieldConfig;
import com.health.report.domain.CollectionRecord;
import com.health.report.domain.MemberInfo;
import com.health.report.dto.collection.excel.BatchImportRequest;
import com.health.report.dto.collection.excel.BatchImportResponse;
import com.health.report.dto.collection.excel.CollectionRecordExcelDTO;
import com.health.report.mapper.CollectionDataItemMapper;
import com.health.report.mapper.CollectionFieldConfigMapper;
import com.health.report.mapper.CollectionRecordMapper;
import com.health.report.mapper.MemberInfoMapper;
import com.health.report.service.CollectionBatchImportService;
import com.health.report.utils.ExcelParseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 采集信息批量导入Service实现（按行导入：一行对应一条采集记录，多行即批量）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionBatchImportServiceImpl implements CollectionBatchImportService {
    @Autowired
    private ExcelParseUtil excelParseUtil;
    @Autowired
    private CollectionRecordMapper collectionRecordMapper;
    @Autowired
    private CollectionDataItemMapper collectionDataItemMapper;
    @Autowired
    private MemberInfoMapper memberInfoMapper;
    @Autowired
    private CollectionFieldConfigMapper collectionFieldConfigMapper;

    // ======================== 固定配置常量 ========================
    /** 数据来源：Excel导入 */
    private static final Integer SOURCE_TYPE_EXCEL = 2;
    /** 初始采集状态：草稿（未完成） */
    private static final Integer INIT_COLLECTION_STATUS = 0;
    /** 初始报告状态：未生成 */
    private static final Integer INIT_REPORT_STATUS = 0;
    /** Excel主数据sheet名称（需与模板一致，不可随意修改） */
    private static final String MAIN_SHEET_NAME = "体检信息";
    /** Excel主数据sheet名称（英文） */
    private static final String MAIN_SHEET_NAME_EN = "Physical Examination";

    // ======================== 错误提示消息 ========================
    private static final Map<String, Map<String, String>> MESSAGES = new HashMap<>();
    static {
        // 中文提示
        Map<String, String> zh = new HashMap<>();
        zh.put("NO_FILES", "未选择任何Excel文件，导入终止");
        zh.put("EMPTY_FILE", "文件无有效数据（无符合格式的体检信息行）");
        zh.put("FILE_PROCESS_ERROR", "文件处理失败：");
        zh.put("MEMBER_NO_EMPTY", "【用户编号】不能为空");
        zh.put("MEMBER_NAME_EMPTY", "【用户姓名】不能为空");
        zh.put("GENDER_EMPTY", "【性别】不能为空");
        zh.put("CHECKUP_DATE_EMPTY", "【体检日期】不能为空");
        zh.put("CHECKUP_DATE_FORMAT", "【体检日期】格式错误");
        zh.put("MEMBER_NOT_EXIST", "用户编号[{0}]不存在");
        zh.put("MEMBER_DUPLICATE", "用户编号[{0}]重复");
        zh.put("ROW_SAVE_FAILED", "第{0}行保存失败，全部回滚");
        zh.put("ROW_PREFIX", "第{0}行：");
        MESSAGES.put("zh", zh);

        // 英文提示
        Map<String, String> en = new HashMap<>();
        en.put("NO_FILES", "No Excel files selected, import terminated");
        en.put("EMPTY_FILE", "File contains no valid data (no physical Examination rows)");
        en.put("FILE_PROCESS_ERROR", "File processing failed: ");
        en.put("MEMBER_NO_EMPTY", "[User ID] cannot be empty");
        en.put("MEMBER_NAME_EMPTY", "[User Name] cannot be empty");
        en.put("GENDER_EMPTY", "[Gender] cannot be empty");
        en.put("CHECKUP_DATE_EMPTY", "[Checkup Date] cannot be empty");
        en.put("CHECKUP_DATE_FORMAT", "[Checkup Date] format error");
        en.put("MEMBER_NOT_EXIST", "User ID [{0}] does not exist");
        en.put("MEMBER_DUPLICATE", "User ID [{0}] is duplicated");
        en.put("ROW_SAVE_FAILED", "Row {0} save failed, rolling back all");
        en.put("ROW_PREFIX", "Row {0}:");
        MESSAGES.put("en", en);
    }

    // ======================== 获取提示消息 ========================
    private String getMessage(String lang, String key, Object... args) {
        Map<String, String> messages = MESSAGES.getOrDefault(lang, MESSAGES.get("zh"));
        String msg = messages.getOrDefault(key, key);
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return msg;
    }

    // ======================== 核心批量导入方法 ========================
    /**
     * 多文件批量导入（每个文件内按行处理，一行一条记录）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResponse batchImport(BatchImportRequest request) {
        String lang = request.getLanguageType() != null ? request.getLanguageType() : "zh";
        BatchImportResponse importResponse = new BatchImportResponse();
        List<BatchImportResponse.ImportFailDetail> failDetails = new ArrayList<>();
        List<Long> successCollectionIds = new ArrayList<>();

        if (CollectionUtils.isEmpty(request.getFiles())) {
            BatchImportResponse.ImportFailDetail failDetail = new BatchImportResponse.ImportFailDetail();
            failDetail.setReason(getMessage(lang, "NO_FILES"));
            failDetails.add(failDetail);
            importResponse.setFailCount(failDetails.size());
            importResponse.setFailReasons(failDetails);
            return importResponse;
        }

        for (MultipartFile file : request.getFiles()) {
            String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown.xlsx");
            log.info("开始处理文件：{}，文件大小：{}KB", fileName, file.getSize() / 1024);

            try {
                // 根据语言选择sheet名称
                String sheetName = "en".equals(lang) ? MAIN_SHEET_NAME_EN : MAIN_SHEET_NAME;
                List<CollectionRecordExcelDTO> rowDataList = excelParseUtil.parseRowData(file, sheetName);
                if (CollectionUtils.isEmpty(rowDataList)) {
                    BatchImportResponse.ImportFailDetail failDetail = new BatchImportResponse.ImportFailDetail();
                    failDetail.setFileName(fileName);
                    failDetail.setReason(getMessage(lang, "EMPTY_FILE"));
                    failDetails.add(failDetail);
                    log.warn("文件{}处理完成：无有效数据", fileName);
                    continue;
                }

                // 第一步：全量预校验所有行
                List<BatchImportResponse.ImportFailDetail> tempFailList = new ArrayList<>();
                preValidateAllRows(rowDataList, fileName, tempFailList, lang);

                // 关键：只要有错误 → 直接不保存，全部回滚
                if (!CollectionUtils.isEmpty(tempFailList)) {
                    failDetails.addAll(tempFailList);
                    continue;
                }

                // 全部通过 → 才开始保存
                processEachRow(rowDataList, fileName, request.getOverrideIfExist(), failDetails, successCollectionIds, lang);

            } catch (Exception e) {
                BatchImportResponse.ImportFailDetail failDetail = new BatchImportResponse.ImportFailDetail();
                failDetail.setFileName(fileName);
                failDetail.setReason(getMessage(lang, "FILE_PROCESS_ERROR") + e.getMessage());
                failDetails.add(failDetail);
                log.error("文件{}处理异常", fileName, e);
            }
        }

        importResponse.setSuccessCount(successCollectionIds.size());
        importResponse.setFailCount(failDetails.size());
        importResponse.setFailReasons(failDetails);
        importResponse.setCollectionIds(successCollectionIds);
        log.info("所有文件批量导入完成：总成功{}条，总失败{}条", successCollectionIds.size(), failDetails.size());
        return importResponse;
    }

    // ==============================================
    // 全量预校验（所有行必须通过，否则整体失败）
    // ==============================================
    private void preValidateAllRows(List<CollectionRecordExcelDTO> rowDataList,
                                    String fileName,
                                    List<BatchImportResponse.ImportFailDetail> failDetails,
                                    String lang) {
        for (int i = 0; i < rowDataList.size(); i++) {
            int rowNum = i + 2;
            CollectionRecordExcelDTO dto = rowDataList.get(i);
            BatchImportResponse.ImportFailDetail detail = new BatchImportResponse.ImportFailDetail();
            detail.setFileName(fileName);
            detail.setRowNum(rowNum);

            try {
                // 必填校验
                validateRowData(dto, rowNum, detail, lang);

                // 用户必须存在
                MemberInfo member = getMemberIdByMemberNo(dto.getMemberNo().trim(), detail, failDetails, lang);
                if (member == null) {
                    throw new IllegalArgumentException("User not exist");
                }

            } catch (Exception e) {
                detail.setReason(e.getMessage());
                failDetails.add(detail);
            }
        }
    }

    // ======================== 单行处理 ========================
    private void processEachRow(List<CollectionRecordExcelDTO> rowDataList,
                                String fileName,
                                boolean overrideIfExist,
                                List<BatchImportResponse.ImportFailDetail> failDetails,
                                List<Long> successCollectionIds,
                                String lang) {
        for (int rowIndex = 0; rowIndex < rowDataList.size(); rowIndex++) {
            int actualRowNum = rowIndex + 2;
            CollectionRecordExcelDTO rowDTO = rowDataList.get(rowIndex);

            try {
                MemberInfo memberInfo = getMemberIdByMemberNo(rowDTO.getMemberNo().trim(), null, failDetails, lang);
                Long newCollectionId = insertCollectionData(rowDTO, memberInfo, actualRowNum, fileName, lang);
                successCollectionIds.add(newCollectionId);

            } catch (Exception e) {
                log.error("保存失败：", e);
                throw new RuntimeException(getMessage(lang, "ROW_SAVE_FAILED", actualRowNum));
            }
        }
    }

    // ======================== 4项必填校验 ========================
    private void validateRowData(CollectionRecordExcelDTO rowDTO, int rowNum, BatchImportResponse.ImportFailDetail failDetail, String lang) {
        // 【核心】获取国际化的 "第X行"
        String rowPrefix = getMessage(lang, "ROW_PREFIX", rowNum);
        // 1. 用户编号 必填
        if (!StringUtils.hasText(rowDTO.getMemberNo())) {
            throw new IllegalArgumentException(rowPrefix + getMessage(lang, "MEMBER_NO_EMPTY"));
        }
        // 2. 用户姓名 必填
        if (!StringUtils.hasText(rowDTO.getMemberName())) {
            throw new IllegalArgumentException(rowPrefix + getMessage(lang, "MEMBER_NAME_EMPTY"));
        }
        // 3. 性别 必填
        if (rowDTO.getMemberGender() == null || rowDTO.getMemberGender() < 1 || rowDTO.getMemberGender() > 2) {
            throw new IllegalArgumentException(rowPrefix + getMessage(lang, "GENDER_EMPTY"));
        }
        // 4. 体检日期 必填
        if (rowDTO.getCheckupDate() == null) {
            throw new IllegalArgumentException(rowPrefix + getMessage(lang, "CHECKUP_DATE_EMPTY"));
        }
        try {
            LocalDate.parse(rowDTO.getCheckupDate().toString());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(rowPrefix + getMessage(lang, "CHECKUP_DATE_FORMAT"));
        }
    }

    // ======================== 用户查询 ========================
    private MemberInfo getMemberIdByMemberNo(String memberNo,
                                             BatchImportResponse.ImportFailDetail failDetail,
                                             List<BatchImportResponse.ImportFailDetail> failDetails,
                                             String lang) {
        MemberInfo queryMember = new MemberInfo();
        queryMember.setMemberNo(memberNo);
        queryMember.setDeleted(0);

        List<MemberInfo> memberList = memberInfoMapper.selectByMemberNo(queryMember);

        if (CollectionUtils.isEmpty(memberList)) {
            if(failDetail!=null) failDetail.setReason(getMessage(lang, "MEMBER_NOT_EXIST", memberNo));
            return null;
        }
        if (memberList.size() > 1) {
            if(failDetail!=null) failDetail.setReason(getMessage(lang, "MEMBER_DUPLICATE", memberNo));
            return null;
        }
        return memberList.get(0);
    }

    // ======================== 插入数据 ========================
    @Transactional(rollbackFor = Exception.class)
    protected Long insertCollectionData(CollectionRecordExcelDTO rowDTO,
                                        MemberInfo memberInfo,
                                        int rowNum,
                                        String fileName,
                                        String lang) {
        CollectionRecord collectionRecord = buildCollectionRecord(rowDTO, memberInfo);
        collectionRecordMapper.insert(collectionRecord);
        Long newCollectionId = collectionRecord.getId();

        List<CollectionDataItem> dataItemList = buildCollectionDataItemList(rowDTO, newCollectionId, lang);
        if (!CollectionUtils.isEmpty(dataItemList)) {
            collectionDataItemMapper.insertBatch(dataItemList);
        }

        updateCollectionCompleteness(newCollectionId);
        return newCollectionId;
    }

    // ======================== 构建主表 ========================
    private CollectionRecord buildCollectionRecord(CollectionRecordExcelDTO rowDTO, MemberInfo memberInfo) {
        CollectionRecord record = new CollectionRecord();
        record.setMemberId(memberInfo.getId());
        record.setUserId(memberInfo.getUserId());
        record.setMemberName(rowDTO.getMemberName().trim());
        record.setMemberGender(rowDTO.getMemberGender());
        record.setCheckupDate(rowDTO.getCheckupDate());
        record.setSourceType(SOURCE_TYPE_EXCEL);
        record.setReportStatus(INIT_COLLECTION_STATUS);
        record.setCompleteness(BigDecimal.ZERO);
        record.setCollectionNo("COL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        record.setDeleted(0);
        return record;
    }

    // ======================== 构建明细表 ========================
    private List<CollectionDataItem> buildCollectionDataItemList(CollectionRecordExcelDTO rowDTO, Long collectionId, String lang) {
        List<CollectionDataItem> itemList = new ArrayList<>();
        if (CollectionUtils.isEmpty(rowDTO.getFieldValues())) return itemList;
        CollectionFieldConfig queryFieldConfig=new CollectionFieldConfig();
        // 一次性查出所有启用指标，避免循环查库
        List<CollectionFieldConfig> allFields = collectionFieldConfigMapper.selectCollectionFieldConfigList(queryFieldConfig);

        for (Map.Entry<String, String> fieldEntry : rowDTO.getFieldValues().entrySet()) {
            String excelColumnName = fieldEntry.getKey().trim();
            String fieldValue = fieldEntry.getValue();

            if (!StringUtils.hasText(fieldValue)) continue;

            // 第一步：按【指标名称】匹配（中文或英文）
            CollectionFieldConfig fieldConfig = allFields.stream()
                    .filter(f -> excelColumnName.equals(f.getFieldName()) || excelColumnName.equals(f.getFieldNameEn()))
                    .findFirst().orElse(null);

            // 第二步：名称匹配不到 → 按【别名】匹配（根据语言选择中文或英文别名）
            if (fieldConfig == null) {
                final boolean isEnglish = "en".equals(lang);
                fieldConfig = allFields.stream()
                        .filter(f -> {
                            // 根据语言选择对应的别名字段
                            String alias = isEnglish ? f.getAliasNameEn() : f.getAliasName();
                            if (!StringUtils.hasText(alias)) {
                                // 如果英文别名为空，回退到中文别名
                                alias = f.getAliasName();
                            }
                            if (!StringUtils.hasText(alias)) return false;
                            // 按顿号切分别名
                            String[] aliases = alias.split("、");
                            for (String a : aliases) {
                                if (excelColumnName.equals(a.trim())) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .findFirst().orElse(null);
            }

            // 都匹配不到则跳过
            if (fieldConfig == null) {
                continue;
            }

            // 保存数据项
            CollectionDataItem dataItem = new CollectionDataItem();
            dataItem.setCollectionId(collectionId);
            dataItem.setCategoryCode(fieldConfig.getCategoryCode());
            dataItem.setFieldCode(fieldConfig.getFieldCode());
            dataItem.setFieldName(fieldConfig.getFieldName());
            // 根据语言保存对应的单位
            if ("en".equals(lang) && StringUtils.hasText(fieldConfig.getUnitEn())) {
                dataItem.setUnit(fieldConfig.getUnitEn());
            } else {
                dataItem.setUnit(fieldConfig.getUnit());
            }

            String fieldType = fieldConfig.getFieldType();
            if ("NUMBER".equalsIgnoreCase(fieldType)) {
                try {
                    dataItem.setNumberValue(new BigDecimal(fieldValue.trim()));
                } catch (NumberFormatException e) {
                    dataItem.setStringValue(fieldValue.trim());
                }
            } else {
                dataItem.setStringValue(fieldValue.trim());
            }

            dataItem.setCreateTime(new Date());
            dataItem.setUpdateTime(new Date());
            dataItem.setDeleted(0);
            itemList.add(dataItem);
        }
        return itemList;
    }

    // ======================== 更新完整性 ========================
    private void updateCollectionCompleteness(Long collectionId) {
        try {
            int filledFieldCount = collectionDataItemMapper.selectCountByCollectionId(collectionId);
            int totalFieldCount = collectionFieldConfigMapper.selectCountAllEnabled();

            BigDecimal completeness = BigDecimal.ZERO;
            if (totalFieldCount > 0) {
                completeness = BigDecimal.valueOf(filledFieldCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalFieldCount), 2, RoundingMode.HALF_UP);
            }

            CollectionRecord updateRecord = new CollectionRecord();
            updateRecord.setId(collectionId);
            updateRecord.setCompleteness(completeness);
            updateRecord.setUpdateTime(new Date());
            collectionRecordMapper.updateById(updateRecord);
        } catch (Exception e) {
            log.error("体检信息{}：完整性计算异常", collectionId, e);
        }
    }

    // ======================== 单文件导入 ========================
    @Override
    public void importSingleFile(MultipartFile file,
                                 boolean overrideIfExist,
                                 List<BatchImportResponse.ImportFailDetail> failDetails,
                                 List<Long> successCollectionIds) {
        BatchImportRequest request = new BatchImportRequest();
        request.setFiles(Collections.singletonList(file));
        request.setOverrideIfExist(overrideIfExist);
        BatchImportResponse response = batchImport(request);

        if (!CollectionUtils.isEmpty(response.getFailReasons())) {
            failDetails.addAll(response.getFailReasons());
        }
        if (!CollectionUtils.isEmpty(response.getCollectionIds())) {
            successCollectionIds.addAll(response.getCollectionIds());
        }
    }
}
