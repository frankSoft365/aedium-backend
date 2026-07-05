package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.utils.AliyunOSSOperator;
import com.microsoft.aediumbackend.utils.AvatarUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;


@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {
    @Resource
    private AvatarUtils avatarUtils;
    @Resource
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 上传头像-压缩
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(MultipartFile avatar) {
        avatarUtils.verifyAvatar(avatar);
        String url = avatarUtils.compressAndUploadAvatar(avatar);
        return Result.success(url);
    }

    /**
     * 无损上传图片
     */
    @PostMapping("/image")
    public Result<String> uploadImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, FILE_EMPTY);
        }

        try {
            String originalFilename = image.getOriginalFilename();
            if (originalFilename == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, FILENAME_EMPTY);
            }

            String url = aliyunOSSOperator.upload(image.getBytes(), originalFilename);
            return Result.success(url);
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, FILE_UPLOAD_ERROR);
        }
    }
}
