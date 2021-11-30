package io.github.pierresj.huaweifs;

import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@ManagedResource(description = "Manages Huawei OBS file storage client", objectName = "jmix.Huaweifs:type=HuaweiFileStorage")
@Component("huaweifs_HuaweiFileStorageManagementFacade")
public class HuaweiFileStorageManagementFacade {
    @Autowired
    protected FileStorageLocator fileStorageLocator;

    @ManagedOperation(description = "Refresh Huawei OBS file storage client")
    public String refreshHuaweiOssClient() {
        FileStorage fileStorage = fileStorageLocator.getDefault();
        if (fileStorage instanceof HuaweiFileStorage) {
            ((HuaweiFileStorage) fileStorage).refreshObsClient();
            return "Refreshed successfully";
        }
        return "Not an Huawei file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh Huawei OBS file storage client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "Huawei OBS access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "Huawei OBS secret access key")})
    public String refreshAliOssClient(String storageName, String accessKey, String secretAccessKey) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof HuaweiFileStorage) {
            HuaweiFileStorage HuaweiFileStorage = (HuaweiFileStorage) fileStorage;
            HuaweiFileStorage.setAccessKey(accessKey);
            HuaweiFileStorage.setSecretAccessKey(secretAccessKey);
            HuaweiFileStorage.refreshObsClient();
            return "Refreshed successfully";
        }
        return "Not an Huawei OBS file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh Huawei OBS file storage client by storage name")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "storageName", description = "Storage name"),
            @ManagedOperationParameter(name = "accessKey", description = "Huawei OBS access key"),
            @ManagedOperationParameter(name = "secretAccessKey", description = "Huawei OBS secret access key"),
            @ManagedOperationParameter(name = "bucket", description = "Huawei OBS bucket name"),
            @ManagedOperationParameter(name = "chunkSize", description = "Huawei OBS chunk size (kB)"),
            @ManagedOperationParameter(name = "endpointUrl", description = "Optional custom Huawei OBS storage endpoint URL")})
    public String refreshAliOssClient(String storageName, String accessKey, String secretAccessKey,
                                  String regionName, String bucket, int chunkSize, @Nullable String endpointUrl) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof HuaweiFileStorage) {
            HuaweiFileStorage HuaweiFileStorage = (HuaweiFileStorage) fileStorage;
            HuaweiFileStorage.setAccessKey(accessKey);
            HuaweiFileStorage.setSecretAccessKey(secretAccessKey);
            HuaweiFileStorage.setBucket(bucket);
            HuaweiFileStorage.setChunkSize(chunkSize);
            HuaweiFileStorage.setEndpointUrl(endpointUrl);
            HuaweiFileStorage.refreshObsClient();
            return "Refreshed successfully";
        }
        return "Not an Huawei OBS file storage - refresh attempt ignored";
    }
}
