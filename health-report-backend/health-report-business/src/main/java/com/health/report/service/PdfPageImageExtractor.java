package com.health.report.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * PDF页面图片提取器
 * 使用多线程并行渲染PDF页面为图片，提高处理速度
 */
@Component
public class PdfPageImageExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfPageImageExtractor.class);

    @Value("${ocr.pdf.render-dpi:150}")
    private int renderDpi;

    @Value("${ocr.pdf.max-pages:50}")
    private int maxPages;

    @Value("${ocr.pdf.render-threads:4}")
    private int renderThreads;

    /**
     * 将PDF页面渲染为临时PNG文件
     * 使用线程池并行渲染，提高大PDF处理速度
     */
    public List<Path> renderToPngFiles(InputStream pdfInput) throws IOException {
        byte[] bytes = toByteArray(pdfInput);
        try (PDDocument document = PDDocument.load(bytes)) {
            int total = document.getNumberOfPages();
            int pages = Math.min(total, maxPages);
            List<Path> paths = new ArrayList<>(pages);
            if (pages == 0) {
                return paths;
            }

            // 使用线程池并行渲染PDF页面
            ExecutorService pool = Executors.newFixedThreadPool(Math.min(renderThreads, pages));
            try {
                List<Future<Path>> futures = new ArrayList<>(pages);
                for (int i = 0; i < pages; i++) {
                    final int pageIndex = i;
                    Callable<Path> task = () -> renderOnePage(bytes, pageIndex);
                    futures.add(pool.submit(task));
                }
                for (Future<Path> f : futures) {
                    paths.add(f.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("PDF render interrupted", e);
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof IOException) {
                    throw (IOException) c;
                }
                throw new IOException("PDF render failed", c != null ? c : e);
            } finally {
                pool.shutdown();
            }
            return paths;
        }
    }

    /**
     * 渲染单页PDF为PNG图片
     */
    private Path renderOnePage(byte[] pdfBytes, int pageIndex) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, renderDpi, ImageType.RGB);
            Path temp = Files.createTempFile("ocr-pdf-page-", ".png");
            temp.toFile().deleteOnExit();
            ImageIO.write(image, "png", temp.toFile());
            return temp;
        }
    }

    /**
     * 将InputStream转换为byte数组
     */
    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        while ((n = in.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }
}
