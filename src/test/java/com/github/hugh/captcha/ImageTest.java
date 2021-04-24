package com.github.hugh.captcha;

import com.github.hugh.captcha.model.dto.ImageVerificationDTO;
import com.github.hugh.captcha.model.vo.ImageVerificationVo;
import com.github.hugh.captcha.util.image.ImageVerificationUtil;
import org.junit.Test;

import java.util.UUID;

public class ImageTest {

    @Test
    public void test01() {
        String s = UUID.randomUUID().toString();
        ImageVerificationDTO dto = new ImageVerificationDTO();
        dto.setSessionId(s);
        ImageVerificationVo imageVerificationVo = ImageVerificationUtil.slideVerificationCode(dto);
        System.out.println("---" + imageVerificationVo);
        boolean verify = ImageVerificationUtil.verify(imageVerificationVo.getX() + "", imageVerificationVo.getY() + "", s);
        System.out.println(verify);
    }
}
