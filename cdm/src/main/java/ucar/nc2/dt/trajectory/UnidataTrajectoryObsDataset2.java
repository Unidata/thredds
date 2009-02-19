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

import ucar.nc2.dt.*;
import ucar.nc2.dt.point.UnidataObsDatasetHelper;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

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
public class UnidataTrajectoryObsDataset2
        extends TypedDatasetImpl
        implements TrajectoryObsDataset,
                   TypedDatasetFactoryIF
{
  protected Variable trajVar;
  protected Dimension trajDim;
  protected Variable timeVar;
  protected Dimension timeDim;
  protected Structure recordVar;
  protected Variable latVar;
  protected Variable lonVar;
  protected Variable elevVar;

  protected String trajDimName;
  protected String trajVarName;
  protected String timeDimName;
  protected String timeVarName;
  protected String latVarName;
  protected String lonVarName;
  protected String elevVarName;

  protected boolean isMultiTrajStructure;
  protected boolean isTimeDimensionFirst;
  protected TrajectoryObsDataset backingTraj;

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

  public UnidataTrajectoryObsDataset2() {}

  public UnidataTrajectoryObsDataset2( NetcdfDataset ncd) throws IOException
  {
    super( ncd );

    // Find the time coordinate variable.
    this.timeVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Time );
    if ( this.timeVar == null )
      throw new IllegalArgumentException( "Dataset has no time coordinate variable." );
    int timeVarNumOfDims = this.timeVar.getDimensions().size();
    if ( timeVarNumOfDims != 1 )
      throw new IllegalArgumentException( "Dataset time variable does not have exactly one (1) dimension [" + timeVarNumOfDims + "]." );
    this.timeVarName = this.timeVar.getName();
    this.timeDim = this.timeVar.getDimension( 0 );
    this.timeDimName = this.timeDim.getName();

    // Find the latitude or GeoY coordinate variable.
    this.latVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Lat );
//    if ( this.latVar == null )
//      this.latVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.GeoY );
    if ( this.latVar == null )
      throw new IllegalArgumentException( "Dataset has no Latitude/GeoY variable." );
    List<Dimension> latVarDimList = this.latVar.getDimensions();
    int latVarNumOfDims = latVarDimList.size();
    if ( latVarNumOfDims == 1 )
      this.isMultiTrajStructure = false;
    if ( latVarNumOfDims == 2 )
    {
      this.isMultiTrajStructure = true;
      // Lat, lon, and alt variables have time and trajectory dimesions.
      if ( latVarDimList.get( 0 ).equals( this.timeDim) )
      {
        this.isTimeDimensionFirst = true;
        this.trajDim = latVarDimList.get( 1 );
        this.trajDimName = this.trajDim.getName();
      }
      else
      {
        if ( ! latVarDimList.get( 1 ).equals( this.timeDim) )
          throw new IllegalArgumentException( "Dataset Latitude/GeoY variable has no time dimension. ");
        this.isTimeDimensionFirst = false;
        this.trajDim = latVarDimList.get( 0 );
        this.trajDimName = this.trajDim.getName();
      }
    }
    else
      throw new IllegalArgumentException( "Dataset Latitude/GeoY variable does not have 1 or 2 dimensions [" + latVarNumOfDims + "]." );

    // If multi-trajectory structure, find trajectory coordinate variable.
    if ( this.isMultiTrajStructure )
    {
      this.trajVarName = this.trajDimName;
      this.trajVar = ncd.findTopVariable( this.trajVarName );
    }


    ////////////////////////
    // Find the longitude or GeoX coordinate variable.
    lonVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Lon );
//    if ( lonVar == null )
//      lonVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.GeoX );
    if ( lonVar == null )
      throw new IllegalArgumentException( "Missing Longitude/GeoX coordinate variable." );

    // Find the vertical height coordinate variable (may be null).
    elevVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Height );

    timeDimName = timeVar.getDimension(0).getName();
    timeVarName = timeVar.getName();
    latVarName = latVar.getName();
    lonVarName = lonVar.getName();
    elevVarName = elevVar != null ? elevVar.getName() : null;



    if ( ! isMultiTrajStructure )
    {
      this.backingTraj = new SingleTrajectoryObsDataset( ncd);
      SingleTrajectoryObsDataset.Config trajConfig =
              new SingleTrajectoryObsDataset.Config(
                      "1Hz data",
                      ncd.getRootGroup().findDimension( timeDimName ),
                      ncd.getRootGroup().findVariable( timeVarName ),
                      ncd.getRootGroup().findVariable( latVarName ),
                      ncd.getRootGroup().findVariable( lonVarName ),
                      ncd.getRootGroup().findVariable( elevVarName ));
      ((SingleTrajectoryObsDataset) this.backingTraj).setTrajectoryInfo( trajConfig );
    }
    else
    {
      this.backingTraj = new MultiTrajectoryObsDataset( ncd );

      ( (MultiTrajectoryObsDataset) this.backingTraj).setTrajectoryInfo( ncfile.getRootGroup().findDimension( trajDimName ),
                              ncfile.getRootGroup().findVariable( trajVarName ),
                              ncfile.getRootGroup().findDimension( timeDimName ),
                              ncfile.getRootGroup().findVariable( timeVarName ),
                              ncfile.getRootGroup().findVariable( latVarName ),
                              ncfile.getRootGroup().findVariable( lonVarName ),
                              ncfile.getRootGroup().findVariable( elevVarName ) );

    }

  }

  public String getDetailInfo()
  {
    return backingTraj.getDetailInfo();
  }

  public String getTitle()
  {
    return backingTraj.getTitle();
  }

  public String getDescription()
  {
    return backingTraj.getDescription();
  }

  public String getLocation()
  {
    return backingTraj.getLocationURI();
  }

  protected void setStartDate() { /* ToDo implement. */ }

  public Date getStartDate()
  {
    return backingTraj.getStartDate();
  }

  protected void setEndDate() { /* ToDo implement. */ }

  public Date getEndDate()
  {
    return backingTraj.getEndDate();
  }

  protected void setBoundingBox() { /* ToDo implement. */ }

  public LatLonRect getBoundingBox()
  {
    return backingTraj.getBoundingBox();
  }

  public List<Attribute> getGlobalAttributes()
  {
    return backingTraj.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase( String name )
  {
    return backingTraj.findGlobalAttributeIgnoreCase( name );
  }

  public List<VariableSimpleIF> getDataVariables()
  {
    return backingTraj.getDataVariables();
  }

  public VariableSimpleIF getDataVariable( String shortName )
  {
    return backingTraj.getDataVariable( shortName );
  }

  public NetcdfFile getNetcdfFile()
  {
    return backingTraj.getNetcdfFile();
  }

  public void close()
          throws IOException
  {
    backingTraj.close();
  }

  public List<String> getTrajectoryIds()
  {
    return backingTraj.getTrajectoryIds();
  }

  public List getTrajectories()
  {
    return backingTraj.getTrajectories();
  }

  public TrajectoryObsDatatype getTrajectory( String trajectoryId )
  {
    return backingTraj.getTrajectory( trajectoryId );
  }

  public boolean syncExtend()
  {
    return backingTraj.syncExtend();
  }
}