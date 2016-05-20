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

import org.junit.Test;
import static org.junit.Assert.*;

import ucar.ma2.DataType;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.File;

/**
 * A description
 *
 * @author edavis
 * @since Feb 22, 2005T22:33:51 PM
 */
public class TestSimpleTrajectoryObsDataset
{
  private File testFileDir = new File( TestDir.cdmLocalTestDataDir, "trajectory/aircraft");

  /**
   * Test ...
   */
  @Test
  public void testSimple_UW_KingAir() throws IOException {
    File datasetFile = new File( testFileDir, "uw_kingair-2005-01-19-113957.nc");
    String datasetFilePath = datasetFile.getPath();

    assertTrue( "Test file <" + datasetFilePath + "> does not exist.",
                datasetFile.exists() );

    TrajectoryObsDataset me = null;

    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFilePath, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from UW KingAir aircraft file <" + datasetFilePath + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false);
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFilePath + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFilePath + "> not a SimpleTrajectoryObsDataset.",
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
            new TestTrajectoryObsDataset.TrajDatasetInfo( null, null, datasetFilePath,
                                                   startDateLong, endDateLong, null,
                                                   1, "history", "TrackFile.java,v 1.19 2002/05/24 16:30:34 martin Exp",
                                                   15, "HWSPD", "wind speed", "m/s", 0, new int[] {}, DataType.FLOAT.toString(), 4,
                                                   exampleVarStartVal, exampleVarEndVal,
                                                   1, "trajectory data", null, 545,
                                                   expStartLat, expEndLat,
                                                   expStartLon, expEndLon,
                                                   expStartElev, expEndElev );
    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected( me, trajDsInfo );

    me.close();
  }
  @Test
  public void testSimple_WMI_Lear() throws IOException {
    File datasetFile = new File( testFileDir, "WMI_Lear-2003-05-28-212817.nc");
    String datasetFilePath = datasetFile.getPath();
    assertTrue( "Test file <" + datasetFilePath + "> does not exist.",
                datasetFile.exists() );

    TrajectoryObsDataset me = null;

    try
    {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, datasetFilePath, null, errlog);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from UW KingAir aircraft file <" + datasetFilePath + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false );
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFilePath + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFilePath + "> not a SimpleTrajectoryObsDataset.",
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
            new TestTrajectoryObsDataset.TrajDatasetInfo( null, null, datasetFilePath,
                                                          startDateLong, endDateLong, null,
                                                          1, "history", "TrackFile.java,v 1.20 2003/05/07 04:53:23 maclean",
                                                          7, "tdry", "temperature", "deg_C", 0, new int[]{}, DataType.FLOAT.toString(), 5,
                                                          exampleVarStartVal, exampleVarEndVal,
                                                          1, "trajectory data", null, 588,
                                                          expStartLat, expEndLat,
                                                          expStartLon, expEndLon,
                                                          expStartElev, expEndElev );
    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected( me, trajDsInfo );

    me.close();
  }
}
