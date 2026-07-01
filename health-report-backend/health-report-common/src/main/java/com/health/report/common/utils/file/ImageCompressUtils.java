package com.health.report.common.utils.file;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片压缩工具类
 */
public class ImageCompressUtils {

    /**
     * 目标大小：1MB
     */
    private static final long TARGET_SIZE = 1024 * 1024L;

    /**
     * 最小质量值
     */
    private static final float MIN_QUALITY = 0.1f;

    /**
     * 质量递减步长
     */
    private static final float QUALITY_STEP = 0.1f;

    /**
     * 压缩图片到指定大小以内
     *
     * @param originalBytes 原始图片字节数组
     * @param formatName 图片格式（如 jpg, png）
     * @return 压缩后的图片字节数组
     * @throws IOException 如果压缩失败
     */
    public static byte[] compressImage(byte[] originalBytes, String formatName) throws IOException {
        // 如果原始图片已经小于等于目标大小，直接返回
        if (originalBytes.length <= TARGET_SIZE) {
            return originalBytes;
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (image == null) {
            throw new IOException("无法读取图片");
        }

        // 先尝试通过降低质量来压缩
        byte[] compressed = compressByQuality(image, formatName);
        if (compressed != null && compressed.length <= TARGET_SIZE) {
            return compressed;
        }

        // 如果降低质量还不行，再尝试缩小尺寸
        return compressBySize(image, formatName);
    }

    /**
     * 通过降低质量压缩图片
     */
    private static byte[] compressByQuality(BufferedImage image, String formatName) throws IOException {
        float quality = 1.0f;

        while (quality >= MIN_QUALITY) {
            byte[] result = writeImageWithQuality(image, formatName, quality);
            if (result != null && result.length <= TARGET_SIZE) {
                return result;
            }
            quality -= QUALITY_STEP;
        }

        return null;
    }

    /**
     * 通过缩小尺寸压缩图片
     */
    private static byte[] compressBySize(BufferedImage image, String formatName) throws IOException {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // 从90%开始缩小
        double scale = 0.9;

        while (scale > 0.1) {
            int newWidth = (int) (originalWidth * scale);
            int newHeight = (int) (originalHeight * scale);

            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            // 先用80%质量尝试
            byte[] result = writeImageWithQuality(scaledImage, formatName, 0.8f);
            if (result != null && result.length <= TARGET_SIZE) {
                return result;
            }

            // 如果80%质量还不行，再尝试降低质量
            result = compressByQuality(scaledImage, formatName);
            if (result != null && result.length <= TARGET_SIZE) {
                return result;
            }

            scale -= 0.1;
        }

        // 尝试最小质量和最小尺寸
        BufferedImage minImage = new BufferedImage(
                Math.max(100, (int) (originalWidth * 0.1)),
                Math.max(100, (int) (originalHeight * 0.1)),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g2d = minImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, minImage.getWidth(), minImage.getHeight(), null);
        g2d.dispose();

        return writeImageWithQuality(minImage, formatName, MIN_QUALITY);
    }

    /**
     * 使用指定质量写入图片
     */
    private static byte[] writeImageWithQuality(BufferedImage image, String formatName, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 获取ImageWriter
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName.toLowerCase());
        if (!writers.hasNext()) {
            // 如果没有找到指定格式的writer，尝试使用jpg
            writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IOException("不支持的图片格式: " + formatName);
            }
        }

        ImageWriter writer = writers.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        // 设置压缩参数
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }
}
