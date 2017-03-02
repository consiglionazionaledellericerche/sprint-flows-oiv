package it.cnr.si.flows.ng.config;

import org.activiti.rest.common.application.ContentTypeResolver;
import org.activiti.rest.common.application.DefaultContentTypeResolver;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(FlowsConfigurations.class)
@ConditionalOnClass(name = {"org.activiti.rest.service.api.RestUrls", "org.springframework.web.servlet.DispatcherServlet"})
public class ActivitiRestApiConfiguration {

    @Bean
    public RestResponseFactory restResponseFactory() {
        RestResponseFactory restResponseFactory = new RestResponseFactory();
        return restResponseFactory;
    }

    @Bean
    public ContentTypeResolver contentTypeResolver() {
        ContentTypeResolver resolver = new DefaultContentTypeResolver();
        return resolver;
    }

    @Configuration
    @ComponentScan(basePackages = {"org.activiti.rest.exception", "org.activiti.rest.service.api"},
            nameGenerator = ActivitiBeanNameGenerator.class

            )
    public static class ComponentScanRestResourcesConfiguration {

        // The component scan cannot be on the root configuration, it would trigger
        // always even if the condition is evaluating to false.
        // Hence, this 'dummy' configuration

    }
}