rbtcloudRootDir = "https://omar-dev.ossim.io"
s3Bucket = "o2-test-data/Standard_test_imagery_set"
s3BucketUrl = "https://s3.amazonaws.com"
s3WcsVerificationFiles = "WCS_verification_images"
s3BasemapVerificationFiles = "Basemap_verification_images"
s3BasemapUrlList = "Basemaptest-dev.txt"

downloadService = "${rbtcloudRootDir}/omar-download"
stagingService = "${rbtcloudRootDir}/omar-stager/dataManager"
wfsServerProperty = "${rbtcloudRootDir}/omar-wfs/wfs"
wmsServerProperty = "${rbtcloudRootDir}/omar-wms/wms"
wcsServerProperty = "${rbtcloudRootDir}/omar-wcs/wcs"
wmtsServerProperty = "${rbtcloudRootDir}/omar-wmts/wmts"
geoscriptService = "${rbtcloudRootDir}/omar-geoscript/geoscriptApi"
imageSpaceServerProperty = "${rbtcloudRootDir}/omar-oms/imageSpace"
ngtService = "${rbtcloudRootDir}/ngt-service/ngt"
jpipService = "${rbtcloudRootDir}/omar-jpip/jpip"

mensaUrl = "${rbtcloudRootDir}/omar-mensa"
wfsUrl = "${rbtcloudRootDir}/omar-wfs"
wmsUrl = "${rbtcloudRootDir}/omar-wms"
wmtsUrl = "${rbtcloudRootDir}/omar-wmts"
omarOldmarProxy = "${rbtcloudRootDir}/omar"
wfsPostString = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><wfs:GetFeature xmlns:wfs="http://www.opengis.net/ogc" version="1.0.0" resultType="results" maxFeatures="20"><wfs:Query srsName="EPSG:4326" typeName="omar:raster_entry"><Filter xmlns="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml"><And><And><PropertyIsGreaterThanOrEqualTo><PropertyName>acquisition_date</PropertyName><Literal>2000-12-13T00:00:00Z</Literal></PropertyIsGreaterThanOrEqualTo><PropertyIsLessThan><PropertyName>acquisition_date</PropertyName><Literal>2017-12-14T00:00:00Z</Literal></PropertyIsLessThan></And><BBox areanamehint="temp area 9"><PropertyName>ground_geom</PropertyName><gml:Envelope srsName="CRS84"><gml:lowerCorner>138.9577467 -23.5650826</gml:lowerCorner><gml:upperCorner>138.9745629 -23.5502934</gml:upperCorner></gml:Envelope></BBox></And></Filter></wfs:Query></wfs:GetFeature>"""
geoscriptDefaultMax = 15

// minutes to wait
waitForStage = 5

images = [
    geoeye: [
        msi: [
            geotiff: [ "14AUG20010406-M1BS-053852449040_01_P001" ],
            nitf21: [ "05FEB09OV05010005V090205M0001912264B220000100072M_001508507" ]
        ],
        pan: [
            geotiff: [ "14AUG20010406-P1BS-053852449040_01_P001" ],
            nitf21: [ "05FEB09OV05010005V090205P0001912264B220000100282M_001508507" ]
        ]
    ],
    ikonos: [
        pan: [
            nitf: [ "po_106005_pan_0000000" ]
        ]
    ],
    ntm: [
        ir: [
            nitf: [ "" ]
        ],
        pan: [
            nitf: [ "" ]
        ],
        sar: [
            nitf: [ "" ]
        ]
    ],
    quickbird: [
        msi: [
            geotiff: [ "04DEC11050020-M2AS_R1C1-000000185964_01_P001" ]
        ],
        pan: [
            geotiff: [ "04DEC11050020-P2AS_R1C1-000000185964_01_P001" ]
        ]
    ],
    rapideye: [
        msi: [
            geotiff: [ "2010-12-05T221358_RE2_3A-NAC_6683383_113276" ]
        ]
    ],
    "terrasar-x": [
        sar: [
            nitf20: [ "14SEP15TS0107001_100021_SL0023L_25N121E_001X___SVV_0101_OBS_IMAG" ]
        ]
    ],
    worldview2: [
        msi: [
            geotiff: [ "14SEP12113301-M1BS-053951940020_01_P001" ]
        ],
        pan: [
            geotiff: [
                "16MAY02111606-P1BS-055998375010_01_P013",
                "16MAY02111607-P1BS-055998375010_01_P014"
            ],
            nitf20: [ "11MAR08WV010500008MAR11071429-P1BS-005707719010_04_P003" ]
        ]
    ],
    local: [
            hsi:[
                    envi:"/data/hsi/2012-06-11/AM/ALPHA/2012-06-11_18-20-11/HSI/Scan_00007/2012-06-11_18-20-11.HSI.Scan_00007.scene.corrected.hsi"
            ]
    ],
    remote: [
        quickbirdpan: [
            nitf:[
              s3: "s3://o2-test-data/direct-test/celtic/007/po_105215_pan_0000000.ntf",
              mount: "/s3/o2-test-data/direct-test/celtic/007/po_105215_pan_0000000.ntf",
            ]
        ]
    ]

]
