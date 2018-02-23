package ossim.cucumber.step_definitions

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ossim.cucumber.config.CucumberConfig
import ossim.cucumber.ogc.util.FileCompare
import ossim.cucumber.ogc.wfs.WFSCall

import java.nio.charset.Charset

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

String defaultCharset = Charset.defaultCharset().displayName()

def httpResponse

config = CucumberConfig.config
def downloadService = config.downloadService
def stagingService = config.stagingService
def wfsServer = config.wfsServerProperty
def s3Bucket = config.s3Bucket
def s3BucketUrl = config.s3BucketUrl

String getImageId(String index = "a", String format, String platform, String sensor) {
    format = format.toLowerCase()
    platform = platform.toLowerCase()
    sensor = sensor.toLowerCase()
    return config.images[platform][sensor][format][index == "another" ? 1 : 0]
}

Given(~/^that the download service is running$/) { ->
    def healthText = new URL("${downloadService}/health").text
    def healthJson = new JsonSlurper().parseText(healthText)
    assert healthJson.status == "UP"
}

When(~/^we download (.*) (.*) (.*) (.*) image$/) {
    String index, String platform, String sensor, String format ->
        println "we download image: $index $platform $sensor $format"
        def imageFileName = validFileName(getImageId(index, format, platform, sensor))
        assert imageFileName != null && imageFileName != "/"
        def feature = fetchWfsFeaturesForImageId(imageFileName)
        assert feature != null
        String rasterFiles = fetchSupportingFilesForFeature(feature)
        def downloadRequestOptions = getPostDataForDownloadRequest(imageFileName, rasterFiles)
        downloadImageFile(imageFileName, downloadRequestOptions)
}

Then(~/^(.*) (.*) (.*) (.*) image is downloaded along with supporting zip file$/) {
    String index, String platform, String sensor, String format ->
        def imageId = getImageId(index, format, platform, sensor)

        def filter = "filename LIKE '%${imageId}%'"
        def wfsQuery = new WFSCall(wfsServer, filter, "JSON", 1)
        def features = wfsQuery.result.features

        def rasterFilesUrl = stagingService + "/getRasterFiles?id=${features[0].properties.id}"
        def rasterFilesText = new URL(rasterFilesUrl).getText()
        def rasterFiles = new JsonSlurper().parseText(rasterFilesText).results

        def zipFile = new File("${imageId}")
        if (zipFile.exists()) {
            def command = "unzip -l ${zipFile}"
            def process = command.execute()
            def files = process.getText()
            println files
            rasterFiles.each {
                def file = new File(it)
                if (!files.contains(file.name)) {
                    assert files.contains(file.name)
                }
            }
        } else {
            assert zipFile.exists()
        }

        // clean up
        "rm -f ${imageId}".execute()

        assert true
}

Then(~/^the response should return a status of (\d+) and a message of "(.*)"$/) { int statusCode, String message ->
    println httpResponse


    assert httpResponse.status == statusCode && httpResponse.message == message
}

When(~/^the download service is called to download (.*) (.*) (.*) (.*) image as a zip file$/) {
    String index, String platform, String sensor, String format ->

        def imageId = getImageId(index, format, platform, sensor)

        def filter = "filename LIKE '%${imageId}%'"
        def wfsQuery = new WFSCall(wfsServer, filter, "JSON", 1)
        def features = wfsQuery.result.features

        // get all the supporting files
        def rasterFilesUrl = stagingService + "/getRasterFiles?id=${features[0].properties.id}"
        def rasterFilesText = new URL(rasterFilesUrl).getText()
        def rasterFiles = new JsonSlurper().parseText(rasterFilesText).results

        // formulate the post data
        def filename = "${imageId}"
        def map = [
                type          : "Download",
                zipFileName   : filename,
                archiveOptions: [type: "zip"],
                fileGroups    : [
                        [
                                rootDirectory: "",
                                files        : rasterFiles
                        ]
                ]
        ]
        def jsonPost = JsonOutput.toJson(map)

        // download the file
        def command = curlDownloadCommand(filename, jsonPost)
        println command
        def process = command.execute()
        process.waitFor()


        assert new File("${imageId}").exists()
}

When(~/^the download service is called with no fileGroups specified in the json$/) { ->
    def map = [
            type          : "Download",
            zipFileName   : "",
            archiveOptions: [type: "zip"],
            fileGroups    : []
    ]
    def jsonPost = JsonOutput.toJson(map)

    def command = curlDownloadCommand(null, jsonPost)
    println command

    def stdOut = new StringBuilder()
    def stdError = new StringBuilder()
    def process = command.execute()
    process.consumeProcessOutput(stdOut, stdError)
    process.waitFor()
    println stdOut.toString()

    httpResponse = new JsonSlurper().parseText(stdOut.toString())


    assert httpResponse != null
}

