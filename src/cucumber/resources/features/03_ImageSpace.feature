@image_space
Feature: ImageSpaceService


  Scenario: [IMG-01] call ImageSpace to view a full screen jpeg image of an entire commercial EO image
#    Given a GeoEye MSI NITF21 image has been staged
    When a call is made to ImageSpace for a png of the entire bounding box of a GeoEye MSI NITF21 image
    Then ImageSpace returns a png that matches the validation of a GeoEye MSI NITF21 image

  Scenario: [IMG-02] call ImageSpace to view a single tile of overview jpeg image of an entire commercial SAR image
#    Given a TerraSAR-X SAR NITF20 image has been staged
    When a call is made to ImageSpace for a jpeg single tile overview of a TerraSAR-X SAR NITF20 image
    Then ImageSpace returns a jpeg that matches the validation of a TerraSAR-X SAR NITF20 image

  Scenario: [IMG-03] call ImageSpace to get a thumbnail overview of an image
#    Given a QuickBird MSI GeoTIFF image has been staged
    When a call is made to ImageSpace to get a png thumbnail of a QuickBird MSI GeoTIFF image
    Then ImageSpace returns a png that matches the validation of a QuickBird MSI GeoTIFF_thumbnail image

  Scenario: [IMG-04] call ImageSpace to view an overview tile of an commercial msi image in red green blue band order
#    Given a WorldView2 MSI GeoTIFF image has been staged
    When a call is made to ImageSpace for an overview tile in red green blue order of a WorldView2 MSI GeoTIFF image
    Then ImageSpace returns a png that matches the validation of a WorldView2 MSI GeoTIFF_rgb image

  Scenario: [IMG-05] call ImageSpace to view an overview tile of an commercial msi image in green blue red band order
#    Given a WorldView2 MSI GeoTIFF image has been staged
    When a call is made to ImageSpace for an overview tile in green blue red order of a WorldView2 MSI GeoTIFF image
    Then ImageSpace returns a png that matches the validation of a WorldView2 MSI GeoTIFF_gbr image

  Scenario: [IMG-06] call ImageSpace to view an overview tile of an commercial msi image with only the green band
#    Given a WorldView2 MSI GeoTIFF image has been staged
    When a call is made to ImageSpace for an overview tile green band of a WorldView2 MSI GeoTIFF image
    Then ImageSpace returns a png that matches the validation of a WorldView2 MSI GeoTIFF_g image
