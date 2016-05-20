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
package ucar.nc2.dt.radial;

import junit.framework.*;

import org.junit.experimental.categories.Category;
import ucar.nc2.dt.*;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Date;

/** Test radial datasets in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestRadialDataset extends TestCase {
  // private RadialDatasetSweepFactory datasetFactory = new RadialDatasetSweepFactory();
  private String topDir = TestDir.cdmUnitTestDir + "formats/";
  private boolean show = false, showTime = false, doQuick = true;

  public TestRadialDataset( String name) {
    super(name);
  }

  public void testNexrad2Dataset() throws IOException {
    testAllMethods( topDir+"nexrad/level2/Level2_KYUX_20060527_2335.ar2v");
  }

  public void testReadDirectory() throws IOException {
    long start = System.currentTimeMillis();
    // doDirectory(TestAll.testdataDir + "radar/nexrad/level2/VCP11", false);
    //DiskCache.setCachePolicy( true);
    //DiskCache.setRootDirectory( System.getProperty( "java.io.tmpdir" ) + "/cache/");
    doDirectory(topDir + "nexrad/level2/", false, 10, ".raw");
    long took = System.currentTimeMillis() - start;
    System.out.println("that took = "+took+" msec");
  }

  private int doDirectory(String dirName, boolean alwaysUncompress, int max, String suffix) throws IOException {

    File dir = new File(dirName);
    if ( ! dir.exists())  {
      System.out.println( "TestRadialDataset.doDirectory(): non-existent directory <" + dirName + ">." );
      throw new IllegalArgumentException( "Non-existent directory <" + dirName + ">.");
    }
    File[] files = dir.listFiles();
    if (alwaysUncompress) {
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        String path = file.getPath();
        if (path.endsWith(".uncompress"))
          file.delete();
      }
    }

    int count = 0;
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      String path = file.getPath();
      if (path.endsWith(".uncompress")) continue;

      if (file.isDirectory())
        count += doDirectory(path, alwaysUncompress, max, suffix);
      else if (path.endsWith(suffix)) {
        testAllMethods( path );
        count++;
      }

      if (count > max) break;
    }

    return count;
  }

  private void testAllMethods(String location) throws IOException {
    //RadialDatasetSweep rds = datasetFactory.open( location, null);
    RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open( FeatureType.RADIAL, location, null, new StringBuilder());

    System.out.println(location+"-----------");
    if (show) System.out.println(rds.getDetailInfo());

    Date d1 = rds.getStartDate();
    Date d2 = rds.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals( d2);

    ucar.unidata.geoloc.EarthLocation eloc = rds.getCommonOrigin();
    assert eloc != null;
    LatLonPoint llp = new LatLonPointImpl( eloc.getLatitude(), eloc.getLongitude());

    LatLonRect bb = rds.getBoundingBox();
    assert bb.contains(llp);

    List rvars = rds.getDataVariables();
    assert null != rvars;
    assert 0 < rvars.size();
    for (int i = 0; i < rvars.size(); i++) {
      RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) rvars.get(i);
      testRadialVariable( rv);
    }

  }

  private void testRadialVariable( RadialDatasetSweep.RadialVariable rv) throws IOException {
    System.out.println(" radial variable = "+rv.getShortName());

    int nsweeps = rv.getNumSweeps();
    for (int i = 0; i < nsweeps; i++) {
      RadialDatasetSweep.Sweep sweep =  rv.getSweep(i);
      testSweep( sweep);
    }
  }

  private void testSweep( RadialDatasetSweep.Sweep s) throws IOException {

    if (show) System.out.println("  sweep type= "+s.getType()+
              " nRadials= "+s.getRadialNumber()+
              " nGates= "+s.getGateNumber()+
              " width= "+s.getBeamWidth()+
              " nyqFreq= "+s.getNyquistFrequency()+
              " firstGate= "+s.getRangeToFirstGate()+
              " sizeGate= "+s.getGateSize() +
              " meanElev= "+s.getMeanElevation() +
              " meanAzi= "+s.getMeanAzimuth() +
              " startingTime= "+s.getStartingTime() +
              " endingTime= "+s.getEndingTime()
    );
    long start = System.currentTimeMillis();

    int nrays = s.getRadialNumber();
    int ngates = s.getGateNumber();

    float[] data = s.readData();
    assert data != null;
    assert data.length == nrays * ngates;

    nrays = s.getRadialNumber();
    ngates = s.getGateNumber();
    assert data.length == nrays * ngates;

    data = s.readData();
    assert data != null;
    assert data.length == nrays * ngates;

    if (doQuick) return;

    long took = System.currentTimeMillis();
    if (showTime) System.out.println(" read()="+(took-start));

    for (int ray = 0; ray < nrays; ray++) {
      float[] ray_data = s.readData(ray);
      assert ray_data != null;
      assert ray_data.length == ngates;

      /* double elev = s.getElevation(ray);
      double azi = s.getAzimuth(ray);
      double t = s.getTime(ray); */
    }

    long took2 = System.currentTimeMillis();
    if (showTime) System.out.println(" read(ray)="+(took2-took));

    double result = 0.0;
    for (int ray = 0; ray < nrays; ray++) {
      double elev = s.getElevation(ray);
      double azi = s.getAzimuth(ray);
      double t = s.getTime(ray);
      result += elev * azi * t;
    }

    long took3 = System.currentTimeMillis();
    if (showTime) System.out.println(" getCoord(ray)="+(took3-took2)+" msecs ("+result+")");
  }


}
