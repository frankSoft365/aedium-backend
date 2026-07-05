package com.microsoft.aediumbackend.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AliyunOSSOperator {
    // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    // 填写Bucket名称，例如examplebucket。
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
    @Value("${aliyun.oss.region}")
    private String region;

    /**
     * 上传文件 返回文件路径
     */
    public String upload(byte[] content, String objectName) throws Exception {

        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        // 修饰文件名 时间 + UUID + 文件格式
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = objectName.substring(objectName.indexOf("."));
        String newObjectName = date + "/" + UUID.randomUUID() + extension;

        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);        
        OSS ossClient = OSSClientBuilder.create()
        .endpoint(endpoint)
        .credentialsProvider(credentialsProvider)
        .clientConfiguration(clientBuilderConfiguration)
        .region(region)               
        .build();

        try {
            // 填写Byte数组。

            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, newObjectName, new ByteArrayInputStream(content));
            
            // 创建PutObject请求。
            PutObjectResult result = ossClient.putObject(putObjectRequest);            
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        // 返回的url是可以预知的 就是原本endpoint子路径下的bucketName 其下的具体文件名
        // 示例：https://java-web-frank.oss-cn-beijing.aliyuncs.com/2026/01/03f462ce-2b8e-43a8-b481-504e11595756.jpg
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + newObjectName;
    }
}