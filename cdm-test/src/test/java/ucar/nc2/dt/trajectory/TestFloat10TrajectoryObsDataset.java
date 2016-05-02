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
// $Id: TestFloat10TrajectoryObsDataset.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.DataType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * A description
 *
 * @author edavis
 * @since Feb 22, 2005T22:33:51 PM
 */
@Category(NeedsCdmUnitTest.class)
public class TestFloat10TrajectoryObsDataset
{
  private TrajectoryObsDataset me;

  /**
   * Test ...
   */
  @Test
  public void testStuff() throws IOException {
    File datasetFile = new File( TestDir.cdmUnitTestDir, "ft/trajectory/buoy/testfloat10.nc" );
    assertTrue( "Non-existent dataset file [" + datasetFile.getPath() + "].",
                datasetFile.exists() );
    assertTrue( "Dataset file [" + datasetFile.getPath() + "] is a directory.",
                datasetFile.isFile() );
    assertTrue( "Unreadable dataset file [" + datasetFile.getPath() + "].",
                datasetFile.canRead() );

    try {
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open( FeatureType.TRAJECTORY, datasetFile.getPath(), null, errlog);
    } catch ( IOException e ) {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from UW KingAir aircraft file <"
                      + datasetFile.getPath() + ">: " + e.getMessage();
      assertTrue( tmpMsg,
                  false);
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + datasetFile.getPath() + "> ",
                me != null );
    assertTrue( "Dataset <" + datasetFile.getPath() + "> not a Float10TrajectoryObsDataset.",
                me instanceof Float10TrajectoryObsDataset);

    String dsTitle = null;
    String dsDescrip = null;
    long dsStartDateLong = 994773600000l;
    long dsEndDateLong = 999086400000l;
    LatLonRect dsBoundBox = null;
    int dsNumGlobalAtts = 1;
    String exampleGlobalAttName = "history";
    String exampleGlobalAttVal = "FERRET V5.51    3-Jan-05";
    int dsNumVars = 2;
    String exampleVarName = "SALT";
    String exampleVarDescription = "salinity";
    String exampleVarUnitsString = "PSU";
    int exampleVarRank = 0;
    int[] exampleVarShape = new int[]{};
    String exampleVarDataType = DataType.FLOAT.toString();
    int exampleVarNumAtts = 5;
    int numTrajs = 11;
    String exampleTrajId = "100.0";
    String exampleTrajDesc = null;
    int exampleTrajNumPoints = 1199;
    float exampleTrajStartLat = 56.340836f;
    float exampleTrajEndLat = 56.201443f;
    float exampleTrajStartLon = -153.74309f;
    float exampleTrajEndLon = -154.45656f;
    float exampleTrajStartElev = -0.5914971f;
    float exampleTrajEndElev = -0.8297999f;
    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo =
            new TestTrajectoryObsDataset.TrajDatasetInfo(
                    dsTitle, dsDescrip, datasetFile.getPath(),
                    dsStartDateLong, dsEndDateLong, dsBoundBox,
                    dsNumGlobalAtts, exampleGlobalAttName, exampleGlobalAttVal,
                    dsNumVars, exampleVarName, exampleVarDescription,
                    exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                    new Float( 32.614796f ), new Float( 32.00046f ),
                    numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                    exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev);

    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected( me, trajDsInfo );
  }

}
