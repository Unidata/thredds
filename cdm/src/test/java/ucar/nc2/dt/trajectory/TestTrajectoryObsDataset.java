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
// $Id: TestTrajectoryObsDataset.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.TestAll;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.DataIterator;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Test all TrajectoryObsDataset implementations.
 *
 * @author edavis
 * @since Feb 23, 2005T4:23:04 PM
 */
public class TestTrajectoryObsDataset extends TestCase
{
  // @todo Use properties or env vars to override default values.
  private static String testDataDir = TestAll.cdmLocalTestDataDir;
  private static String remoteTestDataDir = TestAll.testdataDir;

  public TestTrajectoryObsDataset( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public static String getTestDataDir() { return( testDataDir); }
  public static String getRemoteTestDataDir() { return( remoteTestDataDir); }

  public static Test suite ( ) {
    TestSuite suite= new TestSuite();
    // suite.addTestSuite( TestRafTrajectoryObsDataset.class );
    suite.addTestSuite( TestSimpleTrajectoryObsDataset.class );
    suite.addTestSuite( TestFloat10TrajectoryObsDataset.class );
    suite.addTestSuite( TestARMTrajectoryObsDataset.class );

    return suite;
  }

  // @todo Split this test and the TestDatasetInfo into smaller chunks?
  static void testTrajInfo( TrajectoryObsDataset trajDs, TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo) throws IOException {
    // Check TypedDataset stuff.
    assertTrue( "Title <" + trajDs.getTitle() + "> is not as expected <" + trajDsInfo.getTitle() + ">.",
                (trajDsInfo.getTitle() == null
                    ? trajDs.getTitle() == null
                    : trajDsInfo.getTitle().equals( trajDs.getTitle())) );
    assertTrue( "Description <" + trajDs.getDescription() + "> is not as expected <" + trajDsInfo.getDescription() + ">.",
                ( trajDsInfo.getDescription() == null
                  ? trajDs.getDescription() == null
                  : trajDsInfo.getDescription().equals( trajDs.getDescription() ) ) );
    assertTrue( "Dataset location URI <" + trajDs.getLocationURI() + "> not as expected <" + trajDsInfo.getLocationURI() + ">.",
                trajDsInfo.getLocationURI().equals( trajDs.getLocationURI() ) );
    assertTrue( "Start date <" + trajDs.getStartDate() + " - " + trajDs.getStartDate().getTime() + "> not as expected <" + trajDsInfo.getStartDateLong() + ">.",
                trajDs.getStartDate().getTime() == trajDsInfo.getStartDateLong() );
    assertTrue( "End date <" + trajDs.getEndDate() + " - " + trajDs.getEndDate().getTime() + "> not as expected <" + trajDsInfo.getEndDateLong() + ">.",
                trajDs.getEndDate().getTime() == trajDsInfo.getEndDateLong() );
    assertTrue( "Bounding box <" + trajDs.getBoundingBox() + "> not as expteced <" + trajDsInfo.getBb() + ">.",
                ( trajDsInfo.getBb() == null
                    ? trajDs.getBoundingBox() == null
                    : trajDsInfo.getBb().equals( trajDs.getBoundingBox() ) ) );

    // Check global attributes from TypedDataset.
    List globalAtts = trajDs.getGlobalAttributes();
    assertTrue( "Global attributes list is null.",
                globalAtts != null );
    assertTrue( "Number of global attributes <" + globalAtts.size() + "> not as expected <" + trajDsInfo.getNumGlobalAtts() + ">.",
                globalAtts.size() == trajDsInfo.getNumGlobalAtts() );

    Attribute gatt = trajDs.findGlobalAttributeIgnoreCase( trajDsInfo.getExampGlobalAttName() );
    assertTrue( "Global attribute \"" + trajDsInfo.getExampGlobalAttName() + "\" <" + gatt.getStringValue() + "> does not contain expected substring <" + trajDsInfo.getExampGlobalAttValSubstring() + ">.",
                gatt.getStringValue().indexOf( trajDsInfo.getExampGlobalAttValSubstring() ) != -1 );

    // Check data variables from TypedDataset.
    List dataVars = trajDs.getDataVariables();
    assertTrue( "Data vars list is null.",
                dataVars != null );
    assertTrue( "Size of data vars list <" + dataVars.size() + "> not as expected <" + trajDsInfo.getNumVars() + ">.",
                dataVars.size() == trajDsInfo.getNumVars() );
    VariableSimpleIF tdv = trajDs.getDataVariable( trajDsInfo.getExampleVarName() );
    assertTrue( "Variable \"" + trajDsInfo.getExampleVarName() + "\" not found.",
                tdv != null );
    assertTrue( "Variable name <" + tdv.getName() + "> not as expected <" + trajDsInfo.getExampleVarName() + ">.",
                tdv.getName().equals( trajDsInfo.getExampleVarName() ) );
    assertTrue( "Variable description <" + tdv.getDescription() + "> not as expected <" + trajDsInfo.getExampleVarDescription() + ">.",
                tdv.getDescription().equals( trajDsInfo.getExampleVarDescription() ) );
    assertTrue( "Variable units <" + tdv.getUnitsString() + "> not convertable to <" + trajDsInfo.getExampleVarUnitsString() + ">.",
                tdv.getUnitsString().equals( trajDsInfo.getExampleVarUnitsString() ) ||
                SimpleUnit.isCompatible( tdv.getUnitsString(), trajDsInfo.getExampleVarUnitsString() ) );
    assertTrue( "Variable rank <" + tdv.getRank() + "> not as expected <" + trajDsInfo.getExampleVarRank() + ">.",
                tdv.getRank() == trajDsInfo.getExampleVarRank() );
    assertTrue( "Variable shape <" + toStringIntArray( tdv.getShape() ) + "> not as expected <" + toStringIntArray( trajDsInfo.getExampleVarShape() ) + ">.",
                compareIntArray( tdv.getShape(), trajDsInfo.getExampleVarShape() ) );
    assertTrue( "Variable data type <" + tdv.getDataType() + "> not as expected <" + trajDsInfo.getExampleVarDataType() + ">.",
                tdv.getDataType().equals( DataType.getType( trajDsInfo.getExampleVarDataType()) ) );
    assertTrue( "Num variable attributes <" + tdv.getAttributes().size() + "> not as expected <" + trajDsInfo.getExampleVarNumAtts() + ">.",
                tdv.getAttributes().size() == trajDsInfo.getExampleVarNumAtts() );

    // Check the underlying nc file.
    assertTrue( "Underlying netCDF file <" + trajDs.getNetcdfFile().getLocation() + "> not as expected <" + trajDsInfo.getLocationURI() + ">.",
                trajDs.getNetcdfFile().getLocation().equals( trajDsInfo.getLocationURI() ) );

    // Check trajectory stuff.
    List trajNames = trajDs.getTrajectoryIds();
    assertTrue( "Number of trajectory names <" + trajNames.size() + "> more than expected <" + trajDsInfo.getNumTrajs() + ">.",
                trajNames.size() == trajDsInfo.getNumTrajs() );
    TrajectoryObsDatatype traj1 = trajDs.getTrajectory( trajDsInfo.getExTrajId() );
    assertTrue( "Trajectory name <" + traj1.getId() + "> not as expected <" + trajDsInfo.getExTrajId() + ">.",
                ( trajDsInfo.getExTrajId() == null
                  ? traj1.getId() == null
                  : trajDsInfo.getExTrajId().equals( traj1.getId() ) ) );

    assertTrue( "Trajectory description <" + traj1.getDescription() + "> not as expected <" + trajDsInfo.getExTrajDesc() + ">.",
                ( trajDsInfo.getExTrajDesc() == null
                  ? traj1.getDescription() == null
                  : trajDsInfo.getExTrajDesc().equals( traj1.getDescription() ) ) );

    assertTrue( "Number of points in trajectory <" + traj1.getNumberPoints() + "> not as expected <" + trajDsInfo.getExTrajNumPoints() + ">.",
                traj1.getNumberPoints() == trajDsInfo.getExTrajNumPoints() );

    // Check start and end values of time, lat, lon, and elev.
    Date startDate = null, endDate = null;
    double startLat = 0, startLon = 0, startElev = 0;
    double endLat = 0, endLon = 0, endElev = 0;

    try
    {
      startDate = traj1.getTime( 0 );
      startLat = traj1.getLatitude( 0 );
      startLon = traj1.getLongitude( 0 );
      startElev = traj1.getElevation( 0 );
      endDate = traj1.getTime( traj1.getNumberPoints() - 1 );
      endLat = traj1.getLatitude( traj1.getNumberPoints() - 1 );
      endLon = traj1.getLongitude( traj1.getNumberPoints() - 1 );
      endElev = traj1.getElevation( traj1.getNumberPoints() - 1 );
    }
    catch ( IOException e )
    {
      assertTrue( "Couldn't read first/last time, lat, lon, elev from trajectory.", false );
    }
    assertTrue( "Traj start date <" + startDate + " - " + startDate.getTime() + "> not as expected <" + trajDsInfo.getStartDateLong() + ">.",
                startDate.getTime() == trajDsInfo.getStartDateLong() );
    assertTrue( "Traj end date <" + endDate + " - " + endDate.getTime() + "> not as expected <" + trajDsInfo.getEndDateLong() + ">.",
                endDate.getTime() == trajDsInfo.getEndDateLong() );

    assertTrue( "Start latitude <" + startLat + "> not as expected <" + trajDsInfo.getExampleTrajStartLat() + ">.",
                doubleWithinEpsilon( startLat, trajDsInfo.getExampleTrajStartLat(), 0.0001 ) );
    assertTrue( "End latitude <" + endLat + "> not as expected <" + trajDsInfo.getExampleTrajEndLat() + ">.",
                doubleWithinEpsilon( endLat, trajDsInfo.getExampleTrajEndLat(), 0.0001 ) );

    assertTrue( "Start longitude <" + startLon + "> not as expected <" + trajDsInfo.getExampleTrajStartLon() + ">.",
                doubleWithinEpsilon( startLon, trajDsInfo.getExampleTrajStartLon(), 0.0001 ) );
    assertTrue( "End longitude <" + endLon + "> not as expected <" + trajDsInfo.getExampleTrajEndLon() + ">.",
                doubleWithinEpsilon( endLon, trajDsInfo.getExampleTrajEndLon(), 0.0001 ) );

    assertTrue( "Start elevation <" + startElev + "> not as expected <" + trajDsInfo.getExampleTrajStartElev() + ">.",
                doubleWithinEpsilon( startElev, trajDsInfo.getExampleTrajStartElev(), 0.0001 ) );
    assertTrue( "End elevation <" + endElev + "> not as expected <" + trajDsInfo.getExampleTrajEndElev() + ">.",
                doubleWithinEpsilon( endElev, trajDsInfo.getExampleTrajEndElev(), 0.0001 ) );

    // Check the first and last values of the example variable.
    Object exampleVarStartVal = null;
    Object exampleVarEndVal = null;
    try
    {
      Array array = traj1.getData( 0, trajDsInfo.getExampleVarName() );
      exampleVarStartVal = array.getObject( array.getIndex());
      array = traj1.getData( traj1.getNumberPoints() - 1, trajDsInfo.getExampleVarName() );
      exampleVarEndVal = array.getObject( array.getIndex());
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to read start and end values of " + trajDsInfo.getExampleVarName() + ": " + e.getMessage(),
                  false );
    }
    assertTrue( "Example variable \"" + trajDsInfo.getExampleVarName()
                + "\" start value <" + exampleVarStartVal + "> not as expected <" + trajDsInfo.getExampleVarStartVal() + "> or " +
                "end value <" + exampleVarEndVal + "> not as expected <" + trajDsInfo.getExampleVarEndVal() + ">.",
                doubleWithinEpsilon( ((Float) trajDsInfo.getExampleVarStartVal()).doubleValue(), ((Float) exampleVarStartVal).doubleValue(), 0.0001)
                && doubleWithinEpsilon( ((Float) trajDsInfo.getExampleVarEndVal()).doubleValue(), ((Float) exampleVarEndVal).doubleValue(), 0.0001 ) );
//    assertTrue( "Variables' first two values <" + windSpd[0] + "," + windSpd[1] + "> not as expected <" + varVals[0] + "," + varVals[1] + ">.",
//                windSpd[0] == varVals[0] && windSpd[1] == varVals[1] );

    // Test with getPointObsData()
    PointObsDatatype pointOb;
    try
    {
      pointOb = (PointObsDatatype) traj1.getPointObsData( 0 );
    }
    catch ( IOException e )
    {
      assertTrue( "IOException on call to getPointObsData(0): " + e.getMessage(),
                  false);
      return;
    }
    assertTrue( "Start time (getPointObsData) <" + pointOb.getNominalTimeAsDate().getTime() + "> not as expected <" + trajDsInfo.getStartDateLong() + ">.",
                pointOb.getNominalTimeAsDate().getTime() == trajDsInfo.getStartDateLong() );
    assertTrue( "Start lat (getPointObsData) <" + pointOb.getLocation().getLatitude() + "> not as expected <" + trajDsInfo.getExampleTrajStartLat() + ">.",
                doubleWithinEpsilon( pointOb.getLocation().getLatitude(), trajDsInfo.getExampleTrajStartLat(), 0.0001 ) );
    assertTrue( "Start lon (getPointObsData) <" + pointOb.getLocation().getLongitude() + "> not as expected <" + trajDsInfo.getExampleTrajStartLon() + ">.",
                doubleWithinEpsilon( pointOb.getLocation().getLongitude(), trajDsInfo.getExampleTrajStartLon(), 0.0001 ) );
    assertTrue( "Start alt (getPointObsData) <" + pointOb.getLocation().getAltitude() + "> not as expected <" + trajDsInfo.getExampleTrajStartElev() + ">.",
                doubleWithinEpsilon( pointOb.getLocation().getAltitude(), trajDsInfo.getExampleTrajStartElev(), 0.0001 ) );

    // Test with
    StructureData sdata;
    try
    {
      sdata = traj1.getData( 0);
    }
    catch ( IOException e )
    {
      assertTrue( "IOException on getData(0): " + e.getMessage(),
                  false);
      return;
    }
    catch ( InvalidRangeException e )
    {
      assertTrue( "InvalidRangeException on getData(0): " + e.getMessage(),
                  false );
      return;
    }
    assertTrue( "Null StructureData from getData(0).",
                sdata != null);

    // Test with getDataIterator()
    DataIterator it;
    try
    {
      it = traj1.getDataIterator( 0);
    }
    catch ( IOException e )
    {
      assertTrue( "IOException on call to trajectory.getDataIterator(): " + e.getMessage(),
                  false);
      return;
    }
    if ( it == null)
    {
      System.out.println( "Null trajectory iterator, skipping iterator tests." );
      return;
    }

    if ( ! it.hasNext() )
    {
      assertTrue( "First call to Iterator.hasNext() returned false.",
                  false );
      return;
    }
    pointOb = (PointObsDatatype) it.nextData();
    assertTrue( "Start time (iterator) <" + pointOb.getNominalTimeAsDate().getTime() + "> not as expected <" + trajDsInfo.getStartDateLong() + ">.",
                pointOb.getNominalTimeAsDate().getTime() == trajDsInfo.getStartDateLong() );
    assertTrue( "Start lat (iterator) <" + pointOb.getLocation().getLatitude() + "> not as expected <" + trajDsInfo.getExampleTrajStartLat() + ">.",
                doubleWithinEpsilon( pointOb.getLocation().getLatitude(), trajDsInfo.getExampleTrajStartLat(), 0.0001 ) );
    assertTrue( "Start lon (iterator) <" + pointOb.getLocation().getLongitude() + "> not as expected <" + trajDsInfo.getExampleTrajStartLon() + ">.",
                doubleWithinEpsilon( pointOb.getLocation().getLongitude(), trajDsInfo.getExampleTrajStartLon(), 0.0001 ) );
    assertTrue( "Start alt (iterator) <" + pointOb.getLocation().getAltitude() + "> not as expected <" + trajDsInfo.getExampleTrajStartElev() + ">.",
                doubleWithinEpsilon( pointOb.getLocation().getAltitude(), trajDsInfo.getExampleTrajStartElev(), 0.0001 ) );
  }

