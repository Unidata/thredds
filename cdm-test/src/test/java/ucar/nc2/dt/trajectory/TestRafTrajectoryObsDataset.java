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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.StructureData;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;

/**
 * A description
 *
 * @author edavis
 * @since Feb 11, 2005T2:33:51 PM
 */
@Category(NeedsCdmUnitTest.class)
public class TestRafTrajectoryObsDataset
{
  private TrajectoryObsDataset me;

  private File testFileDir = new File( TestDir.cdmUnitTestDir, "ft/trajectory/aircraft");
  private String test_Raf_1_2_FileName = "raftrack.nc";
  private String test_Raf_1_3_Recvar_FileName = "135_raw.nc";
  private String test_Raf_1_3_NoRecvar_FileName = "135_ordrd.nc";

  /**
   * Test ...
   */
  @Test
  public void testRaf_1_2() throws IOException {
    File datasetFile = new File(  testFileDir, test_Raf_1_2_FileName);
    assertTrue( "Test file <" + datasetFile.getPath() + "> does not exist.",
                datasetFile.exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFile.getPath(), null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file [" + datasetFile.getPath() + "]: " + e.getMessage();
      assertTrue( tmpMsg,
                  false);
    }
    assertTrue( "Null TrajectoryObsDataset after open [" + datasetFile.getPath() + "] ",
                me != null );
    assertTrue( "Dataset [" + datasetFile.getPath() + "] not a RafTrajectoryObsDataset.",
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
                    null, null, datasetFile.getPath(),
                    1105193344000l, 1105201500000l, null,
                    14, "Source", "NCAR Research Aviation Facility",
                    dsNumVars, exampleVarName, exampleVarDescription,
                    exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                    new Float( 0.18874642f ), new Float( -1.0590017f ),
                    numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                    exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev );

    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected( me, trajDsInfo );

  }

  /**
   * Test ...
   */
  @Test
  public void testRaf_1_3_Recvar_() throws IOException {
    // Test for raw file which has  record variable.
    File datasetFile = new File(  testFileDir, test_Raf_1_3_Recvar_FileName);
    assertTrue( "Test file <" + datasetFile.getPath() + "> does not exist.",
                datasetFile.exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFile.getPath(), null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + datasetFile.getPath() + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFile.getPath() + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFile.getPath() + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo = setupTrajDatasetInfo( datasetFile );

    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected( me, trajDsInfo );
  }

  @Test
  public void testRaf_1_3_NoRecvar() throws IOException {

    // Test for same post-processed file which has no record variable.
    File datasetFile = new File(  testFileDir, test_Raf_1_3_NoRecvar_FileName);

    //location = testFilePath + "/" + test_Raf_1_3_NoRecvar_FileName;
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFile.getPath(), null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + datasetFile.getPath() + ">: " + e.getMessage();
      assertTrue( tmpMsg, false);
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFile.getPath() + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFile.getPath() + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo = setupTrajDatasetInfo( datasetFile );

    //trajDsInfo.setLocationURI( datasetFile.getPath() );
    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected( me, trajDsInfo );
  }

  private TestTrajectoryObsDataset.TrajDatasetInfo setupTrajDatasetInfo( File datasetFile )
  {
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
    return new TestTrajectoryObsDataset.TrajDatasetInfo( null, null, datasetFile.getPath(),
                                                  startDateLong, endDateLong, null,
                                                  numGlobalAtts, exampGlobalAttName, exampGlobalAttVal,
                                                  dsNumVars, exampleVarName, exampleVarDescription,
                                                  exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                                                  exampleVarStartVal, exampleVarEndVal,
                                                  numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                                                  exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev );
  }

  /**
   * Time variable oriented read vs point oriented read.
   *
   * @throws Exception On badness
   */
  public void formerly_testTiming() throws Exception
  {
    // ToDo Move or remove this test.
    //================ record written data ====================
    File datasetFile = new File( testFileDir, test_Raf_1_3_Recvar_FileName);
    String datasetFilePath = datasetFile.getPath();
    assertTrue( "Test file <" + datasetFilePath + "> does not exist.",
                datasetFile.exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFilePath, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + datasetFilePath + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFilePath + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFilePath + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    long startDate, endDate;
    // Read all data in variable oriented manner.
    TrajectoryObsDatatype trajDt = me.getTrajectory( "1Hz data" );

    Range range = trajDt.getFullRange();
    startDate = System.currentTimeMillis();
    for ( Iterator it = trajDt.getDataVariables().iterator(); it.hasNext(); )
    {
      VariableSimpleIF var = (VariableSimpleIF) it.next();
      Array a = trajDt.getData( range, var.getShortName() );
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
    datasetFile = new File( testFileDir, test_Raf_1_3_NoRecvar_FileName);
    datasetFilePath = datasetFile.getPath();

    assertTrue( "Test file <" + datasetFilePath + "> does not exist.",
                datasetFile.exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFilePath, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from RAF aircraft file <" + datasetFilePath + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFilePath + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFilePath + "> not a RafTrajectoryObsDataset.",
                me instanceof RafTrajectoryObsDataset );

    // Read all data in variable oriented manner.
    trajDt = me.getTrajectory( "1Hz data" );

    range = trajDt.getFullRange();
    startDate = System.currentTimeMillis();
    for ( Iterator it = trajDt.getDataVariables().iterator(); it.hasNext(); )
    {
      VariableSimpleIF var = (VariableSimpleIF) it.next();
      Array a = trajDt.getData( range, var.getShortName() );
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
