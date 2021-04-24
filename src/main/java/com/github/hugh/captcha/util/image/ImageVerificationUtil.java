package com.github.hugh.captcha.util.image;

import com.github.hugh.captcha.exception.CaptchaException;
import com.github.hugh.captcha.model.dto.ImageReadDTO;
import com.github.hugh.captcha.model.dto.ImageVerificationDTO;
import com.github.hugh.captcha.model.vo.ImageVerificationVo;
import com.github.hugh.captcha.cache.GuavaCache;
import com.github.hugh.captcha.util.EmptyUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * 图片验证工具
 * -------------------
 *
 * @author hugh
 * @since 1.0.0
 */
@Slf4j
public class ImageVerificationUtil {

    /**
     * 默认图片宽度
     */
    private static final int DEFAULT_IMAGE_WIDTH = 280;

    /**
     * 默认图片高度
     */
    private static final int DEFAULT_IMAGE_HEIGHT = 171;

    /**
     * 生成感兴趣区域坐标
     *
     * @param verificationImage 源图
     * @param templateImage     模板图
     * @return 裁剪坐标
     */
    public static ImageVerificationVo generateCutoutCoordinates(BufferedImage verificationImage, BufferedImage templateImage) {
        int x, y;
        ImageVerificationVo imageVerificationVo = null;
        //  原图宽度
//        int VERIFICATION_IMAGE_WIDTH = verificationImage.getWidth();
        //  原图高度
//        int VERIFICATION_IMAGE_HEIGHT = verificationImage.getHeight();
        //  抠图模板宽度
        int templateImageWidth = templateImage.getWidth();
        //  抠图模板高度
        int templateImageHeight = templateImage.getHeight();
        Random random = new Random(System.currentTimeMillis());
        //  取范围内坐标数据，坐标抠图一定要落在原图中，否则会导致程序错误
        x = random.nextInt(DEFAULT_IMAGE_WIDTH - templateImageWidth) % (DEFAULT_IMAGE_WIDTH - templateImageWidth - templateImageWidth + 1) + templateImageWidth;
        y = random.nextInt(DEFAULT_IMAGE_HEIGHT - templateImageWidth) % (DEFAULT_IMAGE_HEIGHT - templateImageWidth - templateImageWidth + 1) + templateImageWidth;
        if (templateImageHeight - DEFAULT_IMAGE_HEIGHT >= 0) {
            y = random.nextInt(10);
        }
        imageVerificationVo = new ImageVerificationVo();
        imageVerificationVo.setX(x);
        imageVerificationVo.setY(y);
        return imageVerificationVo;
    }

    /**
     * 根据模板图裁剪图片，生成源图遮罩图和裁剪图
     *
     * @param originImage           源图文件
     * @param inputStream
     * @param originImageFileType   源图文件扩展名
     * @param templateImage         模板图文件
     * @param templateImageFileType 模板图文件扩展名
     * @param x                     感兴趣区域X轴
     * @param y                     感兴趣区域Y轴
     * @return
     */
    public static ImageVerificationVo pictureTemplateCutout(BufferedImage originImage, InputStream inputStream, String originImageFileType, BufferedImage templateImage, String templateImageFileType, int x, int y) {
        ImageVerificationVo imageVerificationVo = null;
        try {
            int templateImageWidth = templateImage.getWidth();
            int templateImageHeight = templateImage.getHeight();
            //  切块图   根据模板图尺寸创建一张透明图片
            BufferedImage cutoutImage = new BufferedImage(templateImageWidth, templateImageHeight, templateImage.getType());
            //  根据坐标获取感兴趣区域
            BufferedImage interestArea = getInterestArea(x, y, templateImageWidth, templateImageHeight, inputStream, originImageFileType);
            //  根据模板图片切图
            cutoutImageByTemplateImage(interestArea, templateImage, cutoutImage);
            //  图片绘图
            int bold = 5;
            Graphics2D graphics = cutoutImage.createGraphics();
            graphics.setBackground(Color.white);
            //  设置抗锯齿属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(cutoutImage, 0, 0, null);
            graphics.dispose();
            //  原图生成遮罩
            BufferedImage shadeImage = generateShadeByTemplateImage(originImage, templateImage, x, y);
            imageVerificationVo = new ImageVerificationVo();
            @Cleanup ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //  图片转为二进制字符串
            ImageIO.write(originImage, originImageFileType, byteArrayOutputStream);
            byte[] originImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.flush();
            byteArrayOutputStream.reset();
            //  图片加密成base64字符串
            String originImageString = java.util.Base64.getEncoder().encodeToString(originImageBytes);
            imageVerificationVo.setOriginImage(originImageString);

            ImageIO.write(shadeImage, templateImageFileType, byteArrayOutputStream);
            //  图片转为二进制字符串
            byte[] shadeImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.flush();
            byteArrayOutputStream.reset();
            //  图片加密成base64字符串
            String shadeImageString = java.util.Base64.getEncoder().encodeToString(shadeImageBytes);
            imageVerificationVo.setShadeImage(shadeImageString);
            ImageIO.write(cutoutImage, templateImageFileType, byteArrayOutputStream);
            //  图片转为二进制字符串
            byte[] cutoutImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.reset();
            //  图片加密成base64字符串
            String cutoutImageString = java.util.Base64.getEncoder().encodeToString(cutoutImageBytes);
            imageVerificationVo.setCutoutImage(cutoutImageString);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new CaptchaException();
        }
        return imageVerificationVo;
    }

