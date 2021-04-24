package com.github.hugh.captcha.util.image;

import com.github.hugh.captcha.exception.CaptchaException;
import com.github.hugh.captcha.model.dto.ImageReadDTO;
import com.github.hugh.captcha.util.io.StreamUtils;
import com.google.common.io.Files;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * 图片读取工具类
 *
 * @author hugh
 * @since 1.0.0
 */
public class ImageUtils {

    /**
     * 读取默认的验证图片
     *
     * @return ImageReadDTO
     */
    public static ImageReadDTO readTargetImage() {
        return readTargetImage("/origin/", "jpg");
    }

    /**
     * 读取滑块模板
     *
     * @return ImageReadDTO
     */
    public static ImageReadDTO readTemplateImage() {
        try {
            String filePath = "/templates/template.png";
            InputStream inputStream = StreamUtils.getInputStream(filePath);
            File temp = File.createTempFile("testrunoobtmp", ".png");
            StreamUtils.toFile(inputStream, temp);
            return readImage(temp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CaptchaException(e);
        }
    }

    /**
     * 读取 空的滑块文件
     *
     * @return ImageReadDTO
     */
    public static ImageReadDTO readBorderImageFile() {
        try {
            String filePath = "/templates/border.png";
            InputStream inputStream = StreamUtils.getInputStream(filePath);
            File temp = File.createTempFile("testrunoobtmp", ".png");
            StreamUtils.toFile(inputStream, temp);
            return readImage(temp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CaptchaException(e);
        }
    }

    /**
     * 读取目标图
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @return ImageReadDTO
     */
    public static ImageReadDTO readTargetImage(String prefix, String suffix) {
        try {
            Random random = new Random(System.currentTimeMillis());
            int i = random.nextInt(7);
            while (i == 0) {
                i = random.nextInt(7);
            }
            String filePath = prefix + i + "." + suffix;
            InputStream inputStream = StreamUtils.getInputStream(filePath);
            File temp = File.createTempFile("testrunoobtmp", ".jpg");
            StreamUtils.toFile(inputStream, temp);
            return readImage(temp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CaptchaException(e);
        }
    }

    /**
     * 读取file内的 {@link FileInputStream} 、{@link java.awt.image.BufferedImage} 与文件后缀
     *
     * @param file 文件
     * @return ImageReadDTO
     */
    private static ImageReadDTO readImage(File file) {
        if (file == null) {
            throw new CaptchaException("file is null");
        }
        ImageReadDTO imageDTO = new ImageReadDTO();
        try {
            imageDTO.setImage(ImageIO.read(file));
            // 获取文件后缀
            String extension = Files.getFileExtension(file.getName());
            imageDTO.setFileExtension(extension);
            imageDTO.setInputStream(new FileInputStream(file));
        } catch (IOException e) {
            throw new CaptchaException(e);
        }
        return imageDTO;
    }
}
