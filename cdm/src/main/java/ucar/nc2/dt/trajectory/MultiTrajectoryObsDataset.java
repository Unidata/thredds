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
// $Id: MultiTrajectoryObsDataset.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.StructurePseudo;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dt.VariableSimpleSubclass;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Superclass for for implementations of TrajectoryObsDataset using a
 * NetcdfFile underneath that contains multiple trajectories.  The file
 * must have two coordinate variables one over time and one over the multiple
 * trajectories, the time dimension may be UNLIMITED (if time is not UNLIMITED,
 * there must be no UNLIMITED dimension). The file must also have a latitude
 * variable, a longitude variable, and an elevation variable each over the
 * time and trajectory dimension. All other trajectory variables must be on
 * the time and trajectory dimension with other dimensions as needed.
 * For instance:
 * <pre>
 * traj( traj)        - convertable to -> String
 * time( time)        - convertable to -> double
 * lat( time, traj)   - convertable to -> double
 * lon( time, traj)   - convertable to -> double
 * elev( time, traj)  - convertable to -> double
 * var1( time, traj[, dim#]*)
 * ...
 * varM( time, traj[, dim#]*)
 * </pre>
 *
 * @deprecated use ucar.nc2.ft.*
 * @author edavis
 * @since 13 May 2005 16:12 -0600
 */
public class MultiTrajectoryObsDataset
        extends TypedDatasetImpl
        implements TrajectoryObsDataset
{
  protected Dimension trajDim;
  protected Variable trajVar;
  protected Dimension timeDim;
  protected Variable timeVar;
  protected Structure recordVar = null;
  protected Variable latVar;
  protected Variable lonVar;
  protected Variable elevVar;

  protected String timeVarUnitsString;

  protected double elevVarUnitsConversionFactor;

  protected List trajectoryIds;
  protected List trajectories;
  protected HashMap trajectoriesMap;

  protected int trajectoryNumPoint;
  protected HashMap trajectoryVarsMap;

  public MultiTrajectoryObsDataset() {}
  public MultiTrajectoryObsDataset( NetcdfDataset ncfile)
  {
    super( ncfile);
  }

  /**
   * Setup needed for all MultiTrajectoryObsDatatypes.
   *
   * Units of time varible must be udunits time units.
   * Units of latitude variable must be convertible to "degrees_north" by udunits.
   * Units of longitude variable must be convertible to "degrees_east" by udunits.
   * Units of altitude variable must be convertible to "meters" by udunits.
   *
   * @throws IllegalArgumentException if units of time, latitude, longitude, or altitude variables are not as required.
   */
  public void setTrajectoryInfo( Dimension trajDim, Variable trajVar,
                                 Dimension timeDim, Variable timeVar,
                                 Variable latVar, Variable lonVar, Variable elevVar )
          throws IOException
  {
    this.trajDim = trajDim;
    this.trajVar = trajVar;
    this.timeDim = timeDim;
    this.timeVar = timeVar;
    this.latVar = latVar;
    this.lonVar = lonVar;
    this.elevVar = elevVar;

    trajectoryNumPoint = this.timeDim.getLength();
    timeVarUnitsString = this.timeVar.findAttribute( "units" ).getStringValue();

    // Check that time, lat, lon, elev units are acceptable.
    if ( DateUnit.getStandardDate( timeVarUnitsString ) == null )
      throw new IllegalArgumentException( "Units of time variable <" + timeVarUnitsString + "> not a date unit." );
    String latVarUnitsString = this.latVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( latVarUnitsString, "degrees_north" ) )
      throw new IllegalArgumentException( "Units of lat var <" + latVarUnitsString + "> not compatible with \"degrees_north\"." );
    String lonVarUnitsString = this.lonVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( lonVarUnitsString, "degrees_east" ) )
      throw new IllegalArgumentException( "Units of lon var <" + lonVarUnitsString + "> not compatible with \"degrees_east\"." );
    String elevVarUnitsString = this.elevVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( elevVarUnitsString, "meters" ) )
      throw new IllegalArgumentException( "Units of elev var <" + elevVarUnitsString + "> not compatible with \"meters\"." );

    try
    {
      elevVarUnitsConversionFactor = getMetersConversionFactor( elevVarUnitsString );
    }
    catch ( Exception e )
    {
      throw new IllegalArgumentException( "Exception on getMetersConversionFactor() for the units of elev var <" + elevVarUnitsString + ">." );
    }

    if ( this.ncfile.hasUnlimitedDimension() && this.ncfile.getUnlimitedDimension().equals( timeDim))
    {
      this.ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      this.recordVar = (Structure) this.ncfile.getRootGroup().findVariable( "record");
    } else {
      this.recordVar = new StructurePseudo( this.ncfile, null, "record", timeDim);
    }

    // @todo HACK, HACK, HACK - remove once addRecordStructure() deals with ncd attribute changes.
    Variable latVarInRecVar = this.recordVar.findVariable( this.latVar.getName() );
    Attribute latVarUnitsAtt = latVarInRecVar.findAttribute( "units");
    if ( latVarUnitsAtt != null && !latVarUnitsString.equals( latVarUnitsAtt.getStringValue() ) )
      latVarInRecVar.addAttribute( new Attribute( "units", latVarUnitsString ) );
    Variable lonVarInRecVar = this.recordVar.findVariable( this.lonVar.getName() );
    Attribute lonVarUnitsAtt = lonVarInRecVar.findAttribute( "units" );
    if ( lonVarUnitsAtt != null && !lonVarUnitsString.equals( lonVarUnitsAtt.getStringValue() ) )
      lonVarInRecVar.addAttribute( new Attribute( "units", lonVarUnitsString ) );
    Variable elevVarInRecVar = this.recordVar.findVariable( this.elevVar.getName() );
    Attribute elevVarUnitsAtt = elevVarInRecVar.findAttribute( "units" );
    if ( elevVarUnitsAtt != null && !elevVarUnitsString.equals( elevVarUnitsAtt.getStringValue() ) )
      elevVarInRecVar.addAttribute( new Attribute( "units", elevVarUnitsString ) );

    trajectoryVarsMap = new HashMap();
    for ( Iterator it = this.ncfile.getRootGroup().getVariables().iterator(); it.hasNext(); )
    {
      Variable curVar = (Variable) it.next();
      if ( curVar.getRank() >= 2 &&
         //  !curVar.equals( this.trajVar) && // These two are both one dimensional arrays
         //  !curVar.equals( this.timeVar) && //
           ! curVar.equals( this.latVar) &&
           ! curVar.equals( this.lonVar) &&
           ! curVar.equals( this.elevVar) &&
           ( this.recordVar == null ? true : ! curVar.equals( this.recordVar)))
      {
        MyTypedDataVariable typedVar = new MyTypedDataVariable( new VariableDS(  null, curVar, true ) );
        dataVariables.add( typedVar);
        trajectoryVarsMap.put( typedVar.getName(), typedVar);
      }
    }

    Range startPointRange = null;
    Range endPointRange = null;
    try
    {
      startPointRange = new Range( 0, 0);
      endPointRange = new Range( trajectoryNumPoint-1, trajectoryNumPoint-1);
    }
    catch ( InvalidRangeException e )
    {
      IOException ioe = new IOException( "Start or end point range invalid: " + e.getMessage());
      ioe.initCause( e);
      throw( ioe);
    }
    List section0 = new ArrayList(1);
    List section1 = new ArrayList(1);
    section0.add( startPointRange);
    section1.add( endPointRange);
    Array startTimeArray;
    Array endTimeArray;
    try
    {
      startTimeArray = this.timeVar.read( section0);
      endTimeArray = this.timeVar.read( section1);
    }
    catch ( InvalidRangeException e )
    {
      IOException ioe = new IOException( "Invalid range during read of start or end point: " + e.getMessage());
      ioe.initCause( e);
      throw( ioe);
    }

    String startTimeString;
    String endTimeString;
    if ( this.timeVar.getDataType().equals( DataType.DOUBLE))
    {
      startTimeString = startTimeArray.getDouble( startTimeArray.getIndex() ) + " " + timeVarUnitsString;
      endTimeString = endTimeArray.getDouble( endTimeArray.getIndex() ) + " " + timeVarUnitsString;
    }
    else if ( this.timeVar.getDataType().equals( DataType.FLOAT))
    {
      startTimeString = startTimeArray.getFloat( startTimeArray.getIndex() ) + " " + timeVarUnitsString;
      endTimeString = endTimeArray.getFloat( endTimeArray.getIndex() ) + " " + timeVarUnitsString;
    }
    else if ( this.timeVar.getDataType().equals( DataType.INT ) )
    {
      startTimeString = startTimeArray.getInt( startTimeArray.getIndex() ) + " " + timeVarUnitsString;
      endTimeString = endTimeArray.getInt( endTimeArray.getIndex() ) + " " + timeVarUnitsString;
    }
    else
    {
      String tmpMsg = "Time var <" + this.timeVar.getName() + "> is not a double, float, or integer <" + timeVar.getDataType().toString() + ">.";
      //log.error( tmpMsg );
      throw new IllegalArgumentException( tmpMsg);
    }

    startDate = DateUnit.getStandardDate( startTimeString );
    endDate = DateUnit.getStandardDate( endTimeString);

    trajectoryIds = new ArrayList();
    trajectories = new ArrayList();
    trajectoriesMap = new HashMap();
    Array trajArray = this.trajVar.read();
    Index index = trajArray.getIndex();
    for ( int i = 0; i < trajArray.getSize(); i++ )
    {
      String curTrajId;
      if ( this.trajVar.getDataType().equals( DataType.STRING ) )
      {
        curTrajId = (String) trajArray.getObject( index.set( i ) );
      }
      else if ( this.trajVar.getDataType().equals( DataType.DOUBLE ) )
      {
        curTrajId = String.valueOf( trajArray.getDouble( index.set( i ) ) );
      }
      else if ( this.trajVar.getDataType().equals( DataType.FLOAT ) )
      {
        curTrajId = String.valueOf( trajArray.getFloat( index.set( i ) ) );
      }
      else if ( this.trajVar.getDataType().equals( DataType.INT ) )
      {
        curTrajId = String.valueOf( trajArray.getInt( index.set( i ) ) );
      }
      else
      {
        String tmpMsg = "Trajectory var <" + this.trajVar.getName() + "> is not a string, double, float, or integer <" + this.trajVar.getDataType().toString() + ">.";
        //log.error( tmpMsg );
        throw new IllegalStateException( tmpMsg );
      }

      MultiTrajectory curTraj = new MultiTrajectory( curTrajId, i, trajectoryNumPoint, startDate, endDate,
                                                     this.trajVar, this.timeVar, timeVarUnitsString,
                                                     this.latVar, this.lonVar, this.elevVar,
                                                     dataVariables, trajectoryVarsMap );
      trajectoryIds.add( curTrajId);
      trajectories.add( curTraj);
      trajectoriesMap.put( curTrajId, curTraj );
    }
  }

  protected static double getMetersConversionFactor( String unitsString ) throws Exception
{
    SimpleUnit unit = SimpleUnit.factoryWithExceptions( unitsString );
    return unit.convertTo( 1.0, SimpleUnit.meterUnit );
  }

  public boolean syncExtend()
  {
    return false;
  }

  protected void setStartDate() {}

  protected void setEndDate() {}

  protected void setBoundingBox() {}

  public List getTrajectoryIds()
  {
    return( trajectoryIds);
  }

  public List getTrajectories() // throws IOException;
  {
    return( trajectories);
  }

  public TrajectoryObsDatatype getTrajectory( String trajectoryId ) // throws IOException;
  {
    if ( trajectoryId == null ) return( null);
    return (TrajectoryObsDatatype) trajectoriesMap.get( trajectoryId);
  }

  public String getDetailInfo()
  {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append( "TrajectoryObsDataset\n" );
    sbuff.append( "  adapter   = " + getClass().getName() + "\n" );
    sbuff.append( "  trajectories:" + "\n" );
    for ( Iterator it = this.getTrajectoryIds().iterator(); it.hasNext(); )
    {
      sbuff.append( "      " + (String) it.next() + "\n" );
    }
    sbuff.append( super.getDetailInfo() );

    return sbuff.toString();
  }

  private class MultiTrajectory implements TrajectoryObsDatatype
  {

    private String id;
    private int trajNum;
    private String description;
    private int numPoints;
    private Date startDate;
    private Date endDate;
    private String timeVarUnitsString;
    private Variable trajVar;
    private Variable timeVar;
    private Variable latVar;
    private Variable lonVar;
    private Variable elevVar;
    private List variables;
    private HashMap variablesMap;

    private Range trajPointRange; // Point range on trajectory variable for this trajectory.


    private MultiTrajectory( String id, int idNum, int numPoints,
                              Date startDate, Date endDate, Variable trajVar,
                              Variable timeVar, String timeVarUnitsString,
                              Variable latVar, Variable lonVar, Variable elevVar,
                              List variables, HashMap variablesMap )
    {
      this.description = null;
      this.id = id;
      this.trajNum = idNum;
      this.numPoints = numPoints;
      this.startDate = startDate;
      this.endDate = endDate;
      this.timeVarUnitsString = timeVarUnitsString;
      this.timeVar = timeVar;
      this.trajVar = trajVar;
      this.variables = variables;
      this.variablesMap = variablesMap;
      this.latVar = latVar;
      this.lonVar = lonVar;
      this.elevVar = elevVar;

      // Calculate the point range for this trajectory in the trajectory dimension.
      try
      {
        trajPointRange = new Range( this.trajNum, this.trajNum );
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( e.getMessage() );
        iae.initCause( e );
        throw( iae );
      }
    }

    public String getId()
    {
      return( id);
    }

    public String getDescription()
    {
      return( description);
    }

    public int getNumberPoints()
    {
      return( numPoints);
    }

    public List getDataVariables()
    {
      return ( variables );
    }

    public VariableSimpleIF getDataVariable( String name )
    {
      return ( (VariableSimpleIF) variablesMap.get( name ) );
    }

    public PointObsDatatype getPointObsData( int point ) throws IOException
    {
      return( new MyPointObsDatatype( point));
    }

    public Date getStartDate()
    {
      return( startDate);
    }

    public Date getEndDate()
    {
      return( endDate);
    }

    public LatLonRect getBoundingBox()
    {
      return null;
    }

    public Date getTime( int point ) throws IOException
    {
      return ( DateUnit.getStandardDate( getTimeValue( point ) + " " + timeVarUnitsString ) );
    }

    public ucar.unidata.geoloc.EarthLocation getLocation( int point ) throws IOException
    {
      return ( new MyEarthLocation( point ) );
    }

    public String getTimeUnitsIdentifier()
    {
      return( timeVarUnitsString);
    }

    public double getTimeValue( int point ) throws IOException
    {
      Array array = null;
      try
      {
        array = getTime( this.getPointRange( point ) );
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + ( this.getNumberPoints() - 1 ) + ">: " + e.getMessage() );
        iae.initCause( e );
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else if ( array instanceof ArrayInt )
      {
        return ( array.getInt( array.getIndex() ) );
      }
      else
      {
        throw new IOException( "Time variable not float, double, or integer <" + array.getElementType().toString() + ">.");
      }
    }

    // @todo Make sure units are degrees_north
    public double getLatitude( int point ) throws IOException // required, units degrees_north
    {
      Array array = null;
      try
      {
        array = getLatitude( this.getPointRange( point));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else
      {
        throw new IOException( "Latitude variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    // @todo Make sure units are degrees_east
    public double getLongitude( int point ) throws IOException // required, units degrees_east
    {
      Array array = null;
      try
      {
        array = getLongitude( this.getPointRange( point));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else
      {
        throw new IOException( "Longitude variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    // @todo Make sure units are meters
    public double getElevation( int point ) throws IOException // optional; units meters;  missing = NaN.
    {
      Array array = null;
      try
      {
        array = getElevation( this.getPointRange( point));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
      if ( array instanceof ArrayDouble)
      {
        return( array.getDouble( array.getIndex()));
      }
      else if ( array instanceof ArrayFloat)
      {
        return( array.getFloat( array.getIndex()));
      }
      else
      {
        throw new IOException( "Elevation variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    public StructureData getData( int point ) throws IOException, InvalidRangeException
    {
      return MultiTrajectoryObsDataset.this.recordVar.readStructure( point );
    }

    public Array getData( int point, String parameterName ) throws IOException
    {
      try
      {
        return( getData( this.getPointRange( point), parameterName));
      }
      catch ( InvalidRangeException e )
      {
        IllegalArgumentException iae = new IllegalArgumentException( "Point <" + point + "> not in valid range <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        iae.initCause( e);
        throw iae;
      }
    }

    public Range getFullRange()
    {
      Range range = null;
      try
      {
        range = new Range(0, this.getNumberPoints()-1);
      }
      catch ( InvalidRangeException e )
      {
        IllegalStateException ise = new IllegalStateException( "Full trajectory range invalid <0, " + (this.getNumberPoints()-1) + ">: " + e.getMessage());
        ise.initCause( e);
        throw( ise);
      }
      return range;
    }

    public Range getPointRange( int point ) throws InvalidRangeException
    {
      if ( point >= this.getNumberPoints()) throw new InvalidRangeException( "Point <" + point + "> not in acceptible range <0, " + (this.getNumberPoints()-1) + ">.");
      return( new Range(point, point));
    }

    public Range getRange( int start, int end, int stride ) throws InvalidRangeException
    {
      if ( end >= this.getNumberPoints()) throw new InvalidRangeException( "End point <" + end + "> not in acceptible range <0, " + (this.getNumberPoints()-1) + ">.");
      return( new Range( start, end, stride));
    }

    public Array getTime( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      return( timeVar.read( section));
    }

    // @todo Make sure units are degrees_north
    public Array getLatitude( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(2);
      section.add( range);
      section.add( trajPointRange);
      return( latVar.read( section).reduce());
    }

    // @todo Make sure units are degrees_east
    public Array getLongitude( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(2);
      section.add( range);
      section.add( trajPointRange );
      return( lonVar.read( section));
    }

    // @todo Make sure units are meters
    public Array getElevation( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(2);
      section.add( range);
      section.add( trajPointRange );
      Array a = elevVar.read( section);
      if ( elevVarUnitsConversionFactor == 1.0 ) return ( a );
      for ( IndexIterator it = a.getIndexIterator(); it.hasNext(); )
      {
        if ( elevVar.getDataType() == DataType.DOUBLE )
        {
          double val = it.getDoubleNext();
          it.setDoubleCurrent( val * elevVarUnitsConversionFactor );
        }
        else if ( elevVar.getDataType() == DataType.FLOAT )
        {
          float val = it.getFloatNext();
          it.setFloatCurrent( (float) ( val * elevVarUnitsConversionFactor ) );
        }
        else if ( elevVar.getDataType() == DataType.INT )
        {
          int val = it.getIntNext();
          it.setIntCurrent( (int) ( val * elevVarUnitsConversionFactor ) );
        }
        else if ( elevVar.getDataType() == DataType.LONG )
        {
          long val = it.getLongNext();
          it.setLongCurrent( (long) ( val * elevVarUnitsConversionFactor ) );
        }
        else
        {
          throw new IllegalStateException( "Elevation variable type <" + elevVar.getDataType().toString() + "> not double, float, int, or long." );
        }
      }
      return a;
    }

    public Array getData( Range range, String parameterName ) throws IOException, InvalidRangeException
    {
      Variable variable = ncfile.getRootGroup().findVariable( parameterName );
      int varRank = variable.getRank();
      int [] varShape = variable.getShape();
      List section = new ArrayList( varRank);
      section.add( range);
      section.add( trajPointRange );
      for ( int i = 2; i < varRank; i++)
      {
        section.add( new Range( 0, varShape[i]-1 ) );
      }
      Array array = variable.read( section);
      array = array.reduce(1); // Reduce the trajectory dimension.
      if ( array.getShape()[0] == 1)
        return( array.reduce( 0)); // Reduce the time dimension, if a point was requested.
      else
        return( array);
    }

    public DataIterator getDataIterator( int bufferSize ) throws IOException
    {
      return null; 
      //return new DataIterator( recordVar, bufferSize );
    }

//    private class DataIterator extends DatatypeIterator
//    {
//      protected Object makeDatatypeWithData( int recnum, StructureData sdata )
//      {
//        return new MyPointObsDatatype( recnum, sdata );
//      }
//
//      DataIterator( Structure struct, int bufferSize )
//      {
//        super( struct, bufferSize );
//      }
//    }

    // PointObsDatatype implementation used by MultiTrajectory.
    private class MyPointObsDatatype implements PointObsDatatype
    {
      private int point;
      private double time;
      private ucar.unidata.geoloc.EarthLocation earthLoc;

      private MyPointObsDatatype( int point) throws IOException
      {
        this.point = point;
        this.time = MultiTrajectory.this.getTimeValue( point);
        this.earthLoc = MultiTrajectory.this.getLocation( point);
      }

      public double getNominalTime()
      {
        return ( this.time );
      }

      public double getObservationTime()
      {
        return ( this.time );
      }

      public Date getNominalTimeAsDate() {
        String dateStr = getNominalTime() + " " + timeVarUnitsString;
        return DateUnit.getStandardDate( dateStr );
      }

      public Date getObservationTimeAsDate() {
        String dateStr = getObservationTime() + " " + timeVarUnitsString;
        return DateUnit.getStandardDate( dateStr );
      }

      public ucar.unidata.geoloc.EarthLocation getLocation()
      {
        return( this.earthLoc);
      }

      public StructureData getData() throws IOException
      {
        try {
          return( MultiTrajectory.this.getData( point));
        } catch (InvalidRangeException e) {
          throw new IllegalStateException( e.getMessage());
        }
      }
    }

    // EarthLocation implementation used by MultiTrajectory.
    private class MyEarthLocation extends ucar.unidata.geoloc.EarthLocationImpl
    {
      private double latitude;
      private double longitude;
      private double elevation;

      private MyEarthLocation( int point) throws IOException
      {
        this.latitude = MultiTrajectory.this.getLatitude( point);
        this.longitude = MultiTrajectory.this.getLongitude( point);
        this.elevation = MultiTrajectory.this.getElevation( point);
      }

      public double getLatitude()
      {
        return( this.latitude);
      }

      public double getLongitude()
      {
        return( this.longitude);
      }

      public double getAltitude()
      {
        return( this.elevation);
      }
    }
  }

  private class MyTypedDataVariable extends VariableSimpleSubclass
  {
    private int rank;
    private int[] shape;
    private MyTypedDataVariable( VariableDS v )
    {
      super( v );

      // Calculate the rank and shape of the variable, removing trajectory dimesion.
      rank = super.getRank();
      if ( timeDim != null ) rank--;
      if ( trajDim != null ) rank--;
      shape = new int[rank];
      int[] varShape = super.getShape();
      int trajDimIndex = v.findDimensionIndex( MultiTrajectoryObsDataset.this.trajDim.getName());
      int timeDimIndex = v.findDimensionIndex( MultiTrajectoryObsDataset.this.timeDim.getName());
      for ( int j = 0, i = 0; i < varShape.length; i++)
      {
        if ( i == trajDimIndex) continue;
        if ( i == timeDimIndex) continue;
        shape[ j++] = varShape[ i];
      }
    }

    public int getRank()
    {
      return( rank);
    }

    public int[] getShape()
    {
      return( shape);
    }
  }
}
