package ossim.cucumber.step_definitions

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ossim.cucumber.config.CucumberConfig
import ossim.cucumber.ogc.wfs.WFSCall

import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

String defaultCharset = Charset.defaultCharset().displayName()

def httpResponse

config = CucumberConfig.config
def downloadService = config.downloadService
def stagingService = config.stagingService
def wfsServer = config.wfsServerProperty


def getImageId(format, index, platform, sensor) {
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

Then(~/^(.*) (.*) (.*) (.*) image is downloaded along with supporting zip file$/) {
    String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def filter = "filename LIKE '%${imageId}%'"
        def wfsQuery = new WFSCall(wfsServer, filter, "JSON", 1)
        def features = wfsQuery.result.features

        def rasterFilesUrl = stagingService + "/getRasterFiles?id=${features[0].properties.id}"
        def rasterFilesText = new URL(rasterFilesUrl).getText()
        def rasterFiles = new JsonSlurper().parseText(rasterFilesText).results

        def zipFile = new File("${imageId}.zip")
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
        "rm -f ${imageId}.zip".execute()

        assert true
}

Then(~/^the response should return a status of (\d+) and a message of "(.*)"$/) { int statusCode, String message ->
    println httpResponse


    assert httpResponse.status == statusCode && httpResponse.message == message
}

When(~/^the download service is called to download (.*) (.*) (.*) (.*) image as a zip file$/) {
    String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def filter = "filename LIKE '%${imageId}%'"
        def wfsQuery = new WFSCall(wfsServer, filter, "JSON", 1)
        def features = wfsQuery.result.features

        // get all the supporting files
        def rasterFilesUrl = stagingService + "/getRasterFiles?id=${features[0].properties.id}"
        def rasterFilesText = new URL(rasterFilesUrl).getText()
        def rasterFiles = new JsonSlurper().parseText(rasterFilesText).results

        // formulate the post data
        def filename = "${imageId}.zip"
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
        def command = ["curl", "-L", "-o", "${filename}", "-d", "fileInfo=${URLEncoder.encode(jsonPost, defaultCharset)}", "${downloadService}/archive/download"]
        println command
        def process = command.execute()
        process.waitFor()


        assert new File("${imageId}.zip").exists()
}

When(~/^the download service is called with no fileGroups specified in the json$/) { ->
    def map = [
            type          : "Download",
            zipFileName   : "",
            archiveOptions: [type: "zip"],
            fileGroups    : []
    ]
    def jsonPost = JsonOutput.toJson(map)

    def command = ["curl", "-L", "-d", "fileInfo=${URLEncoder.encode(jsonPost, defaultCharset)}", "${downloadService}/archive/download"]
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

    def command = ["curl", "-L", "-d", "fileInfo=${URLEncoder.encode(jsonPost, defaultCharset)}", "${downloadService}/archive/download"]
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
    def command = ["curl", "-L", "-d", "fileInfo=", "${downloadService}/archive/download"]
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

When(~/^we download a local hsi envi image$/) { ->

    String filename = config.images.local.hsi.envi
    def filter = "filename = '${filename}'"
    println "Finding file with filter ${filter}"
    def wfsQuery = new WFSCall(wfsServer, filter, "JSON", 1)
    def features = wfsQuery.result.features

    println "DOWNLOADING FILES FOR filename = ${filename}"
    // get all the supporting files
    def rasterFilesUrl = stagingService + "/getRasterFiles?id=${features[0].properties.id}"
    def rasterFilesText = new URL(rasterFilesUrl).getText()
    def rasterFiles = new JsonSlurper().parseText(rasterFilesText).results

    // formulate the post data
    File zipFilename = "localHsiEnvi.zip" as File
    def map = [
            type          : "Download",
            zipFileName   : zipFilename.toString(),
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
    def command = ["curl", "-L", "-o", "${zipFilename}", "-d", "fileInfo=${URLEncoder.encode(jsonPost, defaultCharset)}", "${downloadService}/archive/download"]
    println command
    def process = command.execute()
    process.waitFor()

    assert zipFilename.exists() == true

}
Then(~/^the hsi should contain the proper files$/) { ->

//    ZipInputStream in
    File file = new File("localHsiEnvi.zip")
    Boolean foundHsi = false;
    Boolean foundHdr = false;
    if (file.exists()) {
        FileInputStream input = new FileInputStream(file);
        ZipInputStream zip = new ZipInputStream(input);
        ZipEntry entry;
        while ((entry = zip.nextEntry) != null) {
            String name = entry.name
            if (name.endsWith(".hsi")) {
                foundHsi = true
            } else if (name.endsWith("hsi.hdr")) {
                foundHdr = true
            }
        }

        zip.close();
        input.close()
    }

    assert ((foundHsi && foundHdr) == true)
}
