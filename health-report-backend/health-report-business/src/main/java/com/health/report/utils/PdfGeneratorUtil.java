package com.health.report.utils;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.*;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * PDF生成工具类
 * 使用OpenHTMLToPDF将HTML转换为PDF
 */
@Component
public class PdfGeneratorUtil {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorUtil.class);

    /**
     * 生成 HTML 文件（用于测试、预览）
     * @param htmlContent 填充后的 HTML 内容
     * @param outputPath  PDF路径（会自动替换成 .html）
     * @return html文件
     */
    public File generateTestHtmlFile(String htmlContent, String outputPath) {
        try {
            // 把 .pdf 路径改成 .html
            String htmlPath = outputPath.replace(".pdf", ".html");
            File htmlFile = new File(htmlPath);

            // 确保目录存在
            if (!htmlFile.getParentFile().exists()) {
                htmlFile.getParentFile().mkdirs();
            }

            // 写入 HTML
            try (FileWriter writer = new FileWriter(htmlFile, StandardCharsets.UTF_8)) {
                writer.write(htmlContent);
            }

            log.info("✅ 测试 HTML 已生成: {}", htmlPath);
            return htmlFile;
        } catch (Exception e) {
            log.error("❌ 生成测试 HTML 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将HTML内容转换为PDF文件（支持中文）
     *
     * @param htmlContent HTML内容
     * @param outputPath  输出PDF文件路径
     * @param addWatermark 是否添加水印
     * @return 生成的PDF文件
     */
    public File generatePdfFromHtml(String htmlContent, String outputPath, boolean addWatermark,boolean en) {
        try {
            File outputFile = new File(outputPath);
            // 确保父目录存在
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 先生成临时PDF文件
            File tempFile = File.createTempFile("temp_", ".pdf");

            try (OutputStream os = new FileOutputStream(tempFile)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();

                // 添加中文字体支持
                addChineseFonts(builder,en);
                // 修复HTML实体问题：确保HTML有正确的DOCTYPE声明
                String processedHtml = ensureValidXhtml(htmlContent);

                builder.withHtmlContent(processedHtml, null);
                builder.toStream(os);
                builder.run();
            }

            // 如果需要添加水印，使用iText处理
            if (addWatermark) {
                addWatermarkToPdf(tempFile, outputFile, "C-Omega");
                tempFile.delete(); // 删除临时文件
            } else {
                // 直接复制到目标位置
                Files.copy(tempFile.toPath(), outputFile.toPath());
                tempFile.delete();
            }

            log.info("PDF生成成功: {}", outputPath);
            return outputFile;
        } catch (Exception e) {
            log.error("PDF生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("PDF生成失败", e);
        }
    }

    /**
     * 使用iText给PDF添加倾斜水印
     */
    private void addWatermarkToPdf(File inputFile, File outputFile, String watermarkText) {
        try {
            PdfReader reader = new PdfReader(inputFile.getAbsolutePath());
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outputFile));

            // 获取总页数
            int pageCount = reader.getNumberOfPages();

            // 创建水印字体（使用基础字体，支持中文）
            BaseFont baseFont = BaseFont.createFont("Helvetica", BaseFont.WINANSI, BaseFont.EMBEDDED);

            // 为每一页添加水印
            for (int i = 1; i <= pageCount; i++) {
                // 使用 overContent 在内容层之上添加水印
                PdfContentByte overContent = stamper.getOverContent(i);

                // 保存当前状态
                overContent.saveState();

                // 设置透明度
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.2f);
                overContent.setGState(gs);

                // 设置字体和颜色
                overContent.setFontAndSize(baseFont, 24);
                overContent.setColorFill(BaseColor.LIGHT_GRAY);

                // 获取页面尺寸
                com.itextpdf.text.Rectangle pageSize = reader.getPageSize(i);
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                // 在多个位置添加倾斜水印（一行2-3个，间距更大）
                float[][] positions = {
                        {80, pageHeight - 150},
                        {300, pageHeight - 150},
                        {520, pageHeight - 150},
                        {80, pageHeight - 450},
                        {300, pageHeight - 450},
                        {520, pageHeight - 450},
                        {80, pageHeight - 750},
                        {300, pageHeight - 750},
                        {520, pageHeight - 750}
                };

                for (float[] pos : positions) {
                    // 保存状态
                    overContent.saveState();

                    // 移动到指定位置并旋转
                    overContent.concatCTM(
                            (float) Math.cos(Math.toRadians(-30)),
                            (float) Math.sin(Math.toRadians(-30)),
                            -(float) Math.sin(Math.toRadians(-30)),
                            (float) Math.cos(Math.toRadians(-30)),
                            pos[0], pos[1]
                    );

                    // 绘制文字
                    overContent.beginText();
                    overContent.showTextAligned(Element.ALIGN_CENTER, watermarkText, 0, 0, 0);
                    overContent.endText();

                    // 恢复状态
                    overContent.restoreState();
                }

                // 恢复状态
                overContent.restoreState();
            }

            stamper.close();
            reader.close();

            log.info("PDF水印添加成功");
        } catch (Exception e) {
            log.error("添加PDF水印失败: {}", e.getMessage(), e);
            // 如果添加水印失败，直接复制原文件
            try {
                Files.copy(inputFile.toPath(), outputFile.toPath());
            } catch (IOException ioException) {
                throw new RuntimeException("复制PDF文件失败", ioException);
            }
        }
    }

    /**
     * 添加中文字体支持
     */
    private void addChineseFonts(PdfRendererBuilder builder,boolean en) {

        try {
            if(en){
                // 尝试加载系统字体
                String[] fontPaths = {
                        "C:/Windows/Fonts/Times New Roman.ttc",  // 宋体
                        "/usr/share/fonts/TIMES.ttc",
                };

                for (String fontPath : fontPaths) {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists()) {
                        log.info("加载字体: {}", fontPath);
                        builder.useFont(fontFile, "Times New Roman");
                        break;
                    }
                }
            }else {
                // 尝试加载系统字体
                String[] fontPaths = {
                        "C:/Windows/Fonts/simsun.ttc",  // 宋体
                        "C:/Windows/Fonts/simhei.ttf",  // 黑体
                        "C:/Windows/Fonts/simkai.ttf",  // 楷体
                        "C:/Windows/Fonts/msyh.ttc",    // 微软雅黑
                        "/usr/share/fonts/simsun.ttc",
                        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",  // Linux文泉驿
                        "/System/Library/Fonts/PingFang.ttc",  // Mac PingFang
                };

                for (String fontPath : fontPaths) {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists()) {
                        log.info("加载字体: {}", fontPath);
                        builder.useFont(fontFile, "SimSun");
                        builder.useFont(fontFile, "Microsoft YaHei");
                        builder.useFont(fontFile, "PingFang SC");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("加载中文字体失败: {}", e.getMessage());
        }
    }

    /**
     * 确保HTML内容是有效的XHTML，修复常见的实体引用问题
     *
     * @param htmlContent 原始HTML内容
     * @return 处理后的XHTML内容
     */
    private String ensureValidXhtml(String htmlContent) {
        String processed = htmlContent;

        // 如果缺少DOCTYPE声明，添加XHTML DOCTYPE
        if (!processed.trim().toLowerCase().startsWith("<!doctype")) {
            processed = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" + processed;
        }

        // 替换常见的HTML命名实体为数字实体（XML兼容）
        processed = processed.replaceAll("&nbsp;", "&#160;");
        processed = processed.replaceAll("&lt;", "&#60;");
        processed = processed.replaceAll("&gt;", "&#62;");
        processed = processed.replaceAll("&amp;", "&#38;");
        processed = processed.replaceAll("&quot;", "&#34;");
        processed = processed.replaceAll("&apos;", "&#39;");
        processed = processed.replaceAll("&copy;", "&#169;");
        processed = processed.replaceAll("&reg;", "&#174;");
        processed = processed.replaceAll("&trade;", "&#8482;");
        processed = processed.replaceAll("&mdash;", "&#8212;");
        processed = processed.replaceAll("&ndash;", "&#8211;");
        processed = processed.replaceAll("&hellip;", "&#8230;");
        processed = processed.replaceAll("&bull;", "&#8226;");
        processed = processed.replaceAll("&middot;", "&#183;");
        processed = processed.replaceAll("&lsquo;", "&#8216;");
        processed = processed.replaceAll("&rsquo;", "&#8217;");
        processed = processed.replaceAll("&ldquo;", "&#8220;");
        processed = processed.replaceAll("&rdquo;", "&#8221;");

        return processed;
    }
    /**
     * 读取HTML模板文件
     *
     * @param templateName 模板文件名
     * @return 模板内容
     */
    public String loadHtmlTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + templateName);
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            log.error("读取HTML模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取HTML模板失败", e);
        }
    }

    /**
     * 将文件转换为Base64编码
     *
     * @param filePath 文件路径
     * @return Base64编码字符串
     */
    public String fileToBase64(String filePath) {
        try {
            Path path = new File(filePath).toPath();
            byte[] bytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("文件转Base64失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件转Base64失败", e);
        }
    }

    /**
     * 将字节数组转换为Base64编码
     *
     * @param bytes 字节数组
     * @return Base64编码字符串
     */
    public String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
