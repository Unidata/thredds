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
// $Id: ZebraClassTrajectoryObsDataset.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Implements TrajectoryDataset for datasets with these characteristics:
 * <ul>
 *   <li> it has only one dimension, the dimension is UNLIMITED and is named "time"</li>
 *   <li> it has one coordinate variable, time(time), with units that
 *        are udunits time units</li>
 *   <li> has the variables latitude(time), longitude(time), and altitude(time) with
 *        units "deg", "deg", and "m", respectively.
 * </ul>
 *
 * @author edavis
 * @since Feb 22, 2005T5:37:14 PM
 */
public class ZebraClassTrajectoryObsDataset extends SingleTrajectoryObsDataset implements TypedDatasetFactoryIF
{
  private static String timeDimName = "time";
  private static String timeVarName = "time";
  private static String latVarName = "lat";
  private static String lonVarName = "lon";
  private static String elevVarName = "alt";

  private static String trajId = "trajectory data";

  static public boolean isValidFile(NetcdfDataset ncd)
  {
    return ( buildConfig( ncd ) != null );
  }

  private static Config buildConfig( NetcdfDataset ncd )
  {
    // Check for "zebra_platform" attribute w/ value of "class".
    Attribute attrib = ncd.findGlobalAttributeIgnoreCase( "zebra_platform" );
    if ( attrib == null ) return null;
    if ( !attrib.isString() ) return null;
    if ( !attrib.getStringValue().equals( "class" ) ) return null;

    // Check for "history" attribute w/ value of "ClassNcFile".
    attrib = ncd.findGlobalAttributeIgnoreCase( "history" );
    if ( attrib == null ) return null;
    if ( !attrib.isString() ) return null;
    if ( !attrib.getStringValue().equals( "ClassNcFile" ) ) return null;

    // Check for existence of global attribute "AvapsEditorVersion".
    attrib = ncd.findGlobalAttributeIgnoreCase( "AvapsEditorVersion" );
    if ( attrib == null ) return null;
    if ( !attrib.isString() ) return null;

    // Check for existence of global attribute "SoundingDescription".
    attrib = ncd.findGlobalAttributeIgnoreCase( "SoundingDescription" );
    if ( attrib == null ) return null;
    if ( !attrib.isString() ) return null;

    // Check that only one dimension and that it is the time dimension.
    List list = ncd.getRootGroup().getDimensions();
    if ( list.size() != 1) return null;
    Dimension d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName )) return null;

    Config trajConfig = new Config();
    trajConfig.setTimeDim( d);

    // Check that have time variable with units that are udunits time
    Variable var = ncd.getRootGroup().findVariable( timeVarName );
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName )) return null;
    String units = var.findAttribute( "units").getStringValue();
    Date date = DateUnit.getStandardDate( "0 " + units);
    if ( date == null) return null;

    trajConfig.setTimeVar( var);

    // Check for latitude variable with time dimension and units convertable to "degrees_north".
    var = ncd.getRootGroup().findVariable( latVarName );
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "degrees_north")) return null;

    trajConfig.setLatVar( var);

    // Check for longitude variable with time dimension and units convertable to "degrees_east".
    var = ncd.getRootGroup().findVariable( lonVarName);
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName )) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "degrees_east")) return null;

    trajConfig.setLonVar( var);

    // Check for altitude variable with time dimension and units convertable to "m".
    var = ncd.getRootGroup().findVariable( elevVarName);
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName )) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "meters")) return null;

    trajConfig.setElevVar( var);

    trajConfig.setTrajectoryId( trajId);

    return trajConfig;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new ZebraClassTrajectoryObsDataset( ncd);
  }
  public FeatureType getScientificDataType() { return FeatureType.TRAJECTORY; }

  public ZebraClassTrajectoryObsDataset() {}

  public ZebraClassTrajectoryObsDataset( NetcdfDataset ncd ) throws IOException
  {
    super( ncd);

    Config trajConfig = buildConfig( ncd);
    this.setTrajectoryInfo( trajConfig );
  }
}
