package com.weightop.config;

import jakarta.validation.MessageInterpolator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Locale;

/**
 * Validation messages are part of the API response, so they are always rendered in English,
 * regardless of the client's Accept-Language header or the JVM default locale.
 */
@Configuration(proxyBeanMethods = false)
public class ValidationConfig {

    @Bean
    public static LocalValidatorFactoryBean defaultValidator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setMessageInterpolator(new FixedLocaleMessageInterpolator(
                new ResourceBundleMessageInterpolator(), Locale.ENGLISH));
        return factory;
    }

    private record FixedLocaleMessageInterpolator(MessageInterpolator delegate, Locale locale)
            implements MessageInterpolator {

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return delegate.interpolate(messageTemplate, context, locale);
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale ignored) {
            return delegate.interpolate(messageTemplate, context, locale);
        }
    }
}
