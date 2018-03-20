/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geotiff;

import java.util.*;

/** Type-safe enumeration of Tiff Tags. Not complete, just the ones weve actually seen.
 *  *
 * @author caron
 */

class Tag implements Comparable {
  static private HashMap map = new HashMap();

  // general tiff tags
  static public final Tag NewSubfileType = new Tag("NewSubfileType", 254);
  static public final Tag ImageWidth = new Tag("ImageWidth", 256);
  static public final Tag ImageLength = new Tag("ImageLength", 257);
  static public final Tag BitsPerSample = new Tag("BitsPerSample", 258);
  static public final Tag Compression = new Tag("Compression", 259);
  static public final Tag PhotometricInterpretation = new Tag("PhotometricInterpretation", 262);
  static public final Tag FillOrder = new Tag("FillOrder", 266);
  static public final Tag DocumentName = new Tag("DocumentName", 269);
  static public final Tag ImageDescription = new Tag("ImageDescription", 270);
  static public final Tag StripOffsets = new Tag("StripOffsets", 273);
  static public final Tag Orientation = new Tag("Orientation", 274);
  static public final Tag SamplesPerPixel = new Tag("SamplesPerPixel", 277);
  static public final Tag RowsPerStrip = new Tag("RowsPerStrip", 278);
  static public final Tag StripByteCounts = new Tag("StripByteCounts", 279);
  static public final Tag XResolution = new Tag("XResolution", 282);
  static public final Tag YResolution = new Tag("YResolution", 283);
  static public final Tag PlanarConfiguration = new Tag("PlanarConfiguration", 284);
  static public final Tag ResolutionUnit = new Tag("ResolutionUnit", 296);
  static public final Tag PageNumber = new Tag("PageNumber", 297);
  static public final Tag Software = new Tag("Software", 305);
  static public final Tag ColorMap = new Tag("ColorMap", 320);
  static public final Tag TileWidth = new Tag("TileWidth", 322);
  static public final Tag TileLength = new Tag("TileLength", 323);
  static public final Tag TileOffsets = new Tag("TileOffsets", 324);
  static public final Tag TileByteCounts = new Tag("TileByteCounts", 325);
  static public final Tag SampleFormat = new Tag("SampleFormat", 339);
  static public final Tag SMinSampleValue = new Tag("SMinSampleValue", 340);
  static public final Tag SMaxSampleValue = new Tag("SMaxSampleValue", 341);

   // tiff tags used for geotiff
  static public final Tag ModelPixelScaleTag = new Tag("ModelPixelScaleTag", 33550);
  static public final Tag IntergraphMatrixTag = new Tag("IntergraphMatrixTag", 33920);
  static public final Tag ModelTiepointTag = new Tag("ModelTiepointTag", 33922);
  static public final Tag ModelTransformationTag = new Tag("ModelTransformationTag", 34264);
  static public final Tag GeoKeyDirectoryTag = new Tag("GeoKeyDirectoryTag", 34735);
  static public final Tag GeoDoubleParamsTag = new Tag("GeoDoubleParamsTag", 34736);
  static public final Tag GeoAsciiParamsTag = new Tag("GeoAsciiParamsTag", 34737);

  // Gdal tiff tags
  static public final Tag GDALNoData = new Tag("GDALNoDataTag", 42113);

  /**
   * Get the Tag by number.
   * @param code Tiff Tag number.
   * @return Tag or null if no match.
   */
  static Tag get( int code) {
    return (Tag) map.get( new Integer(code));
  }

  private String name;
  private int code;

  private Tag( String  name, int code) {
    this.name = name;
    this.code = code;
    map.put( new Integer(code), this);
  }

  /** for unknown tags */
  Tag( int code) {
    this.code = code;
    //map.put( new Integer(code), this);
  }

  /** get the Tag name. may be null */
  public String getName() { return name; }
  /** get the Tag number */
  public int getCode() { return code; }
  /* String representation */
  public String toString() { return name == null ? code+" " : code+" ("+name+")"; }

  /** sort by number */
  public int compareTo( Object o) {
    return code - ((Tag) o).getCode();
  }

}
