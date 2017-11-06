package ossim.cucumber.step_definitions

import groovy.json.JsonSlurper
import ossim.cucumber.config.CucumberConfig

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

config = CucumberConfig.config
def jpipService = config.jpipService
def jpipResponse = null

Given(~/the JPIP service is running/) { ->
    def httpStatus = null

    HttpURLConnection jpipConnection = new URL("${jpipService}").openConnection()
    httpStatus = jpipConnection.getResponseCode()
    jpipConnection.disconnect()

    assert httpStatus == 200
}

When(~/^a call is made to JPIP to create a stream of an image at (.*) entry (.*) and project code (.*) with a (.*) millisecond timeout/) {
    String imagePath, String entry, String projCode, String timeoutStringMillis ->
        int timeoutInMillis = timeoutStringMillis.toInteger()
        URL createStreamUrl = "$jpipService/createStream?filename=$imagePath&entry=$entry&projCode=$projCode".toURL()
        def jsonSlurper = new JsonSlurper()

        // Need to wait until the image has been processed.
        // The initial "READY" state turns to "FINISHED" when done.
        assert waitForTrueOrTimeout(timeoutInMillis) {
            jpipResponse = jsonSlurper.parse(createStreamUrl)["status"] == "FINISHED"
        }
}

Then(~/^the JPIP service returns a status of FINISHED without timing out/) { ->
    assert jpipResponse == "FINISHED"
}

/**
 * Evaluates the closure at a set intervalInMillis if within a time limit.
 * The function returns false if the closure returns false or the time limit is exceeded.
 * @param timeInMillis The time limit
 * @param intervalInMillis The interval between executions of the closure
 * @param closure A closure that returns a boolean
 * @return True if the closure returned true within the time limit
 */
static Boolean waitForTrueOrTimeout(int timeInMillis, int intervalInMillis = 200, Closure<Boolean> closure) {
    def startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeInMillis) {
        if (closure()) return true
        sleep(intervalInMillis)
    }
    return false
}