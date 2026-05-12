package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OssConfiguration {

    @Bean
    @ConditionalOnMissingBean(AliOssUtil.class)   //避免重复创建 -- 创建了就不创建了
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        //AliOssUtil 中需要用到阿里云的四个参数来上传路径参数 用 AliOssProperties 封装
        log.info("开始创建阿里云文件上传工具类对象，{}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }

}