  private static boolean compareIntArray( int[] array1, int[] array2 )
  {
    return ( TestTrajectoryObsDataset.toStringIntArray( array1 ).equals( TestTrajectoryObsDataset.toStringIntArray( array2 ) ) );
  }

  private static String toStringIntArray( int[] array )
  {
    int size = array.length;
    StringBuffer rep = new StringBuffer( "int[]=(" )
            .append( size ).append( "){" );
    for ( int i = 0; i < size; i++ )
    {
      rep.append( array[i] ).append( "," );
    }
    return ( rep.toString() );
  }

  private static boolean doubleWithinEpsilon( double value, double target, double epsilon )
  {
    return ( target - epsilon <= value && value <= target + epsilon );
  }

  static class TrajDatasetInfo
  {
    private String title;
    private String description;
    private String locationURI;
    private long startDateLong;
    private long endDateLong;
    private LatLonRect bb;

    private int numGlobalAtts;
    private String exampGlobalAttName;
    private String exampGlobalAttValSubstring;

    private int numVars;
    private String exampleVarName;
    private String exampleVarDescription;
    private String exampleVarUnitsString;
    private int exampleVarRank;
    private int[] exampleVarShape;
    private String exampleVarDataType;
    private int exampleVarNumAtts;
    private Object exampleVarStartVal;
    private Object exampleVarEndVal;

