package com.github.hugh.captcha.model.dto;

import lombok.Data;

import java.awt.image.BufferedImage;

/**
 * 文件读取的DTO
 *
 * @author hugh
 * @since 1.0.0
 */
@Data
public class ImageReadDTO {

    private BufferedImage image;// 原图

    private String fileExtension;// 原图文件类型

    private java.io.InputStream InputStream;
}
