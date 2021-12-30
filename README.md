# Jmix Huawei OBS File Storage

用于华为云对象存储

## 安装

| Jmix Version | Add-on Version | Implementation |
|:-|:-|:-|
| 1.1.* |0.0.3|io.github.pierresj:jmix-huaweifs-starter:0.0.3-RELEASE|

### build.gradle
```
implementation 'io.github.pierresj:jmix-huaweifs-starter:0.0.3-RELEASE'
```
### 配置参数(以properties文件为例)
```properties
jmix.core.defaultFileStorage=huawei_fs

jmix.huaweifs.accessKey=your accessKey
jmix.huaweifs.secretAccessKey=your secretAccessKey}
jmix.huaweifs.bucket=your bucket
jmix.huaweifs.chunkSize=${huaweifs.chunkSize}
jmix.huaweifs.endpointUrl=${huaweifs.endpointUrl}
```
### 例子
```
@Autowired
private FileStorage fileStorage;

File file = temporaryStorage.getFile(manuallyControlledField.getFileId());
InputStream fileInputStream = new FileInputStream(file);        
FileRef fileRef = fileStorage.saveStream(event.getFileName(), fileInputStream);
```