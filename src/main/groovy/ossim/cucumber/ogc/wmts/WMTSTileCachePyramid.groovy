package ossim.cucumber.ogc.wmts

import geoscript.layer.Pyramid
import geoscript.layer.Grid
import geoscript.geom.Bounds
import geoscript.proj.Projection
import groovy.json.JsonBuilder
import groovy.transform.ToString

@ToString (includeNames = true)
class WMTSTileCachePyramid extends Pyramid
{

    Bounds clippedBounds = new Projection("EPSG:4326").bounds
    def geographicProj = new Projection("EPSG:4326")
    Integer minLevel
    Integer maxLevel


    def getMinMaxLevel()
    {
        [minLevel: minLevel, maxLevel: maxLevel]
    }

    /**
     * Options can be supplied to shrink the clip region further.  So if your image covers
     * several levels you can clip to a particular level and region
     *
     *
     * minLevel:<minimum level>
     * maxLevel:<maximum level>
     * bbox: minx, miny, maxx, maxy
     * epsgCode: EPSG:4326
     *
     * bbox specifies a clip region.  This will be intersected with the pyramid of the
     * tile cache and intersected with the valid bounds of the image
     *
     * epsgCode specifies the projection the bbox is defined in
     * minLevel is the min level you wish to  clip to
     * maxLevel is the max level to clip
     *
     * @param tileCacheSupport This is a bridge to the OSSIM library for opening and assessing imagery.
     *        This is used to get a clipped bounds and gsd range
     * @param entry The entry within the image.  We support multi entry image sets
     * @param options Supplies override parameters to clip the levels
     *
     * @return a hashmap that contains the clipped bounds that include the minLevel, maxLevel
     *         and the clipped geospatial bbox not tile aligned
     */
    def findIntersections(Bounds bounds, Double gsd, Integer width, Integer height, Integer nRlevels = 0,
                          def options = [:])
    {
        def result = [:]
        // for tiling we only support square projections
        double[] resolutions = grids*.yResolution as double[]
        int[] levels = grids*.z as double[]
        // set to 0,0 meters per degree estimate
        Double mpd = 111319.49079327357

        // println resolutions
        // println levels

        // get the bounds of the input image
        // def ossimEnvelope = tileCacheSupport.getEnvelope(entry)
        def clipBounds
        def latLonClipBounds

//    if(envelope)
//    {
        // println "GOT ENVELOPE: ${ossimEnvelope}"
        // transform the bounds of the input image
        Bounds inputImageBounds = bounds
        inputImageBounds.setProj(geographicProj)
        // println "THIS projeciton: ${this.proj}"

        // create a reprojected bounds defined in the projection of this pyramid
        def reprojectedImageBounds = inputImageBounds.reproject(this.proj)

        // println "reprojectedImageBounds: ${reprojectedImageBounds}"
        //def geoScriptGeom = inputImageBounds.geometry

        // transform the points
        //  def reprojectedGeom = inputImageBounds.proj.transform(geoScriptGeom,geographicProj)

        clipBounds = this.clippedBounds.intersection(reprojectedImageBounds)


        latLonClipBounds = this.proj.transform(clipBounds.geometry, geographicProj)

        if (((clipBounds.width > 0.0) && (clipBounds.height > 0.0)) != true)
        {
            clipBounds = null
        }

        // now clip to the passed in bbox constraint
        //
        if (options.bbox && options.epsgCode && clipBounds)
        {
            def bboxArray = options.bbox.split(",")
            if (bboxArray.size() == 4)
            {
                def bboxBounds = new Bounds(bboxArray[0].toDouble(), bboxArray[1].toDouble(),
                        bboxArray[2].toDouble(), bboxArray[3].toDouble(), new Projection(options.epsgCode))
                if (bboxBounds.proj)
                {
                    clipBounds = bboxBounds.reproject(this.proj).intersection(clipBounds)
                }
            }
        }
        //}

        if (nRlevels > 0)
        {
            // first we will find the number of decimation for the image to be within a single tile
            //
            double highestRes
            double lowestRes
            //println "EPSG ====== ${this.proj.epsg}"
            if (this.proj.epsg == 4326)
            {
                highestRes = (1.0 / mpd) * gsd
            }
            else
            {
                highesRes = gsd
            }
            int tileSize = Math.max(this.tileWidth, this.tileHeight)
            int largestSize = Math.max(width, height)
            int maxDecimationLevels = 0
            int testSize = largestSize
            while ((testSize > 0))
            {
                ++maxDecimationLevels
                testSize = testSize >> 1
            }

            // once we find the number of decimations then we will find the estimate for the
            // resolution at that decimation
            //
            lowestRes = highestRes * (1 << maxDecimationLevels)
            //println "lowestRes res === ${lowestRes}"

            // now we have the full res of the image gsd and the corsest res of the image within the
            // decimation range.
            //
            // now find the min and max levels from the passed in pyramid resolution
            // identified by the resLevels array
            //
            int maxLevel = resolutions.length - 1
            int minLevel = 0
            int i

            // if we are outside the res levels then we do not intersect
            //
            if (!clipBounds && (highestRes > resolutions[0]) || (lowestRes < resolutions[-1]))
            {
                result = [:]
            }
            else
            {
                for (i = 0; i < resolutions.length; ++i)
                {
                    if (highestRes > resolutions[i])
                    {
                        maxLevel = i
                        if (i > 0) maxLevel--
                        break
                    }
                }
                for (i = resolutions.length - 1; i >= 0; --i)
                {
                    if (lowestRes < resolutions[i])
                    {
                        minLevel = i
                        break
                    }
                }
                def resultMinLevel = minLevel + levels[0]
                def resultMaxLevel = maxLevel + levels[0]

                def optionsMinLevel = (options.minLevel != null) ? options.minLevel : resultMinLevel
                def optionsMaxLevel = (options.maxLevel != null) ? options.maxLevel : resultMaxLevel

                if ((options.minLevel != null) || (options.maxLevel != null))
                {
                    if ((optionsMinLevel > resultMaxLevel) || (optionsMaxLevel < resultMinLevel))
                    {
                        resultMinLevel = 9999
                        resultMaxLevel = -1
                    }
                    else
                    {
                        if (optionsMaxLevel < resultMaxLevel)
                        {
                            resultMaxLevel = optionsMaxLevel
                        }
                        if (optionsMinLevel > resultMinLevel)
                        {
                            resultMinLevel = optionsMinLevel
                        }
                    }
                }
                if (resultMinLevel <= resultMaxLevel)
                {
                    def minMax = intersectLevels(resultMinLevel, resultMaxLevel)
                    if (minMax)
                    {
                        result = [clippedGeometryLatLon: latLonClipBounds, clippedBounds: clipBounds,
                                  minLevel             : minMax?.minLevel, maxLevel: minMax?.maxLevel]
                    }
                }
            }
        }
        result
    }

