/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Synthetic (Ncml) datasets for testing point feature variants
 *
 * @author caron
 * @since 6/27/2014
 */
@RunWith(Parameterized.class)
public class TestPreCFpointDatasets {
  static public String CFpointObs_pre16 = TestDir.cdmLocalTestDataDir + "pointPre1.6/";

  private static class FileSort implements Comparable<FileSort> {
    String path;
    int order = 10;

    FileSort(File f) {
      this.path = f.getPath();
      String name = f.getName().toLowerCase();
      if (name.contains("point")) order = 1;
      else if (name.contains("stationprofile")) order = 5;
      else if (name.contains("station")) order = 2;
      else if (name.contains("profile")) order = 3;
      else if (name.contains("traj")) order = 4;
      else if (name.contains("section")) order = 6;
    }

    @Override
    public int compareTo(FileSort o) {
      return order - o.order;
    }
  }

  @Parameterized.Parameters
  public static List<Object[]> getTestParameters() {
    List<FileSort> files = new ArrayList<>();
    File topDir = new File(CFpointObs_pre16);
    for (File f : topDir.listFiles()) {
      files.add( new FileSort(f));
    }
    Collections.sort(files);

    List<Object[]> result = new ArrayList<>();
    for (FileSort f : files) {
      result.add(new Object[] {f.path, FeatureType.ANY_POINT});
      System.out.printf("%s%n", f.path);
    }

    return result;
  }

  String location;
  FeatureType ftype;
  boolean show = false;

  public TestPreCFpointDatasets(String location, FeatureType ftype) {
    this.location = location;
    this.ftype = ftype;
  }

  @Test
  public void checkPointDataset() throws IOException {
    TestPointFeatureTypes test = new TestPointFeatureTypes("");
    assert 0 < test.checkPointDataset(location, ftype, show);
  }


}
