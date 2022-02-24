import io.jmix.core.CoreConfiguration
import io.jmix.core.FileRef
import io.jmix.core.FileStorage
import io.jmix.core.UuidProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import test_support.HuaweiFileStorageTestConfiguration
import test_support.TestContextInititalizer
import io.github.pierresj.huaweifs.HuaweiFileStorageConfiguration

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ContextConfiguration(
        classes = [CoreConfiguration, HuaweiFileStorageConfiguration,HuaweiFileStorageTestConfiguration],
        initializers = [TestContextInititalizer]
)
class HuaweiFileStorageTest extends Specification {

    @Autowired
    private FileStorage fileStorage


    def "save stream"(){
        def fileName=UuidProvider.createUuid().toString()+".txt";
        def fileStream=this.getClass().getClassLoader().getResourceAsStream("files/simple.txt");
        def fileRef=fileStorage.saveStream(fileName,fileStream);
        def openedStream=fileStorage.openStream(fileRef);
        expect:
            openedStream!=null
    }

    def "openStream"(){
        def fileKey = "2022/02/24/449321b3-7dd5-c05c-1d97-d4780528f624.txt"
        def fileName="449321b3-7dd5-c05c-1d97-d4780528f624.txt"
        def storageName = fileStorage.getStorageName()
        def fileRef = new FileRef(storageName, fileKey, fileName)
        fileStorage.openStream(fileRef)

        expect: true
    }

    def "fileExists"() {
        def storageName = fileStorage.getStorageName()
        def fileKey = "2022/02/24/449321b3-7dd5-c05c-1d97-d4780528f624.txt"
        def fileName="449321b3-7dd5-c05c-1d97-d4780528f624.txt"

        def fileref = new FileRef(storageName, fileKey, fileName)
        def exists = fileStorage.fileExists(fileref)

        expect:  exists

    }


    def "removeFile"(){
        def storageName = fileStorage.getStorageName()
        def fileKey = "2022/02/24/2480b1c7-c5fc-c212-3ec4-03d2d6351b90.txt "
        def fileName="2480b1c7-c5fc-c212-3ec4-03d2d6351b90.txt "

        def fileref = new FileRef(storageName, fileKey, fileName);
        fileStorage.removeFile(fileref)

        def exists = fileStorage.fileExists(fileref)

        expect:  !exists
    }

}