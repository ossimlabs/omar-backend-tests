@jpip_service
Feature: JPIP Service
  Scenario Outline: [JP-01] Calls are made to query the JPIP server for a stream link with proper parameters
    Given the JPIP service is running
    When a call is made to JPIP to create a stream of an image at <imagePath> entry <entry> and project code <projCode> with a <timeout> millisecond timeout
    Then the JPIP service returns a status of FINISHED without timing out
    Examples:
      | imagePath                                             | entry | projCode | timeout |
      | /data/s3/2009/02/05/00/105001000C126400-1220100.tif   | 0     | 4326     | 5000    |