package com.svetlecic.filesystem.web;

import com.svetlecic.filesystem.FileSystemApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/{version}",
                HandlerTypePredicate.forBasePackageClass(FileSystemApplication.class)
                        .and(HandlerTypePredicate.forAnnotation(RestController.class)));
    }
}
