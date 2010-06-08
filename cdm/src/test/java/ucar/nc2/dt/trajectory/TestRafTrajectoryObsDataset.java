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
// $Id: TestRafTrajectoryObsDataset.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import junit.framework.TestCase;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.nc2.dt.*;
import ucar.ma2.Range;
import ucar.ma2.Array;

import java.io.IOException;
import java.io.File;
import java.util.Iterator;

/**
 * A description
 *
 * @author edavis
 * @since Feb 11, 2005T2:33:51 PM
 */
public class TestRafTrajectoryObsDataset extends TestCase
{
  private TrajectoryObsDataset me;

  private String testFilePath = TestTrajectoryObsDataset.getRemoteTestDataDir() + "/trajectory/aircraft";
  private String test_Raf_1_2_FileName = "raftrack.nc";
  private String test_Raf_1_3_Recvar_FileName = "135_raw.nc";
  private String test_Raf_1_3_NoRecvar_FileName = "135_ordrd.nc";

  public TestRafTrajectoryObsDataset( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testRaf_1_2() throws IOException {
    String location = testFilePath + "/" + test_Raf_1_2_FileName;
    assertTrue( "Test file <" + location + "> does not exist.",
                new File( location ).exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false);
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );


    int dsNumVars = 341; //325;
    String exampleVarName = "PITCH";
    String exampleVarDescription = "Aircraft Pitch Angle";
    String exampleVarUnitsString = "deg";
    int exampleVarRank = 0;
    int[] exampleVarShape = new int[]{};
    String exampleVarDataType = DataType.FLOAT.toString();
    int exampleVarNumAtts = 6;
    int numTrajs = 1;
    String exampleTrajId = "1Hz data";
    String exampleTrajDesc = null;
    int exampleTrajNumPoints = 8157;
    float exampleTrajStartLat = 17.129574f;
    float exampleTrajEndLat = 17.124596f;
    float exampleTrajStartLon = -61.8017f;
    float exampleTrajEndLon = -61.79552f;
    float exampleTrajStartElev = -34.01111f;
    float exampleTrajEndElev = -19.944588f;
    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo =
            new TestTrajectoryObsDataset.TrajDatasetInfo(
                    null, null, location,
                    1105193344000l, 1105201500000l, null,
                    14, "Source", "NCAR Research Aviation Facility",
                    dsNumVars, exampleVarName, exampleVarDescription,
                    exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                    new Float( 0.18874642f ), new Float( -1.0590017f ),
                    numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                    exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev );

    TestTrajectoryObsDataset.testTrajInfo( me, trajDsInfo);

  }

