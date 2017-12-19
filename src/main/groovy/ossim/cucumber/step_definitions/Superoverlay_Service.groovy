package ossim.cucumber.step_definitions

import ossim.cucumber.config.CucumberConfig
import ossim.cucumber.ogc.wfs.WFSCall

import java.nio.charset.Charset

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

String defaultCharset = Charset.defaultCharset().displayName()

config = CucumberConfig.config
def wfsServer = config.wfsServerProperty
def httpResponse

/**
 * Get's the imageId from the config for given params.
 * @param format
 * @param index
 * @param platform
 * @param sensor
 * @return
 */
String getImageId(String format, String index, String platform, String sensor) {
    format = format.toLowerCase()
    platform = platform.toLowerCase()
    sensor = sensor.toLowerCase()
    return config.images[platform][sensor][format][index == "another" ? 1 : 0]
}

//Given(~/^(.*) (.*) (.*) (.*) image has been staged$/) {
//    String index, String platform, String sensor, String format ->
//
//        // Fetch image from WFS
//        def imageId = getImageId(format, index, platform, sensor)
//        def filter = "filename LIKE '%${imageId}%'"
//        def wfsCall = new WFSCall(wfsServer, filter, "JSON", 1)
//
//        // Image should have some features
//        def numFeatures = wfsCall.numFeatures
//        assert numFeatures > 0
//}

Then(~/^the service returns a KML file for (.*) (.*) (.*) (.*) image$/) {
    String index, String platform, String sensor, String format ->
        def xml = new XmlSlurper().parseText(httpResponse)

        // xml node <document><name>...</name></document> should contain the image Id.
        def imageId = getImageId(format, index, platform, sensor)
        assert xml.Document.name.toString().contains(imageId.toString())
}

When(~/^the superoverlay service is called to download a KML super-overlay of (.*) (.*) (.*) (.*) image$/) {
    String index, String platform, String sensor, String format ->

        // Fetch databaseId for the image
        def imageId = getImageId(format, index, platform, sensor)
        def filter = "filename LIKE '%${imageId}%'"
        def wfsCall = new WFSCall(wfsServer, filter, "JSON", 2)
        int databaseId = wfsCall.result.features[0].properties.id

        // Fetch KML
        URL superOverlayUrl = new URL("https://omar-dev.ossim.io/omar-superoverlay/superOverlay/createKml/$databaseId")
        httpResponse = superOverlayUrl.text

        // Make sure the call was made without error
        assert httpResponse.contains("xml") && httpResponse.contains("kml")
}