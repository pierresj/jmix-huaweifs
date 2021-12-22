package io.github.pierresj.huaweifs;

import com.obs.services.ObsClient;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;
import io.jmix.core.FileStorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class PartUploader implements Runnable {

    private final ObsClient client;
    private final List<PartEtag> partEtags;
    private final String bucketName;
    private final String objectName; //文件名
    private final byte[] data;
    private final long partSize;
    private final int partNumber;
    private final String uploadId;
    private final long offset;


    public PartUploader(ObsClient client,
                        List<PartEtag> partEtags,
                        byte[] chunkedData,
                        String objectName,
                        String bucketName,
                        long partSize,
                        int partNumber,
                        String uploadId,
                        long offset) {
        this.data       = chunkedData;
        this.client     = client;
        this.partEtags  = partEtags;
        this.partSize   = partSize;
        this.bucketName = bucketName;
        this.partNumber = partNumber;
        this.uploadId   = uploadId;
        this.objectName = objectName;
        this.offset     = offset;
    }

    @Override
    public void run() {
        InputStream instream = null;
        try {

            instream = new ByteArrayInputStream(this.data);
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setObjectKey(objectName);
            uploadPartRequest.setUploadId(this.uploadId);
            uploadPartRequest.setInput(instream);
            uploadPartRequest.setPartSize(this.partSize);
            uploadPartRequest.setPartNumber(this.partNumber);
            uploadPartRequest.setOffset(offset);

            UploadPartResult uploadPartResult = client.uploadPart(uploadPartRequest);
            synchronized (partEtags) {
                partEtags.add(new PartEtag(uploadPartResult.getEtag(), uploadPartResult.getPartNumber()));
            }
        } catch (Exception e) {
          throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,"uploading a part of data failed",e);
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }





}