  /**
   * Test ...
   */
  public void testRaf_1_3_Recvar_And_NoRecvar() throws IOException {
    // Test for raw file which has  record variable.
    String location = testFilePath + "/" + test_Raf_1_3_Recvar_FileName;
    assertTrue( "Test file <" + location + "> does not exist.",
                new File( location ).exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    long startDateLong = 1102515300000l;
    long endDateLong = 1102523040000l;
    int numGlobalAtts = 16;
    String exampGlobalAttName = "Source";
    String exampGlobalAttVal = "NCAR Research Aviation Facility";
    int dsNumVars = 350;
    String exampleVarName = "PITCH";
    String exampleVarDescription = "Aircraft Pitch Angle";
    String exampleVarUnitsString = "deg";
    int exampleVarRank = 0;
    int[] exampleVarShape = new int[]{};
    String exampleVarDataType = DataType.FLOAT.toString();
    int exampleVarNumAtts = 11;
    Float exampleVarStartVal = new Float( -0.1351365f );
    Float exampleVarEndVal = new Float( -1.3088403f );
    int numTrajs = 1;
    String exampleTrajId = "1Hz data";
    String exampleTrajDesc = null;
    int exampleTrajNumPoints = 7741;
    float exampleTrajStartLat = 17.11825f;
    float exampleTrajEndLat = 17.137465f;
    float exampleTrajStartLon = -61.817482f;
    float exampleTrajEndLon = -61.79102f;
    float exampleTrajStartElev = 28.45f;
    float exampleTrajEndElev = 23.349998f;
    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo =
            new TestTrajectoryObsDataset.TrajDatasetInfo( null, null, location,
                                                          startDateLong, endDateLong, null,
                                                          numGlobalAtts, exampGlobalAttName, exampGlobalAttVal,
                                                          dsNumVars, exampleVarName, exampleVarDescription,
                                                          exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                                                          exampleVarStartVal, exampleVarEndVal,
                                                          numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                                                          exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev );

    TestTrajectoryObsDataset.testTrajInfo( me, trajDsInfo );

    // Test for same post-processed file which has no record variable.
    location = testFilePath + "/" + test_Raf_1_3_NoRecvar_FileName;
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    trajDsInfo.setLocationURI( location );
    TestTrajectoryObsDataset.testTrajInfo( me, trajDsInfo );
  }

  /**
   * Time variable oriented read vs point oriented read.
   *
   * @throws Exception On badness
   */
  public void formerly_testTiming() throws Exception
  {
    //================ record written data ====================
    String location = testFilePath + "/" + test_Raf_1_3_Recvar_FileName;
    assertTrue( "Test file <" + location + "> does not exist.",
                new File( location ).exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    long startDate, endDate;
    // Read all data in variable oriented manner.
    TrajectoryObsDatatype trajDt = me.getTrajectory( "1Hz data" );

    Range range = trajDt.getFullRange();
    startDate = System.currentTimeMillis();
    for ( Iterator it = trajDt.getDataVariables().iterator(); it.hasNext(); )
    {
      VariableSimpleIF var = (VariableSimpleIF) it.next();
      Array a = trajDt.getData( range, var.getName() );
    }
    endDate = System.currentTimeMillis();
    System.out.println( "Variable-oriented read of record written data: " + ( (endDate - startDate) / 1000.0 ) + " seconds" );

    // Read all data in point oriented manner.
    startDate = System.currentTimeMillis();
    for ( int i = 0; i < trajDt.getNumberPoints(); i++ )
    {
      StructureData structData = trajDt.getData( i );
    }
    endDate = System.currentTimeMillis();
    System.out.println( "Point-oriented read of record written data: " + ( ( endDate - startDate ) / 1000.0 ) + " seconds" );

    // Read all data in point oriented manner with iterator.
    startDate = System.currentTimeMillis();
    for ( DataIterator it = trajDt.getDataIterator( 0); it.hasNext(); )
    {
      PointObsDatatype pointOb = (PointObsDatatype) it.nextData();
    }
    endDate = System.currentTimeMillis();
    System.out.println( "Point-oriented iterator read of record written data: " + ( ( endDate - startDate ) / 1000.0 ) + " seconds" );

    //================ non-record written data ====================
    location = testFilePath + "/" + test_Raf_1_3_NoRecvar_FileName;
    assertTrue( "Test file <" + location + "> does not exist.",
                new File( location ).exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    // Read all data in variable oriented manner.
    trajDt = me.getTrajectory( "1Hz data" );

    range = trajDt.getFullRange();
    startDate = System.currentTimeMillis();
    for ( Iterator it = trajDt.getDataVariables().iterator(); it.hasNext(); )
    {
      VariableSimpleIF var = (VariableSimpleIF) it.next();
      Array a = trajDt.getData( range, var.getName() );
    }
    endDate = System.currentTimeMillis();
    System.out.println( "Variable-oriented read of non-record written data: " + ( ( endDate - startDate ) / 1000.0 ) + " seconds" );

    // Read all data in point oriented manner.
    startDate = System.currentTimeMillis();
    for ( int i = 0; i < trajDt.getNumberPoints(); i++ )
    {
      StructureData structData = trajDt.getData( i );
    }
    endDate = System.currentTimeMillis();
    System.out.println( "Point-oriented read of non-record written data: " + ( ( endDate - startDate ) / 1000.0 ) + " seconds" );

    // Read all data in point oriented manner with iterator.
    startDate = System.currentTimeMillis();
    for ( DataIterator it = trajDt.getDataIterator( 0 ); it.hasNext(); )
    {
      PointObsDatatype pointOb = (PointObsDatatype) it.nextData();
    }
    endDate = System.currentTimeMillis();
    System.out.println( "Point-oriented iterator read of non-record written data: " + ( ( endDate - startDate ) / 1000.0 ) + " seconds" );

    assertTrue( "Timing output",
                false);
  }
}

/*
 * $Log: TestRafTrajectoryObsDataset.java,v $
 * Revision 1.11  2006/06/06 16:07:17  caron
 * *** empty log message ***
 *
 * Revision 1.10  2006/05/08 02:47:37  caron
 * cleanup code for 1.5 compile
 * modest performance improvements
 * dapper reading, deal with coordinate axes as structure members
 * improve DL writing
 * TDS unit testing
 *
 * Revision 1.9  2005/05/25 20:53:42  edavis
 * Add some test data to CVS, the rest is on /upc/share/testdata2.
 *
 * Revision 1.8  2005/05/23 20:18:38  caron
 * refactor for scale/offset/missing
 *
 * Revision 1.7  2005/05/20 19:44:28  edavis
 * Add getDataIterator() to TrajectoryObsDatatype and implement
 * in SingleTrajectoryObsDataset (returns null in
 * MultiTrajectoryObsDataset).
 *
 * Revision 1.6  2005/05/16 23:49:55  edavis
 * Add ucar.nc2.dt.trajectory.ARMTrajectoryObsDataset to handle
 * ARM sounding files. Plus a few other fixes and updates to the
 * tests.
 *
 * Revision 1.5  2005/05/16 16:47:53  edavis
 * A few improvements to SingleTrajectoryObsDataset and start using
 * it in RafTrajectoryObsDataset. Add MultiTrajectoryObsDataset
 * (based on SingleTrajectoryObsDataset) and use in
 * Float10TrajectoryObsDataset.
 *
 * Revision 1.4  2005/05/11 22:16:05  caron
 * some TypedVariables sliiped through the cracks
 *
 * Revision 1.3  2005/05/11 00:10:10  caron
 * refactor StuctureData, dt.point
 *
 * Revision 1.2  2005/04/16 15:55:13  edavis
 * Fix Float10Trajectory. Improve testing.
 *
 * Revision 1.1  2005/03/18 00:29:08  edavis
 * Finish trajectory implementations with the new TrajectoryObsDatatype
 * and TrajectoryObsDataset interfaces and update tests.
 *
 * Revision 1.3  2005/03/01 22:02:24  edavis
 * Two more implementations of the TrajectoryDataset interface.
 *
 * Revision 1.2  2005/02/22 20:54:38  edavis
 * Second pass at TrajectoryDataset interface and RAF aircraft implementation.
 *
 * Revision 1.1  2005/02/15 17:57:05  edavis
 * Implement TrajectoryDataset for RAF Aircraft trajectory data.
 *
 */