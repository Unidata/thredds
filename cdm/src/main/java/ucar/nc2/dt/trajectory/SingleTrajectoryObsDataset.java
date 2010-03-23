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
// $Id: SingleTrajectoryObsDataset.java 63 2006-07-12 21:50:51Z edavis $
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
 * NetcdfFile underneath that contains a single trajectory. The file
 * must have a single coordinate variable for time. The time dimension
 * may be UNLIMITED (if time is not UNLIMITED, there must be no UNLIMITED
 * dimension). The file must also have a latitude variable, a longitude
 * variable, and an elevation variable each on the time dimension only.
 * The data variables also must be on the time dimension but they can
 * also have other dimensions.
 * For instance:
 * <pre>
 * time( time)  - convertable to -> double
 * lat( time)   - convertable to -> double
 * lon( time)   - convertable to -> double
 * elev( time)  - convertable to -> double
 * var1( time[, dim#]*)
 * ...
 * varN( time[, dim#]*)
 * </pre>
 *
 * @deprecated use ucar.nc2.ft.*
 * @author edavis
 * @since 5 May 2005 10:12 -0600
 */
public class SingleTrajectoryObsDataset
        extends TypedDatasetImpl
        implements TrajectoryObsDataset
{
  protected String trajectoryId; // the Id String for the trajectory.
  protected int trajectoryNumPoint; // the num of points in the trajectory.
  protected HashMap trajectoryVarsMap; // a Map of all the TypedDataVariables in the trajectory keyed by their names.

  protected Dimension timeDim;
  protected Variable timeVar;
  protected Structure recordVar;
  protected Variable latVar;
  protected Variable lonVar;
  protected Variable elevVar;

  protected String timeVarUnitsString;

  protected double elevVarUnitsConversionFactor;

  protected TrajectoryObsDatatype trajectory;

  public SingleTrajectoryObsDataset() {}
  
  public SingleTrajectoryObsDataset( NetcdfDataset ncfile)
  {
    super( ncfile);
  }

  /** Setup needed for all SingleTrajectoryObsDatatypes. Can only be called once.
   *
   * Units of time varible must be udunits time units.
   * Units of latitude variable must be convertible to "degrees_north" by udunits.
   * Units of longitude variable must be convertible to "degrees_east" by udunits.
   * Units of altitude variable must be convertible to "meters" by udunits.
   *
   * @throws IllegalArgumentException if units of time, latitude, longitude, or altitude variables are not as required.
   * @throws IllegalStateException if this method has already been called.
   */
  public void setTrajectoryInfo( Config trajConfig )
          throws IOException
  {
    if ( timeDim != null )
      throw new IllegalStateException( "The setTrajectoryInfo() method can only be called once.");

    this.trajectoryId = trajConfig.getTrajectoryId();
    this.timeDim = trajConfig.getTimeDim();
    this.timeVar = trajConfig.getTimeVar();
    this.latVar = trajConfig.getLatVar();
    this.lonVar = trajConfig.getLonVar();
    this.elevVar = trajConfig.getElevVar();

    trajectoryNumPoint = this.timeDim.getLength();
    timeVarUnitsString = this.timeVar.findAttribute( "units" ).getStringValue();

    // Check that time, lat, lon, elev units are acceptable.
    if ( DateUnit.getStandardDate( timeVarUnitsString ) == null )
    {
      throw new IllegalArgumentException( "Units of time variable <" + timeVarUnitsString + "> not a date unit." );
    }
    String latVarUnitsString = this.latVar.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( latVarUnitsString, "degrees_north" ) )
    {
      throw new IllegalArgumentException( "Units of lat var <" + latVarUnitsString + "> not compatible with \"degrees_north\"." );
    }
    String lonVarUnitsString = this.lonVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( lonVarUnitsString, "degrees_east" ) )
    {
      throw new IllegalArgumentException( "Units of lon var <" + lonVarUnitsString + "> not compatible with \"degrees_east\"." );
    }
    String elevVarUnitsString = this.elevVar.findAttribute( "units" ).getStringValue();
    if ( !SimpleUnit.isCompatible( elevVarUnitsString, "meters" ) )
    {
      throw new IllegalArgumentException( "Units of elev var <" + elevVarUnitsString + "> not compatible with \"meters\"." );
    }

    try
    {
      elevVarUnitsConversionFactor = getMetersConversionFactor( elevVarUnitsString);
    }
    catch ( Exception e )
    {
      throw new IllegalArgumentException( "Exception on getMetersConversionFactor() for the units of elev var <" + elevVarUnitsString + ">." );
    }

    if ( this.ncfile.hasUnlimitedDimension() && this.ncfile.getUnlimitedDimension().equals( timeDim ) )
    {
      Object result = this.ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      if ((result != null) && (Boolean) result )
        this.recordVar = (Structure) this.ncfile.getRootGroup().findVariable( "record");
      else
        this.recordVar = new StructurePseudo( this.ncfile, null, "record", timeDim );
    } else {
      this.recordVar = new StructurePseudo( this.ncfile, null, "record", timeDim);
    }

    // @todo HACK, HACK, HACK - remove once addRecordStructure() deals with ncd attribute changes.
    Variable elevVarInRecVar = this.recordVar.findVariable( this.elevVar.getName());
    if ( ! elevVarUnitsString.equals( elevVarInRecVar.findAttribute( "units").getStringValue()))
    {
      elevVarInRecVar.addAttribute( new Attribute( "units", elevVarUnitsString));
    }

    trajectoryVarsMap = new HashMap();
    //for ( Iterator it = this.recordVar.getVariables().iterator(); it.hasNext(); )
    for ( Iterator it = this.ncfile.getRootGroup().getVariables().iterator(); it.hasNext(); )
    {
      Variable curVar = (Variable) it.next();
      if ( curVar.getRank() > 0 &&
           !curVar.equals( this.timeVar) &&
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

    trajectory = new SingleTrajectory( this.trajectoryId, trajectoryNumPoint,
                                       this.timeVar, timeVarUnitsString,
                                       this.latVar, this.lonVar, this.elevVar,
                                       dataVariables, trajectoryVarsMap);

    startDate = trajectory.getTime( 0);
    endDate = trajectory.getTime( trajectoryNumPoint - 1);

    ( (SingleTrajectory) trajectory).setStartDate( startDate );
    ( (SingleTrajectory) trajectory).setEndDate( endDate );
  }

  protected static double getMetersConversionFactor( String unitsString ) throws Exception
  {
    SimpleUnit unit = SimpleUnit.factoryWithExceptions( unitsString );
    return unit.convertTo( 1.0, SimpleUnit.meterUnit );
  }

  protected void setStartDate() {}

  protected void setEndDate() {}

  protected void setBoundingBox() {}

  public List getTrajectoryIds()
  {
    List l = new ArrayList();
    l.add( trajectoryId);
    return( l);
  }

  public List getTrajectories() // throws IOException;
  {
    List l = new ArrayList();
    l.add( trajectory);
    return( l);
  }

  public TrajectoryObsDatatype getTrajectory( String trajectoryId ) // throws IOException;
  {
    if ( ! trajectoryId.equals( this.trajectoryId)) return( null);
    return( trajectory);
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

  public boolean syncExtend()
  {
    if ( ! this.ncfile.hasUnlimitedDimension()) return false;
    try
    {
      if ( ! this.ncfile.syncExtend() ) return false;
    }
    catch ( IOException e )
    {
      return false;
    }

    // Update number of points in this TrajectoryObsDataset and in the child TrajectoryObsDatatype.
    int newNumPoints = this.timeDim.getLength();
    if ( this.trajectoryNumPoint >= newNumPoints )
      return false;
    this.trajectoryNumPoint = newNumPoints;
    ( (SingleTrajectory) this.trajectory).setNumPoints( this.trajectoryNumPoint);

    // Update end date in this TrajectoryObsDataset and in the child TrajectoryObsDatatype.
    try
    {
      endDate = trajectory.getTime( trajectoryNumPoint - 1 );
    }
    catch ( IOException e )
    {
      return false;
    }
    ( (SingleTrajectory) trajectory ).setEndDate( endDate );

    return true;
  }

  public static class Config
  {
    protected String trajectoryId; // the Id String for the trajectory.

    protected Dimension timeDim;
    protected Variable timeVar;
    protected Variable latVar;
    protected Variable lonVar;
    protected Variable elevVar;

    public Config() {}

    public Config( String trajectoryId, Dimension timeDim, Variable timeVar,
                   Variable latVar, Variable lonVar, Variable elevVar )
    {
      this.trajectoryId = trajectoryId;
      this.timeDim = timeDim;
      this.timeVar = timeVar;
      this.latVar = latVar;
      this.lonVar = lonVar;
      this.elevVar = elevVar;
    }

    public void setTrajectoryId( String trajectoryId )
    {
      this.trajectoryId = trajectoryId;
    }

    public void setTimeDim( Dimension timeDim )
    {
      this.timeDim = timeDim;
    }

    public void setTimeVar( Variable timeVar )
    {
      this.timeVar = timeVar;
    }

    public void setLatVar( Variable latVar )
    {
      this.latVar = latVar;
    }

    public void setLonVar( Variable lonVar )
    {
      this.lonVar = lonVar;
    }

    public void setElevVar( Variable elevVar )
    {
      this.elevVar = elevVar;
    }

    public String getTrajectoryId()
    {
      return trajectoryId;
    }

    public Dimension getTimeDim()
    {
      return timeDim;
    }

    public Variable getTimeVar()
    {
      return timeVar;
    }

    public Variable getLatVar()
    {
      return latVar;
    }

    public Variable getLonVar()
    {
      return lonVar;
    }

    public Variable getElevVar()
    {
      return elevVar;
    }
  }

  private class SingleTrajectory implements TrajectoryObsDatatype
  {

    private String id;
    private String description;
    private int numPoints;
    private Date startDate;
    private Date endDate;
    private String timeVarUnitsString;
    private Variable timeVar;
    private Variable latVar;
    private Variable lonVar;
    private Variable elevVar;
    private List variables;
    private HashMap variablesMap;

    //private Structure struct;


    private SingleTrajectory( String id, int numPoints,
                              Variable timeVar, String timeVarUnitsString,
                              Variable latVar, Variable lonVar, Variable elevVar,
                              List variables, HashMap variablesMap )
    {
      this.description = null;
      this.id = id;
      this.numPoints = numPoints;
      this.timeVarUnitsString = timeVarUnitsString;
      this.timeVar = timeVar;
      this.variables = variables;
      this.variablesMap = variablesMap;
      this.latVar = latVar;
      this.lonVar = lonVar;
      this.elevVar = elevVar;

      //this.struct = new Structure( ncfile, ncfile.getRootGroup(), null, "struct for building StructureDatas");
    }

    protected void setNumPoints( int numPoints )
    {
      this.numPoints = numPoints;
    }
    protected void setStartDate( Date startDate )
    {
      if ( this.startDate != null ) throw new IllegalStateException( "Can only call setStartDate() once." );
      this.startDate = startDate;
    }
    protected void setEndDate( Date endDate )
    {
      //if ( this.endDate != null ) throw new IllegalStateException( "Can only call setEndDate() once.");
      this.endDate = endDate;
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
        return array.getDouble( array.getIndex());
      }
      else if ( array instanceof ArrayFloat)
      {
        return array.getFloat( array.getIndex());
      }
      else
      {
        throw new IOException( "Elevation variable not float or double <" + array.getElementType().toString() + ">.");
      }
    }

    public StructureData getData( int point ) throws IOException, InvalidRangeException
    {
      return SingleTrajectoryObsDataset.this.recordVar.readStructure( point );
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
      List section = new ArrayList(1);
      section.add( range);
      return( latVar.read( section));
    }

    // @todo Make sure units are degrees_east
    public Array getLongitude( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      return( lonVar.read( section));
    }

    // @todo Make sure units are meters
    public Array getElevation( Range range ) throws IOException, InvalidRangeException
    {
      List section = new ArrayList(1);
      section.add( range);
      Array a = elevVar.read( section);
      if ( elevVarUnitsConversionFactor == 1.0) return( a);
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
      return( a);
    }

    public Array getData( Range range, String parameterName ) throws IOException, InvalidRangeException
    {
      Variable variable = ncfile.getRootGroup().findVariable( parameterName );
      int varRank = variable.getRank();
      int [] varShape = variable.getShape();
      List section = new ArrayList( varRank);
      section.add( range);
      for ( int i = 1; i < varRank; i++)
      {
        section.add( new Range( 0, varShape[i]-1 ) );
      }
      Array array = variable.read( section);
      if ( array.getShape()[0] == 1 )
      {
        return ( array.reduce( 0 ) );
      }
      else
      {
        return ( array );
      }
      //return( array.getShape()[0] == 1 ? array.reduce( 0 ) : array);
    }

    public DataIterator getDataIterator( int bufferSize ) throws IOException
    {
      return new PointDatatypeIterator( recordVar, bufferSize );
    }

    private class PointDatatypeIterator extends DatatypeIterator
    {
      protected Object makeDatatypeWithData( int recnum, StructureData sdata )
      {
        return new MyPointObsDatatype( recnum, sdata );
      }

      PointDatatypeIterator( Structure struct, int bufferSize )
      {
        super( struct, bufferSize );
      }
    }


    // PointObsDatatype implementation used by SingleTrajectory.
    private class MyPointObsDatatype implements PointObsDatatype
    {
      private int point;
      private StructureData sdata;
      private double time;
      private ucar.unidata.geoloc.EarthLocation earthLoc;

      private MyPointObsDatatype( int point) throws IOException
      {
        this.point = point;
        this.time = SingleTrajectory.this.getTimeValue( point);
        this.earthLoc = SingleTrajectory.this.getLocation( point);
      }

      private MyPointObsDatatype( int point, StructureData sdata )
      {
        this.point = point;
        this.sdata = sdata;
        this.time = sdata.convertScalarDouble( SingleTrajectory.this.timeVar.getName());
        this.earthLoc = new MyEarthLocation( sdata);
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
        if ( sdata != null ) return sdata;
        try {
          return( SingleTrajectory.this.getData( point));
        } catch (InvalidRangeException e) {
          throw new IllegalStateException( e.getMessage());
        }
      }
    }

    // EarthLocation implementation used by SingleTrajectory.
    private class MyEarthLocation extends ucar.unidata.geoloc.EarthLocationImpl
    {
      private double latitude;
      private double longitude;
      private double elevation;

      private MyEarthLocation( int point) throws IOException
      {
        this.latitude = SingleTrajectory.this.getLatitude( point);
        this.longitude = SingleTrajectory.this.getLongitude( point);
        this.elevation = SingleTrajectory.this.getElevation( point);
      }

      private MyEarthLocation( StructureData sdata )
      {
        this.latitude = sdata.convertScalarDouble( SingleTrajectory.this.latVar.getName() );
        this.longitude = sdata.convertScalarDouble( SingleTrajectory.this.lonVar.getName() );
        this.elevation = sdata.convertScalarDouble( SingleTrajectory.this.elevVar.getName() );
        if ( elevVarUnitsConversionFactor != 1.0 ) this.elevation *= elevVarUnitsConversionFactor;
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
      rank = super.getRank() - 1;
      int[] varShape = super.getShape();
      shape = new int[ varShape.length - 1];
      int trajDimIndex = v.findDimensionIndex( SingleTrajectoryObsDataset.this.timeDim.getName());
      for ( int i = 0, j = 0; i < varShape.length; i++)
      {
        if ( i == trajDimIndex) continue;
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
