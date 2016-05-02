/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 3/5/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestAggNested {

  @Test
  public void TestNotCached() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncml/nestedAgg/test.ncml";

    try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null)) {
      Variable time = ncd.findVariable("time");
      assert time != null;
      assert time.getSize() == 19723 : time.getSize();
      // System.out.printf(" time array = %s%n", NCdumpW.toString(time.read()));
    }
  }

  @Test
  public void TestCached() throws IOException {
    try {
      NetcdfDataset.initNetcdfFileCache(10, 20, -1);

      String filename = TestDir.cdmUnitTestDir + "ncml/nestedAgg/test.ncml";
      try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null)) {
        Variable time = ncd.findVariable("time");
        assert time != null;
        assert time.getSize() == 19723 : time.getSize();
        //System.out.printf(" time array = %s%n", NCdumpW.toString(time.read()));
      }

      FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
      cache.showCache();
    } finally {
      NetcdfDataset.shutdown();
    }
  }

  /*@Test
  public void TestCached() throws IOException, InvalidRangeException {
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = TestDir.cdmUnitTestDir + "agg/nestedAgg/test.ncml";
    boolean ok = true;

    System.out.printf("==========%n");
    for (int i=0; i<2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      ok &= CompareNetcdf2.compareFiles(ncd, ncd2, out, false, false, false);
      System.out.printf("----------------%nfile=%s%n%s%n", filename, out);

      EnumSet<NetcdfDataset.Enhance> modes =  ncd2.getEnhanceMode();
      showModes(modes);
      ncd2.close();
      System.out.printf("==========%n");
    }

    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
    assert ok;
  }  */

}
