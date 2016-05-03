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
// $Id: TestARMTrajectoryObsDataset.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
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
public class TestARMTrajectoryObsDataset {
  private TrajectoryObsDataset me;

  /**
   * Test ...
   */
  @Test
  public void testARM() throws IOException {
    String location = TestDir.cdmUnitTestDir + "ft/profile/sonde/sgpsondewnpnC1.a1.20020507.112400.cdf";
    assertTrue("Test file <" + location + "> does not exist.", new File(location).exists());

    try {
      //me = TrajectoryObsDatasetFactory.open( location);
      StringBuilder errlog = new StringBuilder();
      me = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);

    } catch (IOException e) {
      String tmpMsg = "Couldn't create TrajectoryObsDataset from ARM sonde file <" + location + ">: " + e.getMessage();
      assertTrue(tmpMsg, false);
    }
    assertTrue("Null TrajectoryObsDataset after open <" + location + "> ", me != null);
    assertTrue("Dataset <" + location + "> not a ARMTrajectoryObsDataset.", me instanceof ARMTrajectoryObsDataset);

    String dsTitle = null;
    String dsDescrip = null;
    long dsStartDateLong = 1020770640000l;
    long dsEndDateLong = 1020776540000l;
    LatLonRect dsBoundBox = null;
    int dsNumGlobalAtts = 27;
    String exampleGlobalAttName = "history";
    String exampleGlobalAttVal = "created by the Zebra DataStore library";
    int dsNumVars = 10;
    String exampleVarName = "tdry";
    String exampleVarDescription = "Dry Bulb Temperature";
    String exampleVarUnitsString = "C";
    int exampleVarRank = 0;
    int[] exampleVarShape = new int[]{};
    String exampleVarDataType = DataType.FLOAT.toString();
    int exampleVarNumAtts = 4;
    Float exampleVarStartVal = new Float(21.0f);
    Float exampleVarEndVal = new Float(-49.1f);
    int numTrajs = 1;
    String exampleTrajId = "trajectory data";
    String exampleTrajDesc = null;

    int exampleTrajNumPoints = 2951;
    float exampleTrajStartLat = 36.61f;
    float exampleTrajEndLat = 37.12618f;
    float exampleTrajStartLon = -97.49f;
    float exampleTrajEndLon = -96.28289f;
    float exampleTrajStartElev = 315.0f;
    float exampleTrajEndElev = 26771.0f;

    TestTrajectoryObsDataset.TrajDatasetInfo trajDsInfo =
            new TestTrajectoryObsDataset.TrajDatasetInfo(
                    dsTitle, dsDescrip, location,
                    dsStartDateLong, dsEndDateLong, dsBoundBox,
                    dsNumGlobalAtts, exampleGlobalAttName, exampleGlobalAttVal,
                    dsNumVars, exampleVarName, exampleVarDescription,
                    exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                    exampleVarStartVal, exampleVarEndVal,
                    numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                    exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev);

    TestTrajectoryObsDataset.assertTrajectoryObsDatasetInfoAsExpected(me, trajDsInfo);

    TrajectoryObsDatatype traj = me.getTrajectory(exampleTrajId);

    // Test that "alt" units gets changed to "meters"
    // ... from getPointObsData(0)
    PointObsDatatype pointOb;
    try {
      pointOb = traj.getPointObsData(0);
    } catch (IOException e) {
      assertTrue("IOException on call to getPointObsData(0): " + e.getMessage(), false);
      return;
    }
    StructureData sdata;
    try {
      sdata = pointOb.getData();
    } catch (IOException e) {
      assertTrue("IOException on getData(): " + e.getMessage(), false);
      return;
    }

    String u = sdata.findMember("alt").getUnitsString();
    assert u.equals("meters") : "traj.getPointObsData().getData().findMember( \"alt\") units <" + u + "> not as expected";
    //assertTrue( "traj.getPointObsData().getData().findMember( \"alt\") units <" + u + "> not as expected <meters>.",
    //            u.equals( "meters") );

    // ... from getData(0)
    try {
      sdata = traj.getData(0);
    } catch (IOException e) {
      assertTrue("IOException on getData(0): " + e.getMessage(), false);
    } catch (InvalidRangeException e) {
      assertTrue("InvalidRangeException on getData(0): " + e.getMessage(), false);
    }
    //u = sdata.findMember( "alt" ).getUnitsString();
    //assertTrue( "traj.getData().findMember( \"alt\") units <" + u + "> not as expected <meters>.",
    //            u.equals( "meters" ) );

  }

}