    HashMap intersectLevels(Integer minLevel, Integer maxLevel)
    {
        HashMap result = [:]
        HashMap minMaxLevels = this.minMaxLevel

        if (minMaxLevels.minLevel <= minMaxLevels.maxLevel)
        {
            result.maxLevel = Math.min(minMaxLevels.maxLevel, maxLevel)
            result.minLevel = Math.max(minMaxLevels.minLevel, minLevel)
            if (result.minLevel > result.maxLevel)
            {
                result = [:]
            }
        }

        result
    }

    WMTSTileCacheHints getHints()
    {
        def minMax = this.minMaxLevel

        WMTSTileCacheHints result = new WMTSTileCacheHints(tileWidth: tileWidth,
                tileHeight: tileHeight,
                layerBounds: this.bounds,
                proj: this.proj,
                clipBounds: clippedBounds,
                minLevel: minMax.minLevel,
                maxLevel: minMax.maxLevel
        )

        result
    }

    def getLevelInformationAsJSON()
    {
        def levels = []

        this.grids.each { grid ->
            levels << [
                    zoomLevel     : grid.z,
                    minx          : this.bounds.minX,
                    miny          : this.bounds.minY,
                    maxx          : this.bounds.maxX,
                    maxy          : this.bounds.maxY,
                    ncols         : grid.width,
                    nrows         : grid.height,
                    unitsPerPixelX: grid.xResolution,
                    unitsPerPixelY: grid.yResolution,
                    tileDeltaX    : this.tileWidth * grid.xResolution,
                    tileDeltaY    : this.tileHeight * grid.yResolution
            ]
        }

        def builder = new JsonBuilder(levels)

        builder.toString()
    }

    void initializeGrids(def hints)
    {

        if (!this.bounds) this.bounds = hints.layerBounds


        if (!this.bounds)
        {
            if (!this.proj) this.proj = hints.proj

            this.bounds = BoundsUtil.getDefaultBounds(this.proj)
        }

        if (!clippedBounds) clippedBounds = this.bounds

        if (hints.minLevel != null) this.minLevel = hints.minLevel
        if (hints.maxLevel != null) this.maxLevel = hints.maxLevel
        if (hints.origin) this.origin = hints.origin

        this.minLevel = this.minLevel ?: 0
        if (this.maxLevel == null) this.maxLevel = 22
        if ((this.minLevel != null) && (this.maxLevel != null))
        {
            //Until the bug in geoscript is fixed we will make sure that we always start from 0
            //Seems geoscript does not like the starting grid to be something other than 0
            //
            initializeGrids(0, this.maxLevel)
        }

        // println "HINTS: ${this.hints}"
    }

    void initializeGrids(int clampMinLevel, int clampMaxLevel)
    {
        // Geoscript bug for not allowing for sparse grids where we might start at level 5
        // instead of 0
        //
        if (clampMinLevel > 0) clampMinLevel = 0
        if (this.tileWidth &&
                this.tileHeight &&
                this.bounds &&
                this.proj)
        {
            double modelSize = bounds.width
            int numberTilesAtRes0 = 1
            // if geographic

            if (proj.epsg == 4326)
            {
                if (tileWidth == tileHeight)
                {
                    numberTilesAtRes0 = 2
                    modelSize /= 2.0
                }
            }
            int n = 0
            this.grids = (clampMinLevel..clampMaxLevel).collect { long z ->

                n = 2**z
                double res = modelSize / n
                new Grid(z, numberTilesAtRes0 * n, n, res / tileWidth, res / tileWidth)
            }

        }
    }
}

