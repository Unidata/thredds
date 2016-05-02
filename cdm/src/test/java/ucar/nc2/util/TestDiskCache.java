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

package ucar.nc2.util;

import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import java.io.File;

/**
 * Test DiskCache2
 *
 * @author caron
 * @since 7/21/2014
 */
public class TestDiskCache {


  // https://github.com/Unidata/thredds/issues/58  from Cameron Beccario
  @Test
  public void testNotExist() throws Exception {
    DiskCache2 cache = DiskCache2.getDefault();
    File file = cache.getFile("gfs.t00z.master.grbf00.10m.uv.grib2"); // not exist
    System.out.printf("canWrite= %s%n", file.canWrite());
    assert !file.canWrite();
  }

  public void testReletivePath() throws Exception {
    String org = System.getProperty("user.dir");
    try {
      System.setProperty("user.dir", TestDir.cdmUnitTestDir);
      System.out.printf("user.dir = %s%n", System.getProperty("user.dir"));
      File pwd = new File(System.getProperty("user.dir"));

      String filename = "transforms/albers.nc";
      File rel2 = new File(pwd, filename);
      System.out.printf("abs = %s%n", rel2.getCanonicalFile());
      assert rel2.exists();
      assert rel2.canWrite();
    } finally {
      System.setProperty("user.dir", org);
    }
  }

}
