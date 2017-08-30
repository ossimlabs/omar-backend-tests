package ossim.cucumber.ogc.geoscript

import geoscript.geom.Point
import geoscript.proj.Projection

class Geoscript
{
    Geoscript() {}

    static String createPolygon(lat, lon, radius)
    {
        def polygon
        def epsg4326 = new Projection("epsg:4326")
        def epsg3857 = new Projection("epsg:3857")

        polygon = epsg3857.transform(epsg4326.transform(new Point(lon, lat), epsg3857).buffer(radius), epsg4326)
        polygon.toString()
    }
}
