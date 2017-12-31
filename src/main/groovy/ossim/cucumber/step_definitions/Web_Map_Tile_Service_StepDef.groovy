package ossim.cucumber.step_definitions

import groovy.util.slurpersupport.GPathResult
import ossim.cucumber.config.CucumberConfig

import ossim.cucumber.ogc.wfs.WFSCall
import ossim.cucumber.ogc.wmts.WMTSGetCapabilities
import ossim.cucumber.ogc.wmts.WMTSCall

import ossim.cucumber.ogc.util.FileCompare
import org.apache.commons.io.FileUtils

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

WMTSCall wmtsCall
File wmtsReturnImage
def wmtsLayers
def wmtsGetCapabilitiesReturn
def wmtsTiles
def wmtsValidationTile = "WMTS_verification_images/wmtsValidationImage_WorldGeographic_14SEP15TS0107001_100021_SL0023L_25N121E_001X___SVV_0101_OBS_IMAG"
def error = false

config = CucumberConfig.config
def wmtsServer = config.wmtsServerProperty
def wfsServer = config.wfsServerProperty
def s3Bucket = config.s3Bucket
def s3BucketUrl = config.s3BucketUrl

def getImageId(format, index, platform, sensor)
{
    format = format.toLowerCase()
    platform = platform.toLowerCase()
    sensor = sensor.toLowerCase()

    return config.images[platform][sensor][format][index == "another" ? 1 : 0]
}

static HashMap setWFSIntersectionInfo(WFSCall wfsCall, Integer featureIdx)
{
    [gsd       : wfsCall.getGsd(featureIdx),
     nResLevels: wfsCall.getNumberOfResLevels(featureIdx),
     width     : wfsCall.getWidth(featureIdx),
     height    : wfsCall.getHeight(featureIdx),
     bounds    : wfsCall.getBounds(featureIdx)]
}

// Scenario [WMTS-01]
When(~/^a user calls GetCapabilities for version (.*)$/) { String version ->
    wmtsGetCapabilitiesReturn = new WMTSGetCapabilities(wmtsServer, version).wmtsGetCapabilitiesResult
}

// Scenario [WMTS-02]
When(~/^a call is made to WMTS for a (.*) image the for the entire bounding box of (.*) (.*) (.*) (.*) image$/) {
    String imageType, String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)
        String filter = "entry_id='0' and filename LIKE '%${imageId}%'"

        wmtsCall = new WMTSCall(wmtsServer: wmtsServer)
        wmtsLayers = wmtsCall.layers
        HashMap layerHashMap = wmtsLayers[0] as HashMap
        WFSCall wfsCall = new WFSCall(wfsServer, filter, "JSON", 1)
        HashMap wmtsParams = setWFSIntersectionInfo(wfsCall, 0)

        wmtsTiles = wmtsCall.getTile(layerHashMap, wmtsParams.gsd, wmtsParams.nResLevels, wmtsParams.width, wmtsParams.height, wmtsParams.bounds)
        wmtsReturnImage = File.createTempFile("tempImageWMTS1", ".${imageType}")
        FileUtils.copyURLToFile(wmtsTiles.url, wmtsReturnImage)
}

// Scenario [WMTS-03]
When(~/^a call is made to WMTS for a (.*) outside entire bounding box of (.*) (.*) (.*) (.*) image$/) {
    String imageType, String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)
        String filter = "entry_id='0' and filename LIKE '%${imageId}%'"

        try
        {
            wmtsCall = new WMTSCall(wmtsServer: wmtsServer)
            wmtsLayers = wmtsCall.layers
            HashMap layerHashMap = wmtsLayers[0] as HashMap
            WFSCall wfsCall = new WFSCall(wfsServer, filter, "JSON", 1)
            HashMap wmtsParams = setWFSIntersectionInfo(wfsCall, 0)
            wmtsTiles = wmtsCall.getInvalidTile(layerHashMap, wmtsParams.gsd, wmtsParams.nResLevels, wmtsParams.width, wmtsParams.height, wmtsParams.bounds)
            wmtsReturnImage = File.createTempFile("tempImageWMTS1", ".${imageType}")
            FileUtils.copyURLToFile(wmtsTiles.url, wmtsReturnImage)
        } catch (FileNotFoundException e)
        {
            error = true
        }
}

// Scenario [WMTS-04]
When(~/^a call is made to WMTS for a non-existent image$/) { ->
    try
    {
        wmts = new WMTSCall(wmtsServer: wmtsServer)
        wmtsLayers = wmtsCall.layers
        HashMap layerHashMap = wmtsLayers[0] as HashMap
        WFSCall wfsCall = new WFSCall(wfsServer, "This_image_does_not_exist_IMAG", "JSON", 1)
    } catch (IOException e)
    {
        error = true
    }
}

// Scenario [WMTS-05]
When(~/^a call is made to WMTS for (.*) of a subset of (.*) (.*) (.*) (.*) image$/) {
    String imageType, String index, String platform, String sensor, String format ->

        def imageId = getImageId(format, index, platform, sensor)

        String filter = "entry_id='0' and filename LIKE '%${imageId}%'"
        wmtsCall = new WMTSCall(wmtsServer: wmtsServer)
        wmtsLayers = wmtsCall.layers
        HashMap layerHashMap = wmtsLayers[0] as HashMap
        WFSCall wfsCall = new WFSCall(wfsServer, filter, "JSON", 1)
        HashMap wmtsParams = setWFSIntersectionInfo(wfsCall, 0)

        wmtsTiles = wmtsCall.getTile(layerHashMap, wmtsParams.gsd, wmtsParams.nResLevels, wmtsParams.width, wmtsParams.height, wmtsParams.bounds)
        wmtsReturnImage = File.createTempFile("tempImageWMTS1", ".${imageType}")
        FileUtils.copyURLToFile(wmtsTiles.url, wmtsReturnImage)
}

// Scenario [WMTS-01]
Then(~/^the WMTS service responds with a correct GetCapabilities statement$/) { ->
    assert wmtsGetCapabilitiesReturn instanceof GPathResult
}

// Scenario [WMTS-02] & [WMTS-05]
Then(~/^WMTS returns tiles that matches the validation (.*) image$/) { String imageType ->

    def verificationImageUrl = new URL("${s3BucketUrl}/${s3Bucket}/${wmtsValidationTile}.${imageType}")
    File validFile = File.createTempFile("tempImageWMTS2", ".${imageType}")
    FileUtils.copyURLToFile(verificationImageUrl, validFile)

    wmtsReturnImage.deleteOnExit()
    validFile.deleteOnExit()

    def fileComp = new FileCompare()
    assert fileComp.checkImages(validFile, wmtsReturnImage)
}

// Scenario [WMTS-03] & [WMTS-04]
Then(~/^WMTS returns the proper error$/) { ->
    assert error
}