    private int numTrajs;
    private String exTrajId;
    private String exTrajDesc;
    private int exTrajNumPoints;
    private float exampleTrajStartLat;
    private float exampleTrajEndLat;
    private float exampleTrajStartLon;
    private float exampleTrajEndLon;
    private float exampleTrajStartElev;
    private float exampleTrajEndElev;

    public TrajDatasetInfo( String title, String description, String locationURI,
                     long startDateLong, long endDateLong, LatLonRect bb,
                     int numGlobalAtts, String exampGlobalAttName, String exampGlobalAttVal,
                     int numVars, String exampleVarName, String exampleVarDescription, String exampleVarUnitsString,
                     int exampleVarRank, int[] exampleVarShape, String exampleVarDataType, int exampleVarNumAtts,
                     Object exampleVarStartVal, Object exampleVarEndVal,
                     int numTrajs, String exTrajId, String exTrajDesc, int exTrajNumPoints,
                     float exampleTrajStartLat, float exampleTrajEndLat,
                     float exampleTrajStartLon, float exampleTrajEndLon,
                     float exampleTrajStartElev, float exampleTrajEndElev )
    {
      this.title = title;
      this.description = description;
      this.locationURI = locationURI;
      this.startDateLong = startDateLong;
      this.endDateLong = endDateLong;
      this.bb = bb;
      this.numGlobalAtts = numGlobalAtts;
      this.exampGlobalAttName = exampGlobalAttName;
      this.exampGlobalAttValSubstring = exampGlobalAttVal;
      this.numVars = numVars;
      this.exampleVarName = exampleVarName;
      this.exampleVarDescription = exampleVarDescription;
      this.exampleVarUnitsString = exampleVarUnitsString;
      this.exampleVarRank = exampleVarRank;
      this.exampleVarShape = exampleVarShape;
      this.exampleVarDataType = exampleVarDataType;
      this.exampleVarNumAtts = exampleVarNumAtts;

      this.exampleVarStartVal = exampleVarStartVal;
      this.exampleVarEndVal = exampleVarEndVal;

      this.numTrajs = numTrajs;
      this.exTrajId = exTrajId;
      this.exTrajDesc = exTrajDesc;
      this.exTrajNumPoints = exTrajNumPoints;

      this.exampleTrajStartLat = exampleTrajStartLat;
      this.exampleTrajEndLat = exampleTrajEndLat;
      this.exampleTrajStartLon = exampleTrajStartLon;
      this.exampleTrajEndLon = exampleTrajEndLon;
      this.exampleTrajStartElev = exampleTrajStartElev;
      this.exampleTrajEndElev = exampleTrajEndElev;
    }

