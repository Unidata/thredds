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
package ucar.nc2.dt.trajectory;

import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.point.UnidataObsDatasetHelper;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.util.*;

/**
 * Handle trajectory data files that follow the
 * Unidata Observation Dataset convention version 1.0.
 *
 * Documentation on this convention is available at
 * http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html
 *
 * @author edavis
 * @since 2006-11-17T17:26:14-0700
 */
public class UnidataTrajectoryObsDataset extends SingleTrajectoryObsDataset  implements TypedDatasetFactoryIF
{
  private String timeDimName;
  private String timeVarName;
  private String latVarName;
  private String lonVarName;
  private String elevVarName;

  static public boolean isValidFile( NetcdfFile ds)
  {
    Attribute cdmDtAtt = ds.findGlobalAttributeIgnoreCase( "cdm_data_type");
    if ( cdmDtAtt == null )
      cdmDtAtt = ds.findGlobalAttributeIgnoreCase( "cdm_datatype");
    if ( cdmDtAtt == null ) return false;
    if ( ! cdmDtAtt.isString() ) return false;

    String cdmDtString = cdmDtAtt.getStringValue();
    if ( cdmDtString == null ) return false;
    if ( ! cdmDtString.equalsIgnoreCase( FeatureType.TRAJECTORY.toString() ))
      return false;

    Attribute conventionsAtt = ds.findGlobalAttributeIgnoreCase( "Conventions");
    if ( conventionsAtt == null) return( false);
    if ( ! conventionsAtt.isString()) return( false);
    String convString = conventionsAtt.getStringValue();

    StringTokenizer stoke = new StringTokenizer( convString, "," );
    while ( stoke.hasMoreTokens() )
    {
      String toke = stoke.nextToken().trim();
      if ( toke.equalsIgnoreCase( "Unidata Observation Dataset v1.0" ) )
        return true;
    }

    return false;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException
  {
    return new UnidataTrajectoryObsDataset( ncd);
  }
  public FeatureType getScientificDataType() { return FeatureType.TRAJECTORY; }

  public UnidataTrajectoryObsDataset() {}

  public UnidataTrajectoryObsDataset( NetcdfDataset ncd) throws IOException
  {
    super( ncd );

    // coordinate variables
    latVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Lat );
    lonVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Lon );
    timeVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Time );
    elevVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Height );

    if ( latVar == null )
      throw new IllegalStateException( "Missing latitude variable" );
    if ( lonVar == null )
      throw new IllegalStateException( "Missing longitude coordinate variable" );
    if ( timeVar == null )
      throw new IllegalStateException( "Missing time coordinate variable" );
    if ( elevVar == null )
      throw new IllegalStateException( "Missing height coordinate variable" );

    timeDimName = timeVar.getDimension(0).getName();
    timeVarName = timeVar.getName();
    latVarName = latVar.getName();
    lonVarName = lonVar.getName();
    elevVarName = elevVar.getName();


    Config trajConfig = new Config( "1Hz data",
                                    ncd.getRootGroup().findDimension( timeDimName ),
                                    ncd.getRootGroup().findVariable( timeVarName ),
                                    ncd.getRootGroup().findVariable( latVarName ),
                                    ncd.getRootGroup().findVariable( lonVarName ),
                                    ncd.getRootGroup().findVariable( elevVarName ));
    this.setTrajectoryInfo( trajConfig );

  }
}
