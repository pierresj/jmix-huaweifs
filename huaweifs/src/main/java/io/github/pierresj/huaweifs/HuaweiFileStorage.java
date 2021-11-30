package io.github.pierresj.huaweifs;

import com.obs.services.ObsClient;
import com.obs.services.model.*;
import io.jmix.core.*;
import io.jmix.core.annotation.Internal;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Internal
@Component("huaweifs_FileStorage")
public class HuaweiFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(HuaweiFileStorage.class);
    public static final String DEFAULT_STORAGE_NAME = "HuaweiObs";

    protected String storageName;

    @Autowired
    protected HuaweiFileStorageProperties properties;

    boolean useConfigurationProperties = true;
    protected String accessKey;
    protected String secretAccessKey;
    protected String bucket;
    protected int chunkSize;
    protected String endpointUrl;

    @Autowired
    protected TimeSource timeSource;

    protected AtomicReference<ObsClient> clientReference = new AtomicReference<>();

    public HuaweiFileStorage() {
        this(DEFAULT_STORAGE_NAME);
    }

    public HuaweiFileStorage(String storageName) {
        this.storageName = storageName;
    }

    /**
     * Optional constructor that allows you to override {@link HuaweiFileStorageProperties}.
     */
    public HuaweiFileStorage(String storageName,
                            String accessKey,
                            String secretAccessKey,
                            String bucket,
                            int chunkSize,
                            @Nullable String endpointUrl,
                            String regionName) {
        this.useConfigurationProperties = false;
        this.storageName = storageName;
        this.accessKey = accessKey;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.chunkSize = chunkSize;
        this.endpointUrl = endpointUrl;
    }

    @EventListener
    public void initOssClient(ApplicationStartedEvent event) {
        refreshObsClient();
    }

    protected void refreshProperties() {
        if (useConfigurationProperties) {
            this.accessKey = properties.getAccessKey();
            this.secretAccessKey = properties.getSecretAccessKey();
            this.bucket = properties.getBucket();
            this.chunkSize = properties.getChunkSize();
            this.endpointUrl = properties.getEndpointUrl();
        }
    }

    public void refreshObsClient() {
        refreshProperties();
        ObsClient obsClient = new ObsClient(accessKey, secretAccessKey, endpointUrl);
        clientReference.set(obsClient);
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    protected String createFileKey(String fileName) {
        return createDateDir() + "/" + createUuidFilename(fileName);
    }

    protected String createDateDir() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timeSource.currentTimestamp());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d/%s/%s", year,
                StringUtils.leftPad(String.valueOf(month), 2, '0'),
                StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    protected String createUuidFilename(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isNotEmpty(extension)) {
            return UuidProvider.createUuid().toString() + "." + extension;
        } else {
            return UuidProvider.createUuid().toString();
        }
    }
    private void completeMultipartUpload(List<PartEtag> partEtags, String objectName, String uploadId) {
        // Make part numbers in ascending order
        Collections.sort(partEtags, new Comparator<PartEtag>() {
            @Override
            public int compare(PartEtag p1, PartEtag p2) {
                return p1.getPartNumber() - p2.getPartNumber();
            }
        });
        log.info("Completing to upload multiparts\n");
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, objectName, uploadId, partEtags);
        clientReference.get().completeMultipartUpload(completeMultipartUploadRequest);
    }

    private void listAllParts(String objectName, String uploadId) {
        log.debug("Listing all parts......");
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucket, objectName, uploadId);
        ListPartsResult partListing = clientReference.get().listParts(listPartsRequest);
        int partCount = partListing.getMultipartList().size();
        for (int i = 0; i < partCount; i++) {
            Multipart partSummary = partListing.getMultipartList().get(i);
            log.debug("\tPart#" + partSummary.getPartNumber() + ", ETag=" + partSummary.getEtag());
        }
        log.debug("\n");
    }
    @Override
    public FileRef saveStream(String fileName, InputStream inputStream) {
        String fileKey = createFileKey(fileName);
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            ObsClient client = clientReference.get();

            // 初始化线程池
            ExecutorService executorService = Executors.newFixedThreadPool(20);

            // 初始化分段上传任务
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, fileKey);
            InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);

            List<PartEtag> partEtags = new ArrayList<>();

            String uploadId = result.getUploadId();

            // 每段上传100MB
            int chunkSizeBytes = this.chunkSize * 1024;
            int partCount = 0;
            for (int i = 0; i * chunkSizeBytes < data.length; i++) {
                partCount++;
                int partNumber = i + 1;
                int endChunkPosition = Math.min(partNumber * chunkSizeBytes, data.length);
                byte[] chunkBytes = getChunkBytes(data, i * chunkSizeBytes, endChunkPosition);
                // 分段在文件中的起始位置
                long offset = i * chunkSizeBytes;

                PartUploader partUploader = new PartUploader(client, partEtags, chunkBytes, fileKey, bucket, chunkBytes.length, partNumber, uploadId, offset);
                executorService.execute(partUploader);
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                try {
                    executorService.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, "Uploading file to Ali OSS failed", e);
                } finally {
                    executorService.shutdownNow();
                }
            }
            if (partEtags.size() != partCount) {
                throw new IllegalStateException("Upload multiparts fail due to some parts are not finished yet");
            } else {
                log.info("Succeed to complete multiparts into an object named " + fileKey + "\n");
            }

            listAllParts(fileKey, uploadId);
            completeMultipartUpload(partEtags, fileKey, uploadId);
            return new FileRef(getStorageName(), fileKey, fileName);
        } catch (IOException e) {
            String message = String.format("Could not save file %s.", fileName);
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    protected byte[] getChunkBytes(byte[] data, int start, int end) {
        byte[] chunkBytes = new byte[end - start];
        System.arraycopy(data, start, chunkBytes, 0, end - start);
        return chunkBytes;
    }

    @Override
    public InputStream openStream(FileRef reference) {
        try{
            ObsClient client = clientReference.get();
            ObsObject object = client.getObject(bucket, reference.getPath());
            return object.getObjectContent();
        } catch (Exception e) {
            String message = String.format("Could not load file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public void removeFile(FileRef reference) {
        ObsClient client = clientReference.get();
        client.deleteObject(bucket, reference.getPath());
    }

    @Override
    public boolean fileExists(FileRef reference) {
        ObsClient client = clientReference.get();
        ListObjectsRequest request = new ListObjectsRequest(bucket);//根据前缀获取对象
        request.setMaxKeys(1);
        request.setPrefix(reference.getPath());
        ObjectListing result = client.listObjects(request);
        return result.getObjects().size() > 0;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setEndpointUrl(@Nullable String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
