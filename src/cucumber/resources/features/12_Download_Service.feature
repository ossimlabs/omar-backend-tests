@download_service
Feature: DownloadService
  As the OC2S system, I need to ingest imagery for discovery, processing and viewing in Test and Ops

#  Scenario: [DLS-01] Download image file and supporting zip file
#    Given a GeoEye MSI NITF21 image has been staged
#    When the download service is called to download a GeoEye MSI NITF21 image as a zip file
#    Then a GeoEye MSI NITF21 image is downloaded along with supporting zip file

#  Scenario: [DLS-02] Return KML file for list of images
#    Given a QuickBird MSI GeoTIFF image has been staged
#    When the download service is called to download a KML super-overlay of a QuickBird MSI GeoTIFF image
#    Then the service returns a KML file for a QuickBird MSI GeoTIFF image

#  Scenario: [DLS-03] Return SuperOverlay for list of images
#    Given a RadarSat SAR NITF21 has been staged
#    And another TerraSAR-X SAR NITF20 image has been staged
#    When the download service is called to download a SuperOverlay of those images
#    Then the service returns a SuperOverlay of those images

  Scenario: Calling Download Service without a json message
    Given that the download service is running
    When the download service is called without a json message
    Then the response should return a status of 400 and a message of "Invalid parameters"

  Scenario: Calling Download Service without files groups
    Given that the download service is running
    When the download service is called with no fileGroups specified in the json
    Then the response should return a status of 406 and a message of "No File Group Specified"

  Scenario: Calling Download Service with the wrong archive type
    Given that the download service is running
    When the download service is called with the wrong archive type
    Then the response should return a status of 415 and a message of "Archive Option Type Not Recognized"

  Scenario: Calling Download Service For HSI data
    Given that the download service is running
    When we download a local hsi envi image
    Then the hsi should contain the proper files
