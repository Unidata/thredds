/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
