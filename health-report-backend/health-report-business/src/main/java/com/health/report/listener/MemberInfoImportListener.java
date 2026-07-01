package com.health.report.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.health.report.domain.MemberInfo;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 会员信息导入监听器（支持中英文表头）
 */
@Slf4j
public class MemberInfoImportListener extends AnalysisEventListener<Map<Integer, String>> {

    private final List<MemberInfo> memberList;
    private Map<Integer, String> headerMap = new HashMap<>();
    private boolean isHeaderProcessed = false;
    private boolean isEnglish = false;

    // 中英文表头映射
    private static final Map<String, String> HEADER_MAPPING = new HashMap<>() {{
        // 中文表头
        put("姓名", "name");
        put("性别", "gender");
        put("关系", "relationship");
        put("出生日期", "birthDate");
        put("联系方式", "contact");
        put("地址", "address");
        put("备注", "remark");
        // 英文表头
        put("Name", "name");
        put("Gender", "gender");
        put("Relationship", "relationship");
        put("Birth Date", "birthDate");
        put("Contact", "contact");
        put("Address", "address");
        put("Remark", "remark");
    }};

    public MemberInfoImportListener(List<MemberInfo> memberList) {
        this.memberList = memberList;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        // 处理表头，建立列索引到字段名的映射
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            String headerName = entry.getValue().trim();
            String fieldName = HEADER_MAPPING.get(headerName);
            if (fieldName != null) {
                headerMap.put(entry.getKey(), fieldName);
            }
        }

        // 判断是否为英文模板（根据第一个表头判断）
        if (!headMap.isEmpty()) {
            String firstHeader = headMap.get(0);
            isEnglish = "Name".equals(firstHeader) || "Gender".equals(firstHeader);
        }

        isHeaderProcessed = true;
        log.info("导入表头：{}，语言：{}", headMap, isEnglish ? "英文" : "中文");
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        if (!isHeaderProcessed) {
            return;
        }

        MemberInfo member = new MemberInfo();

        for (Map.Entry<Integer, String> entry : data.entrySet()) {
            Integer colIndex = entry.getKey();
            String cellValue = entry.getValue();
            String fieldName = headerMap.get(colIndex);

            if (fieldName == null || cellValue == null) {
                continue;
            }

            cellValue = cellValue.trim();

            switch (fieldName) {
                case "name":
                    member.setName(cellValue);
                    break;
                case "gender":
                    member.setGender(parseGender(cellValue));
                    break;
                case "relationship":
                    member.setRelationship(parseRelationship(cellValue));
                    break;
                case "birthDate":
                    member.setBirthDate(parseDate(cellValue));
                    break;
                case "contact":
                    member.setContact(cellValue);
                    break;
                case "address":
                    member.setAddress(cellValue);
                    break;
                case "remark":
                    member.setRemark(cellValue);
                    break;
                default:
                    break;
            }
        }

        memberList.add(member);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("导入完成，共 {} 条数据", memberList.size());
    }

    /**
     * 解析性别
     * 中文：男=1，女=2
     * 英文：Male=1，Female=2
     */
    private Integer parseGender(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if ("男".equals(value) || "Male".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("女".equals(value) || "Female".equalsIgnoreCase(value)) {
            return 2;
        }
        return null;
    }

    /**
     * 解析关系
     * 中文：本人=1，家人=2，朋友=3
     * 英文：Self=1，Family=2，Friend=3
     */
    private Integer parseRelationship(String value) {
        if (value == null || value.isEmpty()) {
            return 1; // 默认本人
        }
        if ("本人".equals(value) || "Self".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("家人".equals(value) || "Family".equalsIgnoreCase(value)) {
            return 2;
        }
        if ("朋友".equals(value) || "Friend".equalsIgnoreCase(value)) {
            return 3;
        }
        return 1; // 默认本人
    }

    /**
     * 解析日期
     */
    private Date parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 尝试多种日期格式
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd", "yyyy年MM月dd日"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (ParseException e) {
                // 继续尝试下一个格式
            }
        }

        // 如果是数字（Excel日期格式）
        try {
            double excelDate = Double.parseDouble(value);
            // Excel日期是从1900年1月1日开始的天数
            // 这里简化处理，返回null让调用方处理
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
