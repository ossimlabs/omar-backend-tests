package ossim.cucumber.step_definitions

import cucumber.api.Scenario
import ossim.cucumber.config.CucumberConfig
import ossim.cucumber.ogc.imagespace.ImageSpaceCall
import ossim.cucumber.ogc.util.FileCompare

/**
 * Created by kfeldbush on 8/10/16.
 */

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

config = CucumberConfig.config
def s3Bucket = config.s3Bucket
def s3BucketUrl = config.s3BucketUrl
def wfsServer = config.wfsServerProperty
def imageSpaceServer = config.imageSpaceServerProperty
def imageSpaceReturnImage
Scenario scenario

def getImageId(format, index, platform, sensor)
{
    format = format.toLowerCase()
    platform = platform.toLowerCase()
    sensor = sensor.toLowerCase()


    return config.images[platform][sensor][format][index == "another" ? 1 : 0]
}

Before(){ theScenario ->
    scenario = theScenario
}

When(~/^a call is made to ImageSpace for a (.*) of the entire bounding box of (.*) (.*) (.*) (.*) image$/) {
    String imageType, String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def imageSpaceCall = new ImageSpaceCall()
        imageSpaceReturnImage = imageSpaceCall.getImage(imageSpaceServer, wfsServer, imageId, "256", imageType, "3", "0", "3")
}

Then(~/^ImageSpace returns a (.*) that matches the validation of (.*) (.*) (.*) ([^_]*)_?(.*) image$/) {
    String imageType, String index, String platform, String sensor, String format, String extension ->

        def imageId = getImageId(format, index, platform, sensor)

        imageId = extension ? imageId += "_${extension}" : imageId

        verificationImageUrl = new URL("${s3BucketUrl}/${s3Bucket}/ImageSpace_verification_images/${imageId}.${imageType}")
        def fileComp = new FileCompare()
        assert fileComp.checkImages(verificationImageUrl, imageSpaceReturnImage, imageType)
}

When(~/^a call is made to ImageSpace for a (.*) single tile overview of (.*) (.*) (.*) (.*) image$/) {
    String imageType, String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def imageSpaceCall = new ImageSpaceCall()
        imageSpaceReturnImage = imageSpaceCall.getImage(imageSpaceServer, wfsServer, imageId, "256", imageType, "0", "0", "0")
}

When(~/^a call is made to ImageSpace with a time limit of (\d+) to get a (.*) thumbnail of (.*) (.*) (.*) (.*) image$/) {
    String timeLimitInMillis, String imageType, String index, String platform, String sensor, String format ->
        def startTime = System.currentTimeMillis()
        def imageId = getImageId(format, index, platform, sensor)
        int timeLimit = timeLimitInMillis.toInteger()

        def imageSpaceCall = new ImageSpaceCall()
        imageSpaceReturnImage = imageSpaceCall.getThumbnail(imageSpaceServer, wfsServer, imageId, "256", imageType)

        def durationInMillis = System.currentTimeMillis() - startTime
        scenario.write("Time elapsed ${durationInMillis / 1000}s [$imageType, $index, $platform, $sensor, $format]")
        if (timeLimit > 0) {
            assert (durationInMillis < timeLimit)
        }
}

When(~/^a call is made to ImageSpace for an overview tile in red green blue order of (.*) (.*) (.*) (.*) image$/) {
    String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def imageSpaceCall = new ImageSpaceCall()
        imageSpaceReturnImage = imageSpaceCall.getImage(imageSpaceServer, wfsServer, imageId, "256", "png", "0", "0", "0", "3,2,1", "3")
}
When(~/^a call is made to ImageSpace for an overview tile in green blue red order of (.*) (.*) (.*) (.*) image$/) {
    String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def imageSpaceCall = new ImageSpaceCall()
        imageSpaceReturnImage = imageSpaceCall.getImage(imageSpaceServer, wfsServer, imageId, "256", "png", "0", "0", "0", "2,1,3", "3")
}
When(~/^a call is made to ImageSpace for an overview tile green band of (.*) (.*) (.*) (.*) image$/) {
    String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        def imageSpaceCall = new ImageSpaceCall()
        imageSpaceReturnImage = imageSpaceCall.getImage(imageSpaceServer, wfsServer, imageId, "256", "png", "0", "0", "0", "2", "1")
}
//When(~/^a call is made to ImageSpace for an image's thumbnail of image id (.*)$/) {
//    String imageId ->
//        def imageSpaceCall = new ImageSpaceCall()
//        imageSpaceReturnImage = imageSpaceCall.getThumbnail(imageSpaceServer, wfsServer, imageId, imageId, "png")
//}
//Then(~/^ImageSpace returns a thumbnail of format (.*) with size (\\d+)$/) {
//    String imageFormat, int size ->
//        assert (imageFormat == "png")
//        assert (size == 256)
//}