    /**
     * 根据模板图生成遮罩图
     *
     * @param originImage   源图
     * @param templateImage 模板图
     * @param x             感兴趣区域X轴
     * @param y             感兴趣区域Y轴
     * @return 遮罩图
     */
    private static BufferedImage generateShadeByTemplateImage(BufferedImage originImage, BufferedImage templateImage, int x, int y) {
        //  根据原图，创建支持alpha通道的rgb图片
        BufferedImage shadeImage = new BufferedImage(originImage.getWidth(), originImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        //  原图片矩阵
        int[][] originImageMatrix = getMatrix(originImage);
        //  模板图片矩阵
        int[][] templateImageMatrix = getMatrix(templateImage);
        //  将原图的像素拷贝到遮罩图
        for (int i = 0; i < originImageMatrix.length; i++) {
            for (int j = 0; j < originImageMatrix[0].length; j++) {
                int rgb = originImage.getRGB(i, j);
                //  获取rgb色度
                int r = (0xff & rgb);
                int g = (0xff & (rgb >> 8));
                int b = (0xff & (rgb >> 16));
                //  无透明处理
                rgb = r + (g << 8) + (b << 16) + (255 << 24);
                shadeImage.setRGB(i, j, rgb);
            }
        }
        //  对遮罩图根据模板像素进行处理
        for (int i = 0; i < templateImageMatrix.length; i++) {
            for (int j = 0; j < templateImageMatrix[0].length; j++) {
                int rgb = templateImage.getRGB(i, j);
                //对源文件备份图像(x+i,y+j)坐标点进行透明处理
                if (rgb < 0) {
                    int originRGB = shadeImage.getRGB(x + i, y + j);
                    int r = (0xff & originRGB);
                    int g = (0xff & (originRGB >> 8));
                    int b = (0xff & (originRGB >> 16));
                    originRGB = r + (g << 8) + (b << 16) + (140 << 24);
                    //  对遮罩透明处理
                    shadeImage.setRGB(x + i, y + j, originRGB);
                    //  设置遮罩颜色
//                    shadeImage.setRGB(x + i, y + j, originRGB);
                }
            }
        }
        return shadeImage;
    }

    /**
     * 根据模板图抠图
     *
     * @param interestArea  感兴趣区域图
     * @param templateImage 模板图
     * @param cutoutImage   裁剪图
     * @return 裁剪图
     */
    private static BufferedImage cutoutImageByTemplateImage(BufferedImage interestArea, BufferedImage templateImage, BufferedImage cutoutImage) {
        //  获取兴趣区域图片矩阵
//        int[][] interestAreaMatrix = getMatrix(interestArea);
        //  获取模板图片矩阵
        int[][] templateImageMatrix = getMatrix(templateImage);
        //  将模板图非透明像素设置到剪切图中
        for (int i = 0; i < templateImageMatrix.length; i++) {
            for (int j = 0; j < templateImageMatrix[0].length; j++) {
                int rgb = templateImageMatrix[i][j];
                if (rgb < 0) {
                    cutoutImage.setRGB(i, j, interestArea.getRGB(i, j));
                }
            }
        }
        return cutoutImage;
    }

    /**
     * 图片生成图像矩阵
     *
     * @param bufferedImage 图片源
     * @return 图片矩阵
     */
    private static int[][] getMatrix(BufferedImage bufferedImage) {
        int[][] matrix = new int[bufferedImage.getWidth()][bufferedImage.getHeight()];
        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {
                matrix[i][j] = bufferedImage.getRGB(i, j);
            }
        }
        return matrix;
    }

    /**
     * 获取感兴趣区域
     *
     * @param x                   感兴趣区域X轴
     * @param y                   感兴趣区域Y轴
     * @param templateImageWidth  模板图宽度
     * @param templateImageHeight 模板图高度
     * @param originImageType     源图扩展名
     * @return BufferedImage
     */
    private static BufferedImage getInterestArea(int x, int y, int templateImageWidth, int templateImageHeight, InputStream inputStream, String originImageType) {
        try {
//            Iterator<ImageReader> imageReaderIterator = ImageIO.getImageReadersByFormatName(originImageType);
            Iterator<ImageReader> imageReaderIterator = ImageIO.getImageReadersBySuffix(originImageType);
            ImageReader imageReader = imageReaderIterator.next();
            //  获取图片流
            @Cleanup ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
            //  图片输入流顺序读写
            imageReader.setInput(imageInputStream, true);
            ImageReadParam imageReadParam = imageReader.getDefaultReadParam();
            //  根据坐标生成矩形
            Rectangle rectangle = new Rectangle(x, y, templateImageWidth, templateImageHeight);
            imageReadParam.setSourceRegion(rectangle);
            return imageReader.read(0, imageReadParam);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CaptchaException(e);
        }
    }

