package com.github.hugh.captcha.util;

import com.github.hugh.captcha.exception.ToolboxException;
import com.github.hugh.captcha.model.dto.ImageReadDTO;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import com.github.hugh.captcha.util.system.OsUtils;

public class ImageUtils {

    /**
     * 读取目标图
     *
     * @return
     */
    public static ImageReadDTO readTargetImage(String path) {
        ImageReadDTO imageRead = new ImageReadDTO();
        try {
            Random random = new Random(System.currentTimeMillis());
            int i = random.nextInt(7);
            InputStream inputStream = getInputStream("/tem/" + i + ".jpg");
            File temp = File.createTempFile("testrunoobtmp", ".jpg");
            System.out.println("文件路径: "+temp.getAbsolutePath());
            toFile(inputStream,temp);
            imageRead.setImage(ImageIO.read(temp));
//            System.out.println("==files[i].getAbsolutePath()=>>" + files[i].getAbsolutePath());
            String extension = temp.getName().substring(temp.getName().lastIndexOf(".") + 1);
            imageRead.setFileExtension(extension);
            imageRead.setInputStream(new FileInputStream(temp));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageRead;
    }

    public static InputStream getInputStream(final String filePath) {
        InputStream inputStream;
        try {
            inputStream = new URL(filePath).openStream();
        } catch (MalformedURLException localMalformedURLException) {
            try {
                inputStream = new FileInputStream(filePath);
            } catch (Exception localException2) {
                ClassLoader localClassLoader = Thread.currentThread().getContextClassLoader();
                if (localClassLoader == null) {
                    localClassLoader = ImageUtils.class.getClassLoader();
                }
                if (OsUtils.isWindows()) {
                    inputStream = localClassLoader.getClass().getResourceAsStream(filePath);
                } else { // linux jar包情况下
                    inputStream = localClassLoader.getResourceAsStream(filePath);
                }
                if (inputStream == null) {
                    throw new ToolboxException("Could not find file: " + filePath);
                }
            }
        } catch (IOException localIOException1) {
            throw new ToolboxException(localIOException1);
        }
        return inputStream;
    }


    public static void toFile(InputStream inputStream, File file) {
        try (OutputStream os = new FileOutputStream(file)) {
            int bytesRead;
            byte[] buffer = new byte[8192];
            while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        File importImage = new File(ClassLoader.getSystemClassLoader().getResource("tem/").getPath());
        if (importImage == null) {
            throw new RuntimeException("not found target image");
        }
        File[] files = importImage.listFiles();
        Random random = new Random(System.currentTimeMillis());
        assert files != null;
        System.out.println(files);
        System.out.println("-->" + files.length);
    }
}