When(~/^the download service is called with the wrong archive type$/) { ->
    def map = [
            type          : "Download",
            zipFileName   : "",
            archiveOptions: [type: ""],
            fileGroups    : [
                    [
                            rootDirectory: "",
                            files        : ["", ""]
                    ]
            ]
    ]
    def jsonPost = JsonOutput.toJson(map)

    def command = curlDownloadCommand(null, jsonPost)
    println command

    def stdOut = new StringBuilder()
    def stdError = new StringBuilder()
    def process = command.execute()
    process.consumeProcessOutput(stdOut, stdError)
    process.waitFor()
    println stdOut.toString()

    httpResponse = new JsonSlurper().parseText(stdOut.toString())


    assert httpResponse != null
}

When(~/^the download service is called without a json message$/) { ->
    def command = curlDownloadCommand(null, "")
    println command

    def stdOut = new StringBuilder()
    def stdError = new StringBuilder()
    def process = command.execute()
    process.consumeProcessOutput(stdOut, stdError)
    process.waitFor()
    println stdOut.toString()

    httpResponse = new JsonSlurper().parseText(stdOut.toString())

    assert httpResponse != null
}

Then(~/^a file of (.*) (.*) (.*) (.*) image should exist$/) {
    String index, String platform, String sensor, String format ->
        String imageFileName = validFileName(getImageId(index, format, platform, sensor))
        assert imageFileName != null && imageFileName != "/"
        File imageFile = new File(imageFileName)
        assert imageFile.exists()
        println "Image file: $imageFile"
}

Then(~/^a file of (.*) (.*) (.*) (.*) matches the validation of S3 file (.*)/) {
    String index, String platform, String sensor, String format, String s3Path ->
        def imageFileName = validFileName(getImageId(index, format, platform, sensor))
        URL verificationImageUrl = new URL("${s3BucketUrl}/$s3Path")
        FileCompare fileComp = new FileCompare()
        assert fileComp.checkImages(verificationImageUrl, imageFileName)
}

Then(~/^a file of (.*) (.*) (.*) (.*) image should contain image files/) { ->

}

def fetchWfsFeaturesForImageId(String imageId) {
    String geoscriptFilter = "filename LIKE '%${imageId}%'"
    def wfsQuery = new WFSCall(config.wfsServerProperty, geoscriptFilter, "JSON", 1)
    return wfsQuery.result.features
}

def fetchWfsFeaturesForFileName(String fileName) {
    String geoscriptFilter = "filename = $fileName"
    def wfsQuery = new WFSCall(config.wfsServerProperty, geoscriptFilter, "JSON", 1)
    return wfsQuery.result.features
}

def fetchSupportingFilesForFeature(def feature) {
    def rasterFilesUrl = config.stagingService + "/getRasterFiles?id=${feature["properties"]["id"][0]}"
    def rasterFilesText = new URL(rasterFilesUrl).getText()
    return new JsonSlurper().parseText(rasterFilesText).results
}

String getPostDataForDownloadRequest(String imageFileName, String rasterFiles) {
    return JsonOutput.toJson([
            type          : "Download",
            zipFileName   : imageFileName+".zip",
            archiveOptions: [type: "zip"],
            fileGroups    : [
                    [
                            rootDirectory: "",
                            files        : rasterFiles
                    ]
            ]
    ])
}

List<String> curlDownloadCommand(String fileName = null, String fileInfo = null) {
    List<String> command = ["curl", "-L", "${config.downloadService}/archive/download"]

    // Callers may want to output to stdout.
    if (fileName != null) command.addAll(1, ["-o", "${validFileName(fileName)}"])

    // An empty string for 'fileInfo' is invalid but is used in tests.
    if (fileInfo != null) command.addAll(1,
            ["-d", "fileInfo=${URLEncoder.encode(fileInfo, Charset.defaultCharset().displayName())}"]
    )

    // Necessary to optionally support authentication
    if (config.curlOptions) command.addAll(1, config.curlOptions)
    return command
}

String validFileName(String imageId) {
    return imageId.replace('/', '_').replace('\\', '_')
}

void downloadImageFile(String imageFileName, String fileInfo) {
    def command = curlDownloadCommand(imageFileName, fileInfo)

    def stdOut = new StringBuilder()
    def stdErr = new StringBuilder()
    def process = command.execute()
    process.consumeProcessOutput(stdOut, stdErr)
    process.waitFor()
}

//// Used #7
//Then(~/^the hsi should contain the proper files$/) { ->
//
////    ZipInputStream in
//    File file = new File("/data/hsi/2012-06-11/AM/ALPHA/2012-06-11_18-20-11/HSI/Scan_00007/2012-06-11_18-20-11.HSI.Scan_00007.scene.corrected.hsi.zip")
//    Boolean foundHsi = false;
//    Boolean foundHdr = false;
//    if (file.exists()) {
//        FileInputStream input = new FileInputStream(file);
//        ZipInputStream zip = new ZipInputStream(input);
//        ZipEntry entry;
//        while ((entry = zip.nextEntry) != null) {
//            String name = entry.name
//            if (name.endsWith(".hsi")) {
//                foundHsi = true
//            } else if (name.endsWith("hsi.hdr")) {
//                foundHdr = true
//            }
//        }
//
//        zip.close();
//        input.close()
//    }
//
//    assert ((foundHsi && foundHdr) == true)
//}
