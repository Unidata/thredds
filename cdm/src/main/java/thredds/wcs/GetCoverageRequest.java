package thredds.wcs;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.StringTokenizer;

/**
 * Handles the GetCoverage Request.
 * @author caron
 * @version $Revision$ $Date$
 */
public class GetCoverageRequest {
  private String coverage, time, vertical;
  private Format format;
  private LatLonRect bbox;

  public GetCoverageRequest( String coverage, String bbox, String time, String vertical, String format) {
    this.coverage = coverage;
    this.bbox = parseBB(bbox);
    this.time = time;
    this.vertical = vertical;
    this.format = Format.getType(format);
  }

  public String getCoverage() { return coverage; }
  public LatLonRect getBoundingBox() { return bbox; }
  public String getTime() { return time; }
  public String getVertical() { return vertical; }
  public Format getFormat() { return format; }

  private LatLonRect parseBB( String bbox) {
    if (bbox == null) return null;
    StringTokenizer stoker = new StringTokenizer( bbox, ", ");
    double minx = Double.parseDouble( stoker.nextToken());
    double miny = Double.parseDouble( stoker.nextToken());
    double maxx = Double.parseDouble( stoker.nextToken());
    double maxy = Double.parseDouble( stoker.nextToken());

    LatLonPointImpl minll = new LatLonPointImpl(miny, minx);
    LatLonPointImpl maxll = new LatLonPointImpl(maxy, maxx);
    return new LatLonRect( minll, maxll);
  }

  public static class Format {
    private static java.util.LinkedHashMap hash = new java.util.LinkedHashMap(10);

    public final static Format NONE = new Format("");
    public final static Format GeoTIFF = new Format("GeoTIFF");
    public final static Format GeoTIFFfloat = new Format("GeoTIFFfloat");
    public final static Format NetCDF3 = new Format("NetCDF3");

    public static java.util.Collection getAllTypes() { return hash.values(); }
    public static Format getType(String name) {
      if (name == null) return null;
      Format result = (Format) hash.get( name);
      if (result != null) return result;
      return null;
    }

    private String name;
    public Format(String s) {
      this.name = s;
      hash.put( s, this);
    }

     public String toString() { return name; }
  }
}