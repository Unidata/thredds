package thredds.server.wfs;

import ucar.nc2.ft2.simpgeometry.*;

/**
 * Takes in a Simple Geometry, determines which kind it is, and writes the corresponding xml
 *
 * @author Stanley Kaymen
 *
 */
public class GMLFeatureWriter {

    /**
     * Checks the type of the Simple Geom and calls the appropriate method to build the xml
     *
     * @param geom the Simple Geom
     * @return the xml
     */
    public String writeFeature(SimpleGeometry geom) {

        if (geom instanceof Point) return writePoint((Point)geom);
        else if (geom instanceof Line) return writeLine((Line)geom);
        else if (geom instanceof Polygon) return writePolygon((Polygon)geom);
        else return null;

    }

    /**
     * Takes in a point and writes its xml
     *
     * @param point the point
     * @return the xml
     */
    private String writePoint(Point point) {

        String xml = "";
        xml += "<gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml@900913\" srsDimension=\"2\">"
                + "<gml:pos>" + point.getX() + " " + point.getY() +"</gml:pos>"
                + "</gml:Point>";

        return xml;
    }

    /**
     * Takes in a line and iterates through all its points, writing the posList to xml
     *
     * @param line the line
     * @return the xml
     */
    private String writeLine(Line line) {

        String xml = "";
        xml += "<gml:LineString><gml:posList>";

        for (Point point: line.getPoints()) {
            xml += point.getX() + " " + point.getY() + " ";
        }

        xml += "</gml:posList></gml:LineString>";

        return xml;
    }

    /**
     * Takes in a polygon, checks whether it is an interior or exterior ring, and writes the corresponding xml.
     * Iterates through all linked polygons
     *
     * @param polygon the polygon
     * @return the xml
     */
    private String writePolygon(Polygon poly) {

        String xml = "";
        xml += "<gml:Polygon>";

        Polygon polygon = poly;
        
    //    while (polygon != null) {

            if (!polygon.getInteriorRing()) {
                xml += "<gml:exterior><gml:LinearRing><gml:posList>";

                for (Point point : polygon.getPoints()) {
                    xml += point.getX() + " " + point.getY() + " ";
                }

                xml += "</gml:posList></gml:LinearRing></gml:exterior>";

            }

            else {
                xml += "<gml:interior><gml:LinearRing><gml:posList>";

                for (Point point : polygon.getPoints()) {
                    xml += point.getX() + " " + point.getY() + " ";
                }

                xml += "</gml:posList></gml:LinearRing></gml:interior>";
            }

      //      polygon = polygon.getNext();
       // }

        xml += "</gml:Polygon>";
        return xml;
    }
}