    /**
     * 切块图描边
     *
     * @param imageVerificationVo 图片容器
     * @param borderImage         描边图
     * @param borderImageFileType 描边图类型
     */
    public static void cutoutImageEdge(ImageVerificationVo imageVerificationVo, BufferedImage borderImage, String borderImageFileType) {
        try {
            String cutoutImageString = imageVerificationVo.getCutoutImage();
            //  图片解密成二进制字符创
            byte[] bytes = java.util.Base64.getDecoder().decode(cutoutImageString);
            @Cleanup ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            //  读取图片
            BufferedImage cutoutImage = ImageIO.read(byteArrayInputStream);
            //  获取模板边框矩阵， 并进行颜色处理
            int[][] borderImageMatrix = getMatrix(borderImage);
            for (int i = 0; i < borderImageMatrix.length; i++) {
                for (int j = 0; j < borderImageMatrix[0].length; j++) {
                    int rgb = borderImage.getRGB(i, j);
                    if (rgb < 0) {
                        cutoutImage.setRGB(i, j, -7237488);
                    }
                }
            }
            @Cleanup ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(cutoutImage, borderImageFileType, byteArrayOutputStream);
            //  新模板图描边处理后转成二进制字符串
            byte[] cutoutImageBytes = byteArrayOutputStream.toByteArray();
            //  二进制字符串加密成base64字符串
            String cutoutImageStr = java.util.Base64.getEncoder().encodeToString(cutoutImageBytes);
            imageVerificationVo.setCutoutImage(cutoutImageStr);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new CaptchaException(e);
        }
    }

    /**
     * 滑块验证
     *
     * @param imageVerificationDto 封装图片验证码
     * @return ImageVerificationVo
     */
    public static ImageVerificationVo slideVerificationCode(ImageVerificationDTO imageVerificationDto) {
        try {
            //  随机取得原图文件夹中一张图片
            ImageReadDTO imageReadDTO = ImageUtils.readTargetImage();
            //  获取模板图片文件
            ImageReadDTO templateImageRead = ImageUtils.readTemplateImage();
            //  获取描边图片文件
            ImageReadDTO borderImageRead = ImageUtils.readBorderImageFile();
            //  获取原图文件类型
            String originImageFileType = imageReadDTO.getFileExtension();
            //  获取模板图文件类型
            String templateImageFileType = templateImageRead.getFileExtension();
            //  获取边框图文件类型
            String borderImageFileType = borderImageRead.getFileExtension();
            //  读取原图
            BufferedImage verificationImage = imageReadDTO.getImage();
            //  读取模板图
            BufferedImage readTemplateImage = templateImageRead.getImage();
            //  读取描边图片
            BufferedImage borderImage = borderImageRead.getImage();
            //  获取原图感兴趣区域坐标
            ImageVerificationVo imageVerificationVo = ImageVerificationUtil.generateCutoutCoordinates(verificationImage, readTemplateImage);
            int y = imageVerificationVo.getY();
            //  在分布式应用中，可将session改为redis存储
            String sessionId = imageVerificationDto.getSessionId();
            if (EmptyUtils.isEmpty(sessionId)) {
                throw new CaptchaException("sessionId is null !");
            }
            GuavaCache.VERIFY_CACHE.put(sessionId, imageVerificationVo);
            //  根据原图生成遮罩图和切块图
            imageVerificationVo = ImageVerificationUtil.pictureTemplateCutout(verificationImage, imageReadDTO.getInputStream(), originImageFileType, readTemplateImage, templateImageFileType, imageVerificationVo.getX(), imageVerificationVo.getY());
            //   剪切图描边
            ImageVerificationUtil.cutoutImageEdge(imageVerificationVo, borderImage, borderImageFileType);
            imageVerificationVo.setY(y);
            imageVerificationVo.setType(imageVerificationDto.getType());
            //  =============================================
            //  输出图片
//            HttpServletResponse response = getResponse();
//            response.setContentType("image/jpeg");
//            ServletOutputStream outputStream = response.getOutputStream();
//            outputStream.write(oriCopyImages);
//            BufferedImage bufferedImage = ImageIO.read(originImageFile);
//            ImageIO.write(bufferedImage, originImageType, outputStream);
//            outputStream.flush();
            //  =================================================
            return imageVerificationVo;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CaptchaException(e);
        }
    }

    /**
     * 验证
     *
     * @param x         X轴
     * @param y         Y轴
     * @param sessionId 唯一ID
     * @return boolean
     */
    public static boolean verify(String x, String y, String sessionId) {
        try {
            //阈值
            int threshold = 5;
            ImageVerificationVo imageVerificationVo = GuavaCache.VERIFY_CACHE.get(sessionId);
            boolean b = Math.abs(Integer.parseInt(x) - imageVerificationVo.getX()) <= threshold;
            boolean equals = y.equals(String.valueOf(imageVerificationVo.getY()));
            return b && equals;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }
}
