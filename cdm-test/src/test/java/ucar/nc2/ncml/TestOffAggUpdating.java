/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ncml;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCacheable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Updating aggregation
 *
 * @author caron
 * @since Jul 24, 2009
 */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggUpdating {
  String dir = TestDir.cdmUnitTestDir + "agg/updating";
  String location = dir + "agg/updating.ncml";
  File dirFile = new File(dir);
  String extraFile = dir + "/extra.nc";

  String ncml =
          "<?xml version='1.0' encoding='UTF-8'?>\n" +
                  "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                  "       <aggregation dimName='time' type='joinExisting' recheckEvery='1 msec'>\n" +
                  "         <scan location='" + dir + "' suffix='*.nc' />\n" +
                  "         <variable name='depth'>\n" +
                  "           <attribute name='coordinates' value='lon lat'/>\n" +
                  "         </variable>\n" +
                  "         <variable name='wvh'>\n" +
                  "           <attribute name='coordinates' value='lon lat'/>\n" +
                  "         </variable>\n" +
                  "       </aggregation>\n" +
                  "       <attribute name='Conventions' type='String' value='CF-1.0'/>\n" +
                  "</netcdf>";

  @Before
  public void setup() {
    assert dirFile.exists();
    assert dirFile.isDirectory();
    assert dirFile.listFiles() != null;
  }

  @Test
  public void testUpdateSync() throws IOException, InvalidRangeException, InterruptedException {
     // make sure that the extra file is not in the agg
    move(extraFile);

    // open the agg
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);
    check(ncfile, 12);

    // now make sure that the extra file is  in the agg
    moveBack(extraFile);

    // reread
    ncfile.syncExtend();
    check(ncfile, 18);

    ncfile.close();
  }

  @Test
  public void testUpdateLastModified() throws IOException, InvalidRangeException, InterruptedException {
     // make sure that the extra file is not in the agg
    move(extraFile);

    // open the agg
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);
    long start = ncfile.getLastModified();

    // now make sure that the extra file is  in the agg
    moveBack(extraFile);

    // reread
    long end = ncfile.getLastModified();
    assert (end > start);

    // again
    long end2 = ncfile.getLastModified();
    assert (end == end2);

    ncfile.close();
  }

  @Test
  public void testUpdateCache() throws IOException, InvalidRangeException, InterruptedException {
     // make sure that the extra file is not in the agg
    move(extraFile);

    // open the agg
    NetcdfFile ncfile = NetcdfDataset.acquireDataset(new NcmlStringFileFactory(), location, null, -1, null, null);

    check(ncfile, 12);

    // now make sure that the extra file is in the agg
    moveBack(extraFile);

    // reread
    ncfile.syncExtend();
    check(ncfile, 18);

    ncfile.close();
  }

  private void check(NetcdfFile ncfile, int n) throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("time");
    assert v != null;
    System.out.printf(" time= %s%n", v.getNameAndDimensions());
    assert v.getSize() == n : v.getSize();

    v = ncfile.findVariable("eta");
    assert v != null;
    assert v.getRank() == 3 : v.getRank();
  }

  private class NcmlStringFileFactory implements ucar.nc2.util.cache.FileFactory {

    @Override
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return NcMLReader.readNcML(new StringReader(ncml), location, null);
    }
  }

  public static boolean move(String filename) throws IOException {
    Path src = Paths.get(filename);
    if (!Files.exists(src)) return false;
    Path dest = Paths.get(filename+".save");
    Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    return true;
  }


  public static boolean moveBack(String filename) throws IOException {
    Path src = Paths.get(filename+".save");
    if (!Files.exists(src)) return false;
    Path dest = Paths.get(filename);
    Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    return true;
  }
}

