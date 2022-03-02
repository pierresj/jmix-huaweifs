package io.github.pierresj.huaweifs.autoconfigure;

import io.github.pierresj.huaweifs.HuaweiFileStorageConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({HuaweiFileStorageConfiguration.class})
public class HuaweifsAutoConfiguration {
}

