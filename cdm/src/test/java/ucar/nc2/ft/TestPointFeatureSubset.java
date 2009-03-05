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

package ucar.nc2.ft;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.units.DateType;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.util.Formatter;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 4, 2009
 */
public class TestPointFeatureSubset extends TestCase {

  public TestPointFeatureSubset(String name) {
    super(name);
  }

  public void testProblem() throws IOException {
    //testPointDataset(topDir+"noZ/41001h2007.nc", FeatureType.ANY_POINT, true);
    testPointDataset("R:/testdata/point/gempak/nmcbob.shp", FeatureType.POINT, true);
  }

  int readAllDir(String dirName, FileFilter ff, FeatureType type) throws IOException {
    int count = 0;

    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude")) {
        try {
          testPointDataset(name, type, false);
        } catch (Throwable t) {
          t.printStackTrace();
        }
        count++;
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += readAllDir(f.getAbsolutePath(), ff, type);
    }

    return count;
  }

  private void testPointDataset(String location, FeatureType wantType, boolean show) throws IOException {
    System.out.printf("----------- Read %s %n", location);
    long start = System.currentTimeMillis();

    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(wantType, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      return;
    }

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    FeatureType ftype = fdataset.getFeatureType();
    assert FeatureDatasetFactoryManager.featureTypeOk(wantType, ftype);

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (ftype == fc.getCollectionFeatureType());
      
      if (ftype == FeatureType.POINT) {
        assert (fc instanceof PointFeatureCollection);
        testPointSubset( fdataset, (PointFeatureCollection) fc);

      } else if (ftype == FeatureType.STATION) {
        assert (fc instanceof StationTimeSeriesFeatureCollection);

      } else if (ftype == FeatureType.PROFILE) {
        assert (fc instanceof ProfileFeatureCollection);

      } else if (ftype == FeatureType.STATION_PROFILE) {
        assert (fc instanceof StationProfileFeatureCollection);

      } else if (ftype == FeatureType.TRAJECTORY) {
        assert (fc instanceof TrajectoryFeatureCollection);
        
      } else if (ftype == FeatureType.SECTION) {
        assert (fc instanceof SectionFeatureCollection);
      }
    }

    fdataset.close();
    long took = System.currentTimeMillis() - start;
    System.out.println(" took= " + took + " msec");
  }

  private void testPointSubset( FeatureDataset fd, PointFeatureCollection fc) {
    DateRange dr = makeTimeSubset(fd);
  }


  DateRange makeTimeSubset(FeatureDataset fd) {
    DateRange dr = fd.getDateRange();
    System.out.printf(" original date range=%s %n", dr);
    if (dr == null) return null;

    TimeDuration td = dr.getDuration();
    double secs = td.getValueInSeconds();
    td.setValueInSeconds(secs/4);
    DateType start = dr.getStart();
    start.add(td);
    td.setValueInSeconds(secs/2);
    DateRange result = new DateRange(start, null, td, null );
    System.out.printf(" subset date range=%s %n", result);
    return result;
  }
}