    public String getTitle()
    {
      return title;
    }

    public String getDescription()
    {
      return description;
    }

    public String getLocationURI()
    {
      return locationURI;
    }

    public void setLocationURI( String locationURI )
    {
      this.locationURI = locationURI;
    }

    public long getStartDateLong()
    {
      return startDateLong;
    }

    public long getEndDateLong()
    {
      return endDateLong;
    }

    public LatLonRect getBb()
    {
      return bb;
    }

    public int getNumGlobalAtts()
    {
      return numGlobalAtts;
    }

    public String getExampGlobalAttName()
    {
      return exampGlobalAttName;
    }

    public String getExampGlobalAttValSubstring()
    {
      return exampGlobalAttValSubstring;
    }

    public int getNumVars()
    {
      return numVars;
    }

    public String getExampleVarName()
    {
      return exampleVarName;
    }

    public String getExampleVarDescription()
    {
      return exampleVarDescription;
    }

    public String getExampleVarUnitsString()
    {
      return exampleVarUnitsString;
    }

    public int getExampleVarRank()
    {
      return exampleVarRank;
    }

    public int getExampleVarNumAtts()
    {
      return exampleVarNumAtts;
    }

    public String getExampleVarDataType()
    {
      return exampleVarDataType;
    }

    public int[] getExampleVarShape()
    {
      return exampleVarShape;
    }

