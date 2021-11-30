package io.github.pierresj.huaweifs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "stone.huaweifs")
@ConstructorBinding
public class HuaweiFileStorageProperties {
    String accessKey;
    String secretAccessKey;
    String bucket;
    int chunkSize;
    String endpointUrl;

    public HuaweiFileStorageProperties(
            String accessKey,
            String secretAccessKey,
            String bucket,
            @DefaultValue("8192") int chunkSize,
            @DefaultValue("") String endpointUrl) {
        this.accessKey = accessKey;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
        this.endpointUrl = endpointUrl;
    }

    /**
     * Huawei OBS access key.
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Huawei OBS secret access key.
     */
    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    /**
     * Huawei OBS bucket name.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     *  chunk size (kB).
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Return Huawei OBS storage endpoint URL.
     */
    public String getEndpointUrl() {
        return endpointUrl;
    }
}
