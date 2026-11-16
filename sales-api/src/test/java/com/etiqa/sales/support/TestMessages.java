package com.etiqa.sales.support;

import com.etiqa.sales.common.i18n.Messages;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public final class TestMessages {

    private TestMessages() {
    }

    public static Messages create() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return new Messages(messageSource);
    }
}
