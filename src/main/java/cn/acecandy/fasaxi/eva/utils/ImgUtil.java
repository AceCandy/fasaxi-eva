package cn.acecandy.fasaxi.eva.utils;

import cn.hutool.core.io.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * 图片 工具类
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
public final class ImgUtil {
    private ImgUtil() {
    }

    public static InputStream addAdversarialNoise(String imagePath, float intensity) {
        try {
            File file = new File(imagePath);
            BufferedImage originalImage = ImageIO.read(file);
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            BufferedImage noisyImage = new BufferedImage(width, height, originalImage.getType());
            Random random = new Random();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = originalImage.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xff;
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = pixel & 0xff;

                    // 为每个颜色通道生成独立噪声
                    int deltaRed = (int) ((random.nextFloat() * 2 - 1) * intensity * 255);
                    int deltaGreen = (int) ((random.nextFloat() * 2 - 1) * intensity * 255);
                    int deltaBlue = (int) ((random.nextFloat() * 2 - 1) * intensity * 255);

                    red = clamp(red + deltaRed, 0, 255);
                    green = clamp(green + deltaGreen, 0, 255);
                    blue = clamp(blue + deltaBlue, 0, 255);

                    int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                    noisyImage.setRGB(x, y, newPixel);
                }
            }

            // 将处理后的图像写入内存流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(noisyImage, "png", outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error processing image: " + e.getMessage(), e);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // 覆盖模式枚举
    public enum ScaleMode {
        WIDTH,       // 按宽度比例缩放
        HEIGHT,      // 按高度比例缩放
        CONTAIN,     // 保持比例最大适应
        COVER        // 保持比例完全覆盖
    }

    /**
     * 添加比例缩放水印（默认宽度比例50%）
     *
     * @param imagePath     原始图片路径
     * @param watermarkPath 水印图片路径
     * @param opacity       透明度 0.0-1.0
     * @param scale         缩放比例 (0-1)
     */
    public static InputStream addProportionalWatermark(String imagePath,
                                                       String watermarkPath,
                                                       float opacity,
                                                       float scale) {
        return addProportionalWatermark(imagePath, watermarkPath, opacity, scale, ScaleMode.WIDTH, null);
    }

    /**
     * 高级版比例水印
     *
     * @param scale  缩放比例 (0-1)
     * @param mode   缩放模式
     * @param offset 位置偏移量（基于居中后的坐标）
     */
    public static InputStream addProportionalWatermark(String imagePath,
                                                       String watermarkPath,
                                                       float opacity,
                                                       float scale,
                                                       ScaleMode mode,
                                                       Point offset) {
        try {
            BufferedImage original = ImageIO.read(new File(imagePath));
            BufferedImage watermark = ImageIO.read(new File(watermarkPath));

            // 计算缩放后尺寸
            Dimension scaledSize = calculateScaledSize(
                    new Dimension(original.getWidth(), original.getHeight()),
                    new Dimension(watermark.getWidth(), watermark.getHeight()),
                    scale,
                    mode
            );

            // 缩放水印
            Image scaledWatermark = watermark.getScaledInstance(
                    scaledSize.width,
                    scaledSize.height,
                    Image.SCALE_SMOOTH
            );

            // 转换为BufferedImage
            BufferedImage scaledBuffered = new BufferedImage(
                    scaledSize.width,
                    scaledSize.height,
                    BufferedImage.TYPE_INT_ARGB
            );
            scaledBuffered.getGraphics().drawImage(scaledWatermark, 0, 0, null);

            // 创建画布
            BufferedImage combined = new BufferedImage(
                    original.getWidth(),
                    original.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = combined.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));

            // 计算居中坐标
            int x = (original.getWidth() - scaledSize.width) / 2;
            int y = (original.getHeight() - scaledSize.height) / 2;

            // 应用偏移量
            if (offset != null) {
                x += offset.x;
                y += offset.y;
            }

            // 边界保护
            x = Math.max(0, Math.min(x, original.getWidth() - scaledSize.width));
            y = Math.max(0, Math.min(y, original.getHeight() - scaledSize.height));

            // 绘制水印
            g.rotate(Math.toRadians(180), x + scaledSize.getWidth() / 2, y + scaledSize.getHeight() / 2);
            g.drawImage(scaledBuffered, x, y, null);
            g.dispose();

            // 输出流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(combined, "png", baos);
            return new ByteArrayInputStream(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("添加比例水印失败: " + e.getMessage());
        }
    }

    // 计算缩放尺寸的核心算法
    private static Dimension calculateScaledSize(Dimension original,
                                                 Dimension watermark,
                                                 float scale,
                                                 ScaleMode mode) {
        switch (mode) {
            case WIDTH:
                int targetWidth = (int) (original.width * scale);
                int height = (int) (watermark.height * ((float) targetWidth / watermark.width));
                return new Dimension(targetWidth, height);

            case HEIGHT:
                int targetHeight = (int) (original.height * scale);
                int width = (int) (watermark.width * ((float) targetHeight / watermark.height));
                return new Dimension(width, targetHeight);

            case CONTAIN:
                float widthRatio = (original.width * scale) / (float) watermark.width;
                float heightRatio = (original.height * scale) / (float) watermark.height;
                float ratio = Math.min(widthRatio, heightRatio);
                return new Dimension(
                        (int) (watermark.width * ratio),
                        (int) (watermark.height * ratio)
                );

            case COVER:
                float cWidthRatio = (original.width * scale) / (float) watermark.width;
                float cHeightRatio = (original.height * scale) / (float) watermark.height;
                float cRatio = Math.max(cWidthRatio, cHeightRatio);
                return new Dimension(
                        (int) (watermark.width * cRatio),
                        (int) (watermark.height * cRatio)
                );

            default:
                throw new IllegalArgumentException("不支持的缩放模式");
        }
    }

    public static void main(String[] args) {
        //, "/Users/mac/Downloads/浏览器/output.jpg"
        // FileUtil.writeFromStream(addAdversarialNoise("/Users/mac/Downloads/浏览器/wdzsj.jpg", 0.6f),
        //         "/Users/mac/Downloads/浏览器/output1.png");

        FileUtil.writeFromStream(
                addAdversarialNoise("/Users/mac/Downloads/浏览器/wdzsj.jpg", 0.3f),
                "/Users/mac/Downloads/浏览器/output.png");
        FileUtil.writeFromStream(
                addProportionalWatermark("/Users/mac/Downloads/浏览器/output.png",
                        "/Users/mac/Downloads/浏览器/Steins_Gate_Elite_Teaser.jpg", 0.3f, 1, ScaleMode.COVER, null),
                "/Users/mac/Downloads/浏览器/output1.png");

    }
}