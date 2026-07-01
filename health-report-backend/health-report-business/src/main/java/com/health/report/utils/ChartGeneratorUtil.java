package com.health.report.utils;

import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 图表生成工具类
 * 用于生成雷达图等健康报告图表
 */
@Component
public class ChartGeneratorUtil {

    private static final Logger log = LoggerFactory.getLogger(ChartGeneratorUtil.class);

    // 默认系统标签（按参考图片顺序）- 当未提供动态标签时使用
    private static final String[] DEFAULT_SYSTEM_LABELS = {
            "整体情况", "心血管系统", "肾脏", "消化系统", "骨骼肌肉系统",
            "呼吸系统", "免疫系统", "内分泌系统", "营养评估", "造血系统"
    };

    /**
     * 生成健康评估雷达图 - 显示两条线（黄色生物学年龄 + 蓝色真实年龄）
     */
    public String generateRadarChartBase64(Map<String, Double> biologyAges, double actualAge, List<String> systemLabels, String lang) {
        try {
            int width = 700;
            int height = 700;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawRadarChartWithTwoLines(g2d, biologyAges, actualAge, width, height, systemLabels, lang);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("生成雷达图失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 绘制双线条雷达图 - 固定 0~100 刻度
     */
    private void drawRadarChartWithTwoLines(Graphics2D g2d, Map<String, Double> biologyAges,
                                            double actualAge, int width, int height, List<String> systemLabels, String lang) {
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 2 - 60;

        String[] labels = (systemLabels != null && !systemLabels.isEmpty())
                ? systemLabels.toArray(new String[0])
                : DEFAULT_SYSTEM_LABELS;
        int numAxes = labels.length;

        // ========== 1. 年龄转 0-100 分：±30岁区间 ==========
        final int MAX_SCORE = 100;
        double minAge = 0;
        double maxAge = 100;
        double ageRange = maxAge - minAge;

        // 网格 10 层（0-100分）
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.setStroke(new BasicStroke(2f));
        for (int i = 1; i <= 10; i++) {
            int r = radius * i / 10;
            int[] xPoints = new int[numAxes];
            int[] yPoints = new int[numAxes];
            for (int j = 0; j < numAxes; j++) {
                double angle = Math.PI / 2 - j * 2 * Math.PI / numAxes;
                xPoints[j] = centerX + (int) (r * Math.cos(angle));
                yPoints[j] = centerY - (int) (r * Math.sin(angle));
            }
            g2d.drawPolygon(xPoints, yPoints, numAxes);
        }

        // 轴线
        g2d.setColor(new Color(180, 180, 180, 120));
        g2d.setStroke(new BasicStroke(2f));
        for (int i = 0; i < numAxes; i++) {
            double angle = Math.PI / 2 - i * 2 * Math.PI / numAxes;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY - (int) (radius * Math.sin(angle));
            g2d.drawLine(centerX, centerY, x, y);
        }

        // ========== 2. 实际年龄线：固定 50 分（基准） ==========
        int[] actualAgeX = new int[numAxes];
        int[] actualAgeY = new int[numAxes];
        double actualScore = (actualAge - minAge) / ageRange * MAX_SCORE;
        actualScore = Math.max(0, Math.min(MAX_SCORE, actualScore));
        for (int i = 0; i < numAxes; i++) {
            double angle = Math.PI / 2 - i * 2 * Math.PI / numAxes;
            int r = (int) (radius * actualScore / MAX_SCORE);
            actualAgeX[i] = centerX + (int) (r * Math.cos(angle));
            actualAgeY[i] = centerY - (int) (r * Math.sin(angle));
        }

        g2d.setColor(new Color(66, 133, 244, 200));
        g2d.setStroke(new BasicStroke(4f));
        g2d.drawPolygon(actualAgeX, actualAgeY, numAxes);

        // ========== 3. 生理年龄线：年龄转 0-100 分 ==========
        int[] bioAgeX = new int[numAxes];
        int[] bioAgeY = new int[numAxes];

        for (int i = 0; i < numAxes; i++) {
            String label = labels[i];
            double bioAge = biologyAges.getOrDefault(label, actualAge);

            // 关键：年龄转成 0-100 分
            double bioScore = (bioAge - minAge) / ageRange * MAX_SCORE;
            bioScore = Math.max(0, Math.min(MAX_SCORE, bioScore));

            double angle = Math.PI / 2 - i * 2 * Math.PI / numAxes;
            int r = (int) (radius * bioScore / MAX_SCORE);
            bioAgeX[i] = centerX + (int) (r * Math.cos(angle));
            bioAgeY[i] = centerY - (int) (r * Math.sin(angle));
        }

        g2d.setColor(new Color(251, 188, 5, 230));
        g2d.setStroke(new BasicStroke(5f));
        g2d.drawPolygon(bioAgeX, bioAgeY, numAxes);

        // 数据点
        g2d.setColor(new Color(251, 188, 5));
        for (int i = 0; i < numAxes; i++) {
            g2d.fillOval(bioAgeX[i] - 4, bioAgeY[i] - 4, 8, 8);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(bioAgeX[i] - 4, bioAgeY[i] - 4, 8, 8);
            g2d.setColor(new Color(251, 188, 5));
        }

        g2d.setColor(new Color(66, 133, 244));
        for (int i = 0; i < numAxes; i++) {
            g2d.fillOval(actualAgeX[i] - 3, actualAgeY[i] - 3, 6, 6);
        }

        drawHumanBodyBackground(g2d, centerX, centerY, radius);

        g2d.setFont(new Font("SimSun", Font.BOLD, 13));
        g2d.setColor(new Color(80, 80, 80));
        for (int i = 0; i < numAxes; i++) {
            double angle = Math.PI / 2 - i * 2 * Math.PI / numAxes;
            int labelRadius = radius + 15;
            int x = centerX + (int) (labelRadius * Math.cos(angle));
            int y = centerY - (int) (labelRadius * Math.sin(angle));

            String label = labels[i];
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);

            x -= textWidth / 2;
            y += fm.getAscent() / 2;

            g2d.drawString(label, x, y);
        }

        drawLegendAtBottom(g2d, width, height, lang);
    }

    private void drawLegendAtBottom(Graphics2D g2d, int width, int height, String lang) {
        try {
            if (lang.equals("zh")){
                g2d.setFont(new Font("SimSun", Font.PLAIN, 16));
            }else{
                g2d.setFont(new Font("Times New Roman", Font.PLAIN, 16));
            }
            g2d.setColor(Color.BLACK);

            int yPos = height - 10;
            int center = width / 2;

            g2d.setColor(new Color(66, 133, 244));
            g2d.setStroke(new BasicStroke(4f));
            g2d.drawLine(center - 180, yPos, center - 150, yPos);
            g2d.setColor(Color.BLACK);
            g2d.drawString(lang.equals("zh") ? "实际年龄" : "Chronological Age", center - 140, yPos + 5);

            g2d.setColor(new Color(251, 188, 5));
            g2d.setStroke(new BasicStroke(4f));
            g2d.drawLine(center + 20, yPos, center + 50, yPos);
            g2d.setColor(Color.BLACK);
            g2d.drawString(lang.equals("zh") ? "生理学年龄" : "Biological Age", center + 60, yPos + 5);
        } catch (Exception e) {
            log.error("绘制底部图例失败", e);
        }
    }

    private void drawHumanBodyBackground(Graphics2D g2d, int centerX, int centerY, int radius) {
        try {
            ClassPathResource resource = null;
            InputStream inputStream = null;
            String[] extensions = {".png", ".jpg", ".jpeg"};

            for (String ext : extensions) {
                ClassPathResource tempRes = new ClassPathResource("templates/human-body" + ext);
                if (tempRes.exists()) {
                    resource = tempRes;
                    inputStream = tempRes.getInputStream();
                    break;
                }
            }

            if (inputStream != null) {
                BufferedImage bodyImage = ImageIO.read(inputStream);

                if (bodyImage != null) {
                    int radarDiameter = radius * 2;
                    int bodyHeight = (int) (radarDiameter * 1.8);
                    double aspectRatio = (double) bodyImage.getWidth() / bodyImage.getHeight();
                    int bodyWidth = (int) (bodyHeight * aspectRatio);

                    int drawX = centerX - bodyWidth / 2;
                    int offsetY = -(int) (radarDiameter * 0.25);
                    int drawY = centerY - bodyHeight / 2 + offsetY;

                    AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
                    g2d.setComposite(alphaComposite);
                    g2d.drawImage(bodyImage, drawX, drawY, bodyWidth, bodyHeight, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                    inputStream.close();
                }
            }
        } catch (Exception e) {
            log.error("绘制人体背景图失败: {}", e.getMessage(), e);
        }
    }
}