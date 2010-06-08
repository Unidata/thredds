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
// $Id: TestSimpleTrajectoryObsDataset.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import junit.framework.TestCase;
import ucar.ma2.DataType;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.io.File;

/**
 * A description
 *
 * @author edavis
 * @since Feb 22, 2005T22:33:51 PM
 */
public class TestSimpleTrajectoryObsDataset extends TestCase
{
  private TrajectoryObsDataset me;

  private String testFilePath = TestTrajectoryObsDataset.getTestDataDir() + "/trajectory/aircraft";
  private String testDataFileName = "uw_kingair-2005-01-19-113957.nc";
  private String test_WMI_Lear_FileName = "WMI_Lear-2003-05-28-212817.nc";

  public TestSimpleTrajectoryObsDataset( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testSimple_UW_KingAir() throws IOException {
    String location = testFilePath + "/" + testDataFileName;
    assertTrue( "Test file <" + location + "> does not exist.",
                new File( location).exists());
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from UW KingAir aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false);
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a SimpleTrajectoryObsDataset.",
                me instanceof SimpleTrajectoryObsDataset );

    // Check TypedDataset info.
    long startDateLong = 1106134797000l;
    long endDateLong = 1106149487000l;
    float expStartLat = 17.140499f;
    float expEndLat = 17.1403007f;
    float expStartLon = -61.77740097f;
    float expEndLon = -61.7790985f;
    float expStartElev = 17.0f;
    float expEndElev = 13.0f;
    Object exampleVarStartVal = new Float(13.1f);
    Object exampleVarEndVal = new Float(11.4f);

    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo =
            new TestTrajectoryObsDataset.TrajDatasetInfo( null, null, location,
                                                   startDateLong, endDateLong, null,
                                                   1, "history", "TrackFile.java,v 1.19 2002/05/24 16:30:34 martin Exp",
                                                   15, "HWSPD", "wind speed", "m/s", 0, new int[] {}, DataType.FLOAT.toString(), 4,
                                                   exampleVarStartVal, exampleVarEndVal,
                                                   1, "trajectory data", null, 545,
                                                   expStartLat, expEndLat,
                                                   expStartLon, expEndLon,
                                                   expStartElev, expEndElev );
    TestTrajectoryObsDataset.testTrajInfo( me, trajDsInfo );
  }

  public void testSimple_WMI_Lear() throws IOException {
    String location = testFilePath + "/" + test_WMI_Lear_FileName;
    assertTrue( "Test file <" + location + "> does not exist.",
                new File( location ).exists() );
    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from UW KingAir aircraft file <" + location + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a SimpleTrajectoryObsDataset.",
                me instanceof SimpleTrajectoryObsDataset );

    // Check TypedDataset info.
    long startDateLong = 1054157297000l;
    long endDateLong = 1054159198000l;
    float expStartLat = 38.5473f;
    float expEndLat = 38.3979988098f;
    float expStartLon = -89.8163f;
    float expEndLon = -88.6897964f;
    float expStartElev = 89.90000188f;
    float expEndElev = 7998.0f;
    Object exampleVarStartVal = new Float( 29.0f );
    Object exampleVarEndVal = new Float( -28.2f );

    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo =
            new TestTrajectoryObsDataset.TrajDatasetInfo( null, null, location,
                                                          startDateLong, endDateLong, null,
                                                          1, "history", "TrackFile.java,v 1.20 2003/05/07 04:53:23 maclean",
                                                          7, "tdry", "temperature", "deg_C", 0, new int[]{}, DataType.FLOAT.toString(), 5,
                                                          exampleVarStartVal, exampleVarEndVal,
                                                          1, "trajectory data", null, 588,
                                                          expStartLat, expEndLat,
                                                          expStartLon, expEndLon,
                                                          expStartElev, expEndElev );
    TestTrajectoryObsDataset.testTrajInfo( me, trajDsInfo );
  }
}

/*
 * $Log: TestSimpleTrajectoryObsDataset.java,v $
 * Revision 1.10  2006/06/06 16:07:18  caron
 * *** empty log message ***
 *
 * Revision 1.9  2006/05/08 02:47:37  caron
 * cleanup code for 1.5 compile
 * modest performance improvements
 * dapper reading, deal with coordinate axes as structure members
 * improve DL writing
 * TDS unit testing
 *
 * Revision 1.8  2005/05/25 20:53:42  edavis
 * Add some test data to CVS, the rest is on /upc/share/testdata2.
 *
 * Revision 1.7  2005/05/23 17:02:23  edavis
 * Deal with converting elevation data into "meters".
 *
 * Revision 1.6  2005/05/16 16:47:53  edavis
 * A few improvements to SingleTrajectoryObsDataset and start using
 * it in RafTrajectoryObsDataset. Add MultiTrajectoryObsDataset
 * (based on SingleTrajectoryObsDataset) and use in
 * Float10TrajectoryObsDataset.
 *
 * Revision 1.5  2005/05/11 22:16:05  caron
 * some TypedVariables sliiped through the cracks
 *
 * Revision 1.4  2005/05/11 00:10:10  caron
 * refactor StuctureData, dt.point
 *
 * Revision 1.3  2005/04/16 15:55:13  edavis
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
 *
 */