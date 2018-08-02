package ossim.cucumber.step_definitions

import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import groovy.util.slurpersupport.GPathResult
import ossim.cucumber.config.CucumberConfig
import ossim.cucumber.ogc.util.FileCompare
import ossim.cucumber.ogc.wcs.WCSCall

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

config = CucumberConfig.config
def wcsServer = config.wcsServerProperty
def wfsServer = config.wfsServerProperty
def s3Bucket = config.s3Bucket
def s3BucketUrl = config.s3BucketUrl
def s3WcsVerificationFiles = config.s3WcsVerificationFiles

def serverVersion
def coverage = "omar:raster_entry"
def currentBbox = ""

WCSCall wcsCall
GPathResult wcsGetCapabilitiesResult
GPathResult wcsDescribeCoverageResult
URL wcsGetCoverageURL
URL verificationImageURL

def getImageId(format, index, platform, sensor)
{
    format = format.toLowerCase()
    platform = platform.toLowerCase()
    sensor = sensor.toLowerCase()

    config.images[platform][sensor][format][index == "another" ? 1 : 0]
}

def filterUsingImageId(imageId)
{
    "entry_id='0' and title LIKE '%${imageId}%'"
}


Before('@web_coverage_service') {
    wcsCall = new WCSCall()
}

When(~/^a user calls WCS GetCapabilities version (.*) for (.*) (.*) (.*) (.*) image$/) {
    String version, String index, String platform, String sensor, String format ->

        imageId = getImageId(format, index, platform, sensor)
        println("testing WCS GetCapabilities call on ${imageId}")
        def filter = filterUsingImageId(imageId)
        serverVersion = version

        wcsGetCapabilitiesResult = wcsCall.getCapabilities(wcsServer, serverVersion, coverage, filter)

}

Then(~/^the WCS service responds with a correct GetCapabilities statement$/) { ->
    String s3VerificationResultURL = "${s3BucketUrl}/${s3Bucket}/${s3WcsVerificationFiles}/wcsGetCapabilities_valid_result_${serverVersion}_image_${imageId}.xml"
    GPathResult s3VerificationResult = wcsCall.getVerificationResultFromStringURL(s3VerificationResultURL)
    println "s3VerificationResultURL: ${s3VerificationResultURL}"
    assert wcsCall.compareResults(wcsGetCapabilitiesResult, s3VerificationResult)
}

When(~/^a user calls WCS DescribeCoverage version (.*) for (.*) (.*) (.*) (.*) image$/) {
    String version, String index, String platform, String sensor, String format ->

        imageId = getImageId(format, index, platform, sensor)
        println("testing WCS DescribeCoverage call on ${imageId}")
        def filter = filterUsingImageId(imageId)
        serverVersion = version

        println "Getting wcsDescribeCoverageResult...."
        wcsDescribeCoverageResult = wcsCall.describeCoverage(wcsServer, serverVersion, coverage, filter)
}

Then(~/^the WCS service responds with a correct DescribeCoverage data for that image$/) { ->
    String s3VerificationResultURL = "${s3BucketUrl}/${s3Bucket}/${s3WcsVerificationFiles}/wcsDescribeCoverage_valid_result_${serverVersion}_image_${imageId}.xml"
    GPathResult s3VerificationResult = wcsCall.getVerificationResultFromStringURL(s3VerificationResultURL)
    println "s3VerificationResultURL: ${s3VerificationResultURL}"
    println "Checking with wcsDescribeCoverageResult"
    assert wcsCall.compareResults(wcsDescribeCoverageResult, s3VerificationResult)
}

When(~/^a call is made to WCS version (.*) for the entire bounding box of (.*) (.*) (.*) (.*) image$/) {
    String version, String index, String platform, String sensor, String format ->

        imageId = getImageId(format, index, platform, sensor)
        println("testing WCS GetCoverage call on ${imageId} for entire bounding box")
        def filter = filterUsingImageId(imageId)
        serverVersion = version

        wcsGetCoverageURL = wcsCall.getCoverage(wcsServer, wfsServer, version, coverage, filter)
}

Then(~/^WCS returns a (.*) that matches the validation image$/) { String imageType ->
    verificationImageURL = new URL("${s3BucketUrl}/${s3Bucket}/${s3WcsVerificationFiles}/${imageId}${currentBbox}.${imageType}")

    assert FileCompare.checkImages(verificationImageURL, wcsGetCoverageURL, imageType)
}

When(~/^a call is made to WCS (.*) for a non-existent image$/) { String version ->
    def filter = "title LIKE 'notanimage'"
    println("testing WCS GetCoverage call on a nonexistent image")

    wcsGetCoverageURL = wcsCall.getCoverageNonExistentImage(wcsServer, version, coverage, filter)
}

Then(~/^WCS returns the proper error$/) { ->
    assert wcsGetCoverageURL.text.contains("ServiceExceptionReport")
}

When(~/^a call is made to WCS (.*) for a bounding box (.*) larger than (.*) (.*) (.*) (.*) image$/) {
    String version, String bbox, String index, String platform, String sensor, String format ->

        imageId = getImageId(format, index, platform, sensor)
        currentBbox = bbox
        println("testing WCS GetCoverage call on ${imageId} for bounding box ${bbox}")
        def filter = filterUsingImageId(imageId)
        serverVersion = version

        wcsGetCoverageURL = wcsCall.getCoverageWithBbox(wcsServer, version, coverage, filter, bbox)
}

When(~/^a call is made to WCS (.*) using a subset bounding box (.*) of (.*) (.*) (.*) (.*) image$/) {
    String version, String bbox, String index, String platform, String sensor, String format ->

        imageId = getImageId(format, index, platform, sensor)
        currentBbox = bbox
        println("testing WCS GetCoverage call on ${imageId} for subset bounding box ${bbox}")
        def filter = filterUsingImageId(imageId)
        serverVersion = version

        wcsGetCoverageURL = wcsCall.getCoverageWithBbox(wcsServer, version, coverage, filter, bbox)
}
