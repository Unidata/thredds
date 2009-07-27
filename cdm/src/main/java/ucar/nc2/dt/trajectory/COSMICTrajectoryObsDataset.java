/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.dt.trajectory;

import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.DateUtil;

import java.util.*;
import java.io.IOException;

import visad.CommonUnit;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jul 14, 2009
 * Time: 3:02:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class COSMICTrajectoryObsDataset  extends TrajectoryObsDatasetImpl implements TypedDatasetFactoryIF {
  private static String dimName = "MSL_alt";
  private static String dimVarName = "MSL_alt";
  private static String latVarName = "Lat";
  private static String lonVarName = "Lon";
  private static String elevVarName = "MSL_alt";

  private static String trajId = "trajectory data";
  NetcdfDataset localNCD;

  static public boolean isValidFile(NetcdfDataset ncd)
  {
    return ( buildConfig( ncd ) != null );
  }

  private static TrajectoryObsDatasetImpl.Config buildConfig( NetcdfDataset ncd )
  {
    // Check that only one dimension and that it is named "time".

    List list = ncd.getRootGroup().getDimensions();
    if ( list.size() != 1) return null;
    Dimension d = (Dimension) list.get(0);
    if ( ! d.getName().equals( dimName)) return null;

    TrajectoryObsDatasetImpl.Config trajConfig = new TrajectoryObsDatasetImpl.Config();
    trajConfig.setTrajectoryDim( d);

    // Check that have variable time(time) with units that are udunits time
    Variable var = ncd.getRootGroup().findVariable( dimVarName);
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( dimName)) return null;
    String units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "km")) return null;

    trajConfig.setDimensionVar( var);

    // Check for variable latitude(time) with units of "deg".
    var = ncd.getRootGroup().findVariable( latVarName);
    if ( var == null ) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( dimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "deg")) return null;

    trajConfig.setLatVar( var);

    // Check for variable longitude(time) with units of "deg".
    var = ncd.getRootGroup().findVariable( lonVarName);
    if ( var == null ) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( dimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "deg")) return null;

    trajConfig.setLonVar( var);

    // Check for variable altitude(time) with units of "m".
    var = ncd.getRootGroup().findVariable( elevVarName);
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( dimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "km")) return null;

    trajConfig.setElevVar( var);

    trajConfig.setTrajectoryId( trajId);
    return trajConfig;
  }
  public Date getStartDate()
  {
      double timeValue ;
      Calendar cal = Calendar.getInstance();
       
      timeValue =localNCD.findGlobalAttribute("start_time").getNumericValue().doubleValue();
      cal.setTimeInMillis((long)timeValue*1000);
      Date dd = getTime(localNCD);
     // long dl = dd.getTime();
      return dd;
  }

  public Date getEndDate()
  {
      double timeValue ;
      Calendar cal = Calendar.getInstance();
      timeValue =localNCD.findGlobalAttribute("stop_time").getNumericValue().doubleValue()
              - localNCD.findGlobalAttribute("start_time").getNumericValue().doubleValue();
      Date dd = getTime(localNCD);
      long dl = dd.getTime() + (long)timeValue;
      cal.setTimeInMillis(dl);

      return cal.getTime();
  }

  Date getTime( NetcdfDataset ds) {
    int year = ds.readAttributeInteger(null, "year", 0);
    int month = ds.readAttributeInteger(null, "month", 0);
    int dayOfMonth = ds.readAttributeInteger(null, "day", 0);
    int hourOfDay = ds.readAttributeInteger(null, "hour", 0);
    int minute = ds.readAttributeInteger(null, "minute", 0);
    int second = ds.readAttributeInteger(null, "second", 0);

    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    cal.clear();
    cal.set(year, month -1, dayOfMonth, hourOfDay, minute, second);
    return cal.getTime();
  }

  protected void setStartDate() { /* ToDo implement. */ }
  protected void setEndDate() { /* ToDo implement. */ }
    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new COSMICTrajectoryObsDataset( ncd);
  }
  public FeatureType getScientificDataType() { return FeatureType.TRAJECTORY; }

  public COSMICTrajectoryObsDataset() {}

  public COSMICTrajectoryObsDataset( NetcdfDataset ncd ) throws IOException
  {
    super( ncd);
    localNCD  = ncd;
    TrajectoryObsDatasetImpl.Config trajConfig = buildConfig( ncd);
    this.setTrajectoryInfo( trajConfig );
  }


}
