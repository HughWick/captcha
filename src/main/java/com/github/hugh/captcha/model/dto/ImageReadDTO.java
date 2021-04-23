package com.github.hugh.captcha.model.dto;

import lombok.Data;

import java.awt.image.BufferedImage;

@Data
public class ImageReadDTO {

    private BufferedImage image;// 原图

    private String fileExtension;// 原图文件类型

    private java.io.InputStream InputStream;
}