    public int getNumTrajs()
    {
      return numTrajs;
    }

    public String getExTrajId()
    {
      return exTrajId;
    }

    public String getExTrajDesc()
    {
      return exTrajDesc;
    }

    public int getExTrajNumPoints()
    {
      return exTrajNumPoints;
    }

    public float getExampleTrajStartLat()
    {
      return exampleTrajStartLat;
    }

    public float getExampleTrajEndLat()
    {
      return exampleTrajEndLat;
    }

    public float getExampleTrajStartLon()
    {
      return exampleTrajStartLon;
    }

    public float getExampleTrajEndLon()
    {
      return exampleTrajEndLon;
    }

    public float getExampleTrajStartElev()
    {
      return exampleTrajStartElev;
    }

    public float getExampleTrajEndElev()
    {
      return exampleTrajEndElev;
    }

    public Object getExampleVarStartVal()
    {
      return exampleVarStartVal;
    }

    public Object getExampleVarEndVal()
    {
      return exampleVarEndVal;
    }
  }
}

/*
 * $Log: TestTrajectoryObsDataset.java,v $
 * Revision 1.14  2006/06/06 16:07:18  caron
 * *** empty log message ***
 *
 * Revision 1.13  2006/05/08 02:47:38  caron
 * cleanup code for 1.5 compile
 * modest performance improvements
 * dapper reading, deal with coordinate axes as structure members
 * improve DL writing
 * TDS unit testing
 *
 * Revision 1.12  2005/05/25 20:53:43  edavis
 * Add some test data to CVS, the rest is on /upc/share/testdata2.
 *
 * Revision 1.11  2005/05/23 22:47:01  edavis
 * Handle changing elevation data units (done in ncDataset
 * and record structure, needed some changes from John, too).
 *
 * Revision 1.10  2005/05/23 20:18:38  caron
 * refactor for scale/offset/missing
 *
 * Revision 1.9  2005/05/23 17:02:23  edavis
 * Deal with converting elevation data into "meters".
 *
 * Revision 1.8  2005/05/20 19:44:28  edavis
 * Add getDataIterator() to TrajectoryObsDatatype and implement
 * in SingleTrajectoryObsDataset (returns null in
 * MultiTrajectoryObsDataset).
 *
 * Revision 1.7  2005/05/16 23:49:55  edavis
 * Add ucar.nc2.dt.trajectory.ARMTrajectoryObsDataset to handle
 * ARM sounding files. Plus a few other fixes and updates to the
 * tests.
 *
 * Revision 1.6  2005/05/16 16:47:53  edavis
 * A few improvements to SingleTrajectoryObsDataset and start using
 * it in RafTrajectoryObsDataset. Add MultiTrajectoryObsDataset
 * (based on SingleTrajectoryObsDataset) and use in
 * Float10TrajectoryObsDataset.
 *
 * Revision 1.5  2005/05/11 19:58:12  caron
 * add VariableSimpleIF, remove TypedDataVariable
 *
 * Revision 1.4  2005/05/11 00:10:10  caron
 * refactor StuctureData, dt.point
 *
 * Revision 1.3  2005/04/16 15:55:14  edavis
 * Fix Float10Trajectory. Improve testing.
 *
 * Revision 1.2  2005/03/18 00:29:08  edavis
 * Finish trajectory implementations with the new TrajectoryObsDatatype
 * and TrajectoryObsDataset interfaces and update tests.
 *
 * Revision 1.1  2005/03/10 21:34:18  edavis
 * Redo trajectory implementations with new TrajectoryObsDatatype and
 * TrajectoryObsDataset interfaces.
 *
 * Revision 1.1  2005/03/01 22:02:24  edavis
 * Two more implementations of the TrajectoryDataset interface.
 *
 */