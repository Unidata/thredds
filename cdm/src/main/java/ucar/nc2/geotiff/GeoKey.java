// $Id:GeoKey.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.geotiff;

import java.util.*;

/** A GeoKey is a GeoKey.Tag and a value.
 *  The value may be a String, an array of integers, or an array of doubles.
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

class GeoKey  {
  boolean isDouble = false, isString = false;
  private int count;
  private int[] value;
  private double[] dvalue;
  private String valueS;

  private GeoKey.Tag tag;
  private GeoKey.TagValue tagValue;

  private int id;

  /**
   * Construct a GeoKey with a Tag and TagValue
   * @param tag GeoKey.Tag number
   * @param tagValue value
   */
  GeoKey( GeoKey.Tag tag, GeoKey.TagValue tagValue) {
    this.tag = tag;
    this.tagValue = tagValue;
    count = 1;
  }

  GeoKey( GeoKey.Tag tag, int value) {
    this.tag = tag;
    this.value = new int[1];
    this.value[0] = value;
    count = 1;
  }

  GeoKey( GeoKey.Tag tag, int[] value) {
    this.tag = tag;
    this.value = value;
    count = value.length;
  }

  GeoKey( GeoKey.Tag tag, double[] value) {
    this.tag = tag;
    this.dvalue = value;
    count = value.length;
    isDouble = true;
  }

  GeoKey( GeoKey.Tag tag, double value) {
    this.tag = tag;
    this.dvalue = new double[1];
    this.dvalue[0] = value;
    count = 1;
    isDouble = true;
  }

  GeoKey( GeoKey.Tag tag, String value) {
    this.tag = tag;
    this.valueS = value;
    count = 1;
    isString = true;
  }

  int count() { return count; }
  int tagCode() {
    if (tag != null) return tag.code();
    return id;
  }

  int value() {
    if (tagValue != null) return tagValue.value();
    return value[0];
  }

  int value(int idx) {
    if (idx == 0)
      return value();
    return value[idx];
  }

  double valueD(int idx) {
    return dvalue[idx];
  }

  String valueString() {
    return valueS;
  }

  /**
   * Construct a GeoKey with a single integer value.
   * @param id GeoKey.Tag number
   * @param v value
   */
  GeoKey( int id, int v) {
    this.id = id;
    this.count = 1;

    this.tag = GeoKey.Tag.get(id);
    this.tagValue = TagValue.get(tag, v);

    if (tagValue == null) {
      this.value = new int[1];
      this.value[0] = v;
    }
  }

  public String toString() {
    StringBuilder sbuf  = new StringBuilder();
    if (tag != null)
      sbuf.append(" geoKey = "+tag);
    else
      sbuf.append(" geoKey = "+id);

    if (tagValue != null)
      sbuf.append(" value = "+tagValue);
    else {

      sbuf.append(" values = ");

      if (valueS != null)
        sbuf.append(valueS);
      else if (isDouble)
        for (int i=0; i<count; i++)
          sbuf.append(dvalue[i]+" ");
      else
        for (int i=0; i<count; i++)
          sbuf.append(value[i]+" ");
    }

    return sbuf.toString();
  }

  /** Type-safe enumeration of GeoKeys */
  static class Tag implements Comparable {
    static private HashMap map = new HashMap();

    static public final Tag GTModelTypeGeoKey = new Tag("GTModelTypeGeoKey", 1024);
    static public final Tag GTRasterTypeGeoKey = new Tag("GTRasterTypeGeoKey", 1025);
    static public final Tag GTCitationGeoKey = new Tag("GTCitationGeoKey", 1026);

    static public final Tag GeographicTypeGeoKey = new Tag("GeographicTypeGeoKey", 2048);
    static public final Tag GeogCitationGeoKey = new Tag("GeogCitationGeoKey", 2049);
    static public final Tag GeogGeodeticDatumGeoKey = new Tag("GeogGeodeticDatumGeoKey", 2050);
    static public final Tag GeogPrimeMeridianGeoKey = new Tag("GeogPrimeMeridianGeoKey", 2051);
    static public final Tag GeogLinearUnitsGeoKey = new Tag("GeogLinearUnitsGeoKey", 2052);
    static public final Tag GeogAngularUnitsGeoKey = new Tag("GeogAngularUnitsGeoKey", 2054);
    static public final Tag GeogAngularUnitsSizeGeoKey = new Tag("GeogAngularUnitsSizeGeoKey", 2055);
    static public final Tag GeogSemiMajorAxisGeoKey = new Tag("GeogSemiMajorAxisGeoKey", 2056);
    static public final Tag GeogSemiMinorAxisGeoKey = new Tag("GeogSemiMinorAxisGeoKey", 2057);
    static public final Tag GeogInvFlatteningGeoKey = new Tag("GeogInvFlatteningGeoKey", 2058);
    static public final Tag GeogAzimuthUnitsGeoKey = new Tag("GeogAzimuthUnitsGeoKey", 2060);

    static public final Tag ProjectedCSTypeGeoKey = new Tag("ProjectedCSTypeGeoKey,", 3072);
    static public final Tag PCSCitationGeoKey = new Tag("PCSCitationGeoKey,", 3073);
    static public final Tag ProjectionGeoKey = new Tag("ProjectionGeoKey,", 3074);
    static public final Tag ProjCoordTransGeoKey = new Tag("ProjCoordTransGeoKey", 3075);
    static public final Tag ProjLinearUnitsGeoKey = new Tag("ProjLinearUnitsGeoKey", 3076);
    static public final Tag ProjLinearUnitsSizeGeoKey = new Tag("ProjLinearUnitsSizeGeoKey", 3077);
    static public final Tag ProjStdParallel1GeoKey = new Tag("ProjStdParallel1GeoKey", 3078);
    static public final Tag ProjStdParallel2GeoKey = new Tag("ProjStdParallel2GeoKey", 3079);
    static public final Tag ProjNatOriginLongGeoKey = new Tag("ProjNatOriginLongGeoKey", 3080);
    static public final Tag ProjNatOriginLatGeoKey = new Tag("ProjNatOriginLatGeoKey", 3081);
    static public final Tag ProjFalseEastingGeoKey = new Tag("ProjFalseEastingGeoKey", 3082);
    static public final Tag ProjFalseNorthingGeoKey = new Tag("ProjFalseNorthingGeoKey", 3083);
    static public final Tag ProjFalseOriginLongGeoKey = new Tag("ProjFalseOriginLongGeoKey", 3084);
    static public final Tag ProjFalseOriginLatGeoKey = new Tag("ProjFalseOriginLatGeoKey", 3085);
    static public final Tag ProjFalseOriginEastingGeoKey = new Tag("ProjFalseOriginEastingGeoKey", 3086);
    static public final Tag ProjFalseOriginNorthingGeoKey = new Tag("ProjFalseOriginNorthingGeoKey", 3087);
    static public final Tag ProjCenterLongGeoKey = new Tag("ProjCenterLongGeoKey", 3088);
    static public final Tag ProjCenterLatGeoKey = new Tag("ProjCenterLatGeoKey", 3089);
    static public final Tag ProjCenterEastingGeoKey = new Tag("ProjCenterEastingGeoKey", 3090);
    static public final Tag ProjCenterNorthingGeoKey = new Tag("ProjCenterNorthingGeoKey", 3091);
    static public final Tag ProjScaleAtNatOriginGeoKey = new Tag("ProjScaleAtNatOriginGeoKey", 3092);
    static public final Tag ProjScaleAtCenterGeoKey = new Tag("ProjScaleAtCenterGeoKey", 3093);
    static public final Tag ProjAzimuthAngleGeoKey = new Tag("ProjAzimuthAngleGeoKey", 3094);
    static public final Tag ProjStraightVertPoleLongGeoKey = new Tag("ProjStraightVertPoleLongGeoKey", 3095);

    static public final Tag VerticalCSTypeGeoKey = new Tag("VerticalCSTypeGeoKey", 4096);
    static public final Tag VerticalCitationGeoKey = new Tag("VerticalCitationGeoKey", 4097);
    static public final Tag VerticalDatumGeoKey = new Tag("VerticalDatumGeoKey", 4098);
    static public final Tag VerticalUnitsGeoKey = new Tag("VerticalUnitsGeoKey", 4099);

    static public final Tag GeoKey_ProjCoordTrans = new Tag("GeoKey_ProjCoordTrans", 3075);
    static public final Tag GeoKey_ProjStdParallel1 = new Tag("GeoKey_ProjStdParallel1", 3078);
    static public final Tag GeoKey_ProjStdParallel2 = new Tag("GeoKey_ProjStdParallel2", 3079);
    static public final Tag GeoKey_ProjNatOriginLong = new Tag("GeoKey_ProjNatOriginLong", 3080);
    static public final Tag GeoKey_ProjNatOriginLat = new Tag("GeoKey_ProjNatOriginLat", 3081);
    static public final Tag GeoKey_ProjCenterLong = new Tag("GeoKey_ProjCenterLong", 3088);
    static public final Tag GeoKey_ProjFalseEasting = new Tag("GeoKey_ProjFalseEasting", 3082);
    static public final Tag GeoKey_ProjFalseNorthing = new Tag("GeoKey_ProjFalseNorthing", 3083);
    static public final Tag GeoKey_ProjFalseOriginLong = new Tag("GeoKey_ProjFalseOriginLong", 3084);
    static public final Tag GeoKey_ProjFalseOriginLat = new Tag("GeoKey_ProjFalseOriginLat", 3085);

    static Tag get( int code) {
      return (Tag) map.get( new Integer(code));
    }

    static Tag getOrMake( int code) {
      Tag tag = Tag.get( code);
      return (tag != null) ? tag : new Tag( code);
    }


    String name;
    int code;

    private Tag( String  name, int code) {
      this.name = name;
      this.code = code;
      map.put( new Integer(code), this);
    }

    public Tag( int code) {
      this.code = code;
      //map.put( new Integer(code), this);
    }

    public int code() { return code; }
    public String toString() { return name == null ? code+" " : code+" ("+name+")"; }

    public int compareTo( Object o) {
      if (!(o instanceof Tag))
        return 0;
      Tag to = (Tag) o;
      return code - to.code;
    }
  }

  /** Type-safe enumeration of GeoKey values */
  static class TagValue implements Comparable {
    static private HashMap map = new HashMap();

    static public final TagValue ModelType_Projected = new TagValue(Tag.GTModelTypeGeoKey, "Projected", 1);
    static public final TagValue ModelType_Geographic = new TagValue(Tag.GTModelTypeGeoKey, "Geographic", 2);
    static public final TagValue ModelType_Geocentric = new TagValue(Tag.GTModelTypeGeoKey, "Geocentric", 3);

    static public final TagValue RasterType_Area = new TagValue(Tag.GTRasterTypeGeoKey, "Area", 1);
    static public final TagValue RasterType_Point = new TagValue(Tag.GTRasterTypeGeoKey, "Point", 2);

    // "ellipsoidal only", so you should also specify the GeogPrimeMeridian if not default = Greenwich
    static public final TagValue GeographicType_Clarke1866 = new TagValue(Tag.GeographicTypeGeoKey, "Clarke1866", 4008);
    static public final TagValue GeographicType_GRS_80 = new TagValue(Tag.GeographicTypeGeoKey, "GRS_80", 4019);
    static public final TagValue GeographicType_Sphere = new TagValue(Tag.GeographicTypeGeoKey, "Sphere", 4035);

    // these include the prime meridian, so are preferred
    static public final TagValue GeographicType_NAD83 = new TagValue(Tag.GeographicTypeGeoKey, "GCS_NAD83", 4269);
    static public final TagValue GeographicType_WGS_84 = new TagValue(Tag.GeographicTypeGeoKey, "WGS_84", 4326);
    static public final TagValue GeographicType_GCS_NAD27 = new TagValue(Tag.GeographicTypeGeoKey, "GCS_NAD27", 4267);

    //
    static public final TagValue GeogGeodeticDatum_WGS_84 = new TagValue(Tag.GeogGeodeticDatumGeoKey, "WGS_84", 4326);
    static public final TagValue GeogPrimeMeridian_GREENWICH = new TagValue(Tag.GeogPrimeMeridianGeoKey, "Greenwich", 8901);

    // projections
    static public final TagValue ProjectedCSType_UserDefined = new TagValue(Tag.ProjectedCSTypeGeoKey, "UserDefined", 32767);
    static public final TagValue ProjCoordTrans_LambertConfConic_2SP = new TagValue(Tag.ProjCoordTransGeoKey, "LambertConfConic_2SP", 8);
    static public final TagValue ProjCoordTrans_LambertConfConic_1SP = new TagValue(Tag.ProjCoordTransGeoKey, "LambertConfConic_1SP", 9);
    static public final TagValue ProjCoordTrans_Stereographic = new TagValue(Tag.ProjCoordTransGeoKey, "Stereographic", 14);
    static public final TagValue ProjCoordTrans_TransverseMercator = new TagValue(Tag.ProjCoordTransGeoKey, "TransverseMercator", 1);
    static public final TagValue ProjCoordTrans_AlbersConicalEqualArea = new TagValue(Tag.ProjCoordTransGeoKey, "AlbersConicalEqualArea", 11);
    static public final TagValue ProjCoordTrans_AlbersEqualAreaEllipse = new TagValue(Tag.ProjCoordTransGeoKey, "AlbersEqualAreaEllipse", 11);
    static public final TagValue ProjCoordTrans_Mercator = new TagValue(Tag.ProjCoordTransGeoKey, "Mercator", 7);  
    // units
    static public final TagValue ProjLinearUnits_METER = new TagValue(Tag.ProjLinearUnitsGeoKey, "Meter", 9001);
    static public final TagValue GeogAngularUnits_DEGREE = new TagValue(Tag.GeogAngularUnitsGeoKey, "Degree", 9102);
    //add
    static public final TagValue GeogGeodeticDatum6267 = new TagValue(Tag.GeogGeodeticDatumGeoKey, "North_American_1927", 6267);

    static TagValue get( Tag tag, int code) {
      if (tag == null) return null;
      return (TagValue) map.get( tag.name+code);
    }

    private Tag tag;
    private String name;
    private int value;

    private TagValue( Tag tag, String name, int value) {
      this.tag = tag;
      this.name = name;
      this.value = value;
      map.put( tag.name+value, this);
    }


    public Tag tag() { return tag; }
    public int value() { return value; }
    public String toString() { return value+" ("+name+")"; }

    public int compareTo( Object o) {
      if (!(o instanceof TagValue))
        return 0;
      int ret = tag.compareTo( o);
      if (ret != 0) return ret;
      return value - ((TagValue)o).value;
    }
  }

}

