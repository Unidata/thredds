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

import junit.framework.TestCase;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.io.File;

/**
 * A description
 *
 * @author edavis
 * @since Feb 22, 2005T22:33:51 PM
 */
public class TestFloat10TrajectoryObsDataset extends TestCase
{
  private TrajectoryObsDataset me;

  private String testFilePath = TestTrajectoryObsDataset.getRemoteTestDataDir() + "/trajectory/buoy";
  private String testDataFileName = "testfloat10.nc";

  public TestFloat10TrajectoryObsDataset( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testStuff() throws IOException {
    String location = testFilePath + "/" + testDataFileName;
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
                  false);
    }
    assertTrue( "Null TrajectoryObsDataset after open <" + location + "> ",
                me != null );
    assertTrue( "Dataset <" + location + "> not a Float10TrajectoryObsDataset.",
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
                    dsTitle, dsDescrip, location,
                    dsStartDateLong, dsEndDateLong, dsBoundBox,
                    dsNumGlobalAtts, exampleGlobalAttName, exampleGlobalAttVal,
                    dsNumVars, exampleVarName, exampleVarDescription,
                    exampleVarUnitsString, exampleVarRank, exampleVarShape, exampleVarDataType, exampleVarNumAtts,
                    new Float( 32.614796f ), new Float( 32.00046f ),
                    numTrajs, exampleTrajId, exampleTrajDesc, exampleTrajNumPoints,
                    exampleTrajStartLat, exampleTrajEndLat, exampleTrajStartLon, exampleTrajEndLon, exampleTrajStartElev, exampleTrajEndElev);

    TestTrajectoryObsDataset.testTrajInfo( me, trajDsInfo );
  }

}

/*
 * $Log: TestFloat10TrajectoryObsDataset.java,v $
 * Revision 1.6  2006/06/06 16:07:17  caron
 * *** empty log message ***
 *
 * Revision 1.5  2006/05/08 02:47:37  caron
 * cleanup code for 1.5 compile
 * modest performance improvements
 * dapper reading, deal with coordinate axes as structure members
 * improve DL writing
 * TDS unit testing
 *
 * Revision 1.4  2005/05/25 20:53:42  edavis
 * Add some test data to CVS, the rest is on /upc/share/testdata2.
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
 * Revision 1.1  2005/03/01 22:02:24  edavis
 * Two more implementations of the TrajectoryDataset interface.
 *
 *
 */