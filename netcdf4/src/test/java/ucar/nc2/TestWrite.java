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

package ucar.nc2;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.util.test.TestDir;

/**
 * Test nc2 write JUnit framework.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWrite {
  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private boolean show = false;
  private static String writerLocation;

  @BeforeClass
  public static void setupClass() throws IOException {
    writerLocation = tempFolder.newFile("testWrite2.nc").getAbsolutePath();
  }

  /** Test the cloning of files with vlen in order to demonstrate vlen variable writting. */
  @Test
  public void testNC4CloningVlen() {

    // GIVEN
    final String filename = tempFolder.getRoot().getAbsolutePath() + "/test-vlen.nc";

    // -- A nc with VLEN
    final NetcdfFile ncfile = TestDir
        .openFileLocal("vlen-test/SGB1-HKT-00-SRC_C_EUMT_20181109084333_G_O_20170103000340_20170103000359_O_N____.nc");

    // WHEN
    try (NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filename)) {
      TestUtils.cloneMetadata(ncfile, writer);
      writer.create();
      TestUtils.cloneData(ncfile, writer);

    } catch (IOException | InvalidRangeException e) {
      Assert.fail(e.getMessage());
    }

    // THEN
    File f = new File(filename);
    Assert.assertTrue(f.exists());
  }

}
