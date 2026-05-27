package com.bcsport.admin.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SpringfoxNpeFix {

    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean.getClass().getName().contains("WebMvcRequestHandlerProvider")
                        || bean.getClass().getName().contains("WebFluxRequestHandlerProvider")) {
                    fixHandlerMappings(bean);
                }
                return bean;
            }

            private void fixHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    if (field == null) return;
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<RequestMappingInfoHandlerMapping> mappings =
                            (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                    List<RequestMappingInfoHandlerMapping> filtered = mappings.stream()
                            .filter(mapping -> {
                                try {
                                    Field pp = ReflectionUtils.findField(mapping.getClass(), "patternParser");
                                    if (pp == null) return true;
                                    pp.setAccessible(true);
                                    return pp.get(mapping) == null;
                                } catch (Exception e) {
                                    return true;
                                }
                            })
                            .collect(Collectors.toList());
                    field.set(bean, filtered);
                } catch (Exception ignored) {
                }
            }
        };
    }
}
