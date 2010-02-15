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
package ucar.unidata.geoloc.ogc;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.unidata.util.Parameter;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class EPSG_OGC_CF_Helper
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( EPSG_OGC_CF_Helper.class );

  public enum ProjectionStandardsInfo
  {
    // From CF 1.0
    Albers_Conic_Equal_Area( 9822, "Albers Equal Area", "albers_conical_equal_area"),
    Azimuthal_Equidistant( -1, "", "azimuthal_equidistant"), // 9832, "Modified Azimuthal Equidistant" [?]
    Lambert_Azimuthal_Equal_Area( 9820, "Lambert Azimuthal Equal Area", "lambert_azimuthal_equal_area"),
    Lambert_Conformal_Conic_2SP( 9802, "Lambert Conic Conformal (2SP)", "lambert_conformal_conic" ),
    Polar_Stereographic( 9810, "Polar Stereographic (Variant A)", "polar_stereographic" ),
    Rotated_Pole( -2, "", "rotated_latitude_longitude"), // 9601 "Rotated Latitude" [?????]
    Stereographic( -3, "", "stereographic"), // 9809, "Oblique Stereographic" [?]
    Transverse_Mercator( 9807, "Transverse Mercator", "transverse_mercator"),

    // Added in CF 1.2
    Latitude_Longitude( 0, "", "latitude_longitude" ),
    Vertical_Perspective( 9838, "Vertical Perspective", "vertical_perspective" ),

    // Added in CF 1.4
    Lambert_Cylindrical_Equal_Area( 9835, "Lambert Cylindrical Equal Area", "lambert_cylindrical_equal_area" ),
    Mercator( 9805, "Mercator (2SP)", "mercator"),
    Orthographic( 9840, "Orthographic", "orthographic");

    private final int epsgCode;
    private final String epsgName;
    private final String cfName;

    public String getOgcName() { return this.name(); }
    public int getEpsgCode() { return this.epsgCode; }
    public String getEpsgName() { return this.epsgName; }
    public String getCfName() { return this.cfName; }

    ProjectionStandardsInfo( int epsgCode, String epsgName, String cfName )
    {
      this.epsgCode = epsgCode;
      this.epsgName = epsgName;
      this.cfName = cfName;
    }

    public String toString()
    {
      StringBuilder buf = new StringBuilder();
      buf.append( "[[OGC: ").append( this.name())
              .append( "] [EPSG ").append( this.getEpsgCode()).append( ": ").append( this.getEpsgName())
              .append( "] [CF: ").append( this.getCfName()).append( "]]");
      return buf.toString();
    }

    public static ProjectionStandardsInfo getProjectionByOgcName( String ogcName )
    {
      for ( ProjectionStandardsInfo curProjStdInfo : values() )
      {
        if ( curProjStdInfo.name().equals( ogcName ) )
          return curProjStdInfo;
      }
      throw new IllegalArgumentException( "No such instance <" + ogcName + ">." );
    }

    public static ProjectionStandardsInfo getProjectionByEpsgCode( int epsgCode )
    {
      for ( ProjectionStandardsInfo curProjStdInfo : values() )
      {
        if ( curProjStdInfo.getEpsgCode() == epsgCode )
          return curProjStdInfo;
      }
      throw new IllegalArgumentException( "No such instance <" + epsgCode + ">." );
    }

    public static ProjectionStandardsInfo getProjectionByEpsgName( String epsgName )
    {
      for ( ProjectionStandardsInfo curProjStdInfo : values() )
      {
        if ( curProjStdInfo.getEpsgName().equals( epsgName ) )
          return curProjStdInfo;
      }
      throw new IllegalArgumentException( "No such instance <" + epsgName + ">." );
    }

    public static ProjectionStandardsInfo getProjectionByCfName( String cfName )
    {
      for ( ProjectionStandardsInfo curProjStdInfo : values() )
      {
        if ( curProjStdInfo.getCfName().equals( cfName ) )
          return curProjStdInfo;
      }
      throw new IllegalArgumentException( "No such instance <" + cfName + ">." );
    }

  }

  public static String getWcs1_0CrsId( Projection proj)
  {
    String paramName = null;
    if ( proj == null )
      paramName = "LatLon";
    else
    {
      for ( Parameter curParam : (List<Parameter>) proj.getProjectionParameters() )
        if ( curParam.getName().equalsIgnoreCase( ProjectionImpl.ATTR_NAME) && curParam.isString() )
          paramName = curParam.getStringValue();
    }
    if ( paramName == null )
      return null;
    if ( paramName.equalsIgnoreCase( "LatLon"))
    {
      paramName = "latitude_longitude";
      return "OGC:CRS84";
    }

    ProjectionStandardsInfo psi = ProjectionStandardsInfo.getProjectionByCfName( paramName);
    return "EPSG:" + psi.getEpsgCode() + "[" + psi.name() + "]";
  }
  
  public String getWcs1_0CrsId( GridDatatype gridDatatype, GridDataset gridDataset )
          throws IllegalArgumentException
  {
    gridDataset.getTitle();
    gridDatatype.getName();

    StringBuilder buf = new StringBuilder();

    Attribute gridMappingAtt = gridDatatype.findAttributeIgnoreCase( "grid_mapping" );
    String gridMapping = gridMappingAtt.getStringValue();
    Variable gridMapVar = gridDataset.getNetcdfFile().findTopVariable( gridMapping);

    Attribute gridMappingNameAtt = gridMapVar.findAttributeIgnoreCase( "grid_mapping_name" );
    String gridMappingName = gridMappingNameAtt.getStringValue();
    buf.append( "EPSG:" ).append( ProjectionStandardsInfo.getProjectionByCfName( gridMappingName));


    return buf.toString();
  }
}
