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

package ucar.nc2.util.cache;

import junit.framework.TestCase;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.TestLocal;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ncml.TestNcML;
import ucar.nc2.ncml.NcMLReader;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.util.Formatter;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 7, 2008
 */
public class TestAggCleanup extends TestCase {

  public TestAggCleanup( String name) {
    super(name);
  }

  FileCache cache;
  FileFactory factory = new MyFileFactory();
  protected void setUp() throws java.lang.Exception {
    cache = new FileCache(5, 100, 60 * 60);
  }

  class MyFileFactory implements FileFactory {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return NetcdfDataset.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }


  String ncml =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
    "    <aggregation dimName='time' type='joinExisting'>\n" +
    "      <scan location='D:/data/fileCache/dir%' suffix='.nc' />\n" +
    "    </aggregation>\n" +
    "</netcdf>";

  public void testCache() throws IOException, InvalidRangeException, InterruptedException {
    String filename = "file:"+ TestNcML.topDir + "offsite/aggExistingSSTA.xml";

    RandomAccessFile.setDebugLeaks( true);
    List<String> openfiles = RandomAccessFile.getOpenFiles();
    int count = openfiles.size();
    System.out.println("starting files="+count);
    NetcdfDataset.initNetcdfFileCache(0, 2, 5, 10*60);

    for (int i=0; i<10; i++) {
      String ncmli = StringUtil.replace(ncml,'%',Integer.toString(i));
      NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncmli), filename, null);
      System.out.println(" TestNcmlAggExisting.open "+ filename);

      Variable timev = ncfile.findVariable("time");
      Array timeD = timev.read();

      int count1 = RandomAccessFile.getOpenFiles().size();
      System.out.printf("count files at dir %d count= %d%n", i, count1);
      ncfile.close();
    }

    int count2 = RandomAccessFile.getOpenFiles().size();
    System.out.println("ending files="+count2);
    //assert count == count2 : "openFile count "+count +"!="+ count2;

    Thread.currentThread().sleep(1000 * 10); // sleep 10 seconds
    System.out.println("exiting files="+count2);
  }
}
