package com.microsoft.aediumbackend.utils;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class AvatarUtils {
    @Value("${avatar.upload.max-width}")
    private Integer maxWidth;
    @Value("${avatar.upload.max-height}")
    private Integer maxHeight;
    @Value("${avatar.upload.quality}")
    private Float quality;

    @Resource
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 没有返回值校验图片只看抛异常
     */
    public void verifyAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传头像为空！");
        }
        // 校验大小
        long size = file.getSize();
        // 不能超过5MB
        if (size > 5 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像文件大小不能超过5MB");
        }
        // 校验后缀
        // 拿到文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名为空！");
        }
        String fileExtension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        // 校验内容是否是图片
        try {
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "上传的文件不是有效图片文件！");
            }
        } catch (IOException e) {
            log.error("图片IO异常（校验文件是否为图片）：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片校验失败！");
        }
    }

    /**
     * 图片压缩并上传阿里云
     */
    public String compressAndUploadAvatar(MultipartFile file) {
        // 文件压缩
        ByteArrayOutputStream outputStream;
        try {
            outputStream = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(maxWidth, maxHeight) // 最大尺寸，等比例缩放
                    .outputQuality(quality) // 质量压缩
                    .toOutputStream(outputStream);      // 写入字节输出流
        } catch (IOException e) {
            log.error("图片IO异常（图片压缩）：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败！");
        }
        // 文件上传
        String url;
        try {
            url = aliyunOSSOperator.upload(outputStream.toByteArray(), Objects.requireNonNull(file.getOriginalFilename()));
        } catch (Exception e) {
            log.error("图片上传OSS失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败！");
        }
        return url;
    }
}
