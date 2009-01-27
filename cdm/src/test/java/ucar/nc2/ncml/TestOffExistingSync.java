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
package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.nc2.*;
import ucar.nc2.util.DiskCache2;
import ucar.ma2.*;

public class TestOffExistingSync extends TestCase {

  public TestOffExistingSync(String name) {
    super(name);
  }

  String dataDir = "//zero/share/testdata/image/testSync/";

  public void testTiming() throws IOException, InterruptedException {
    String filename = "file:C:/TEMP/aggManyFiles/TestAggManyFiles.ncml";
    Aggregation.setPersistenceCache( new DiskCache2("/.unidata/cachePersist", true, 60 * 24 * 30, 60));

    long start = System.currentTimeMillis();
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    long ending = System.currentTimeMillis();
    double secs = .001 * (ending - start);
    System.out.println("that took "+secs+" secs");
    ncfile.close();
  }

  public void testSync() throws IOException, InterruptedException {
    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    String filename = "file:./" + TestNcML.topDir + "offsite/aggNewSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 7);

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }

  public void testSyncRemove() throws IOException, InterruptedException {
    String filename = "file:./" + TestNcML.topDir + "offsite/aggNewSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");

    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, 7);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }


  public void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize();
    assert time.getShape()[0] == n;
    assert time.getDataType() == DataType.STRING;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == String.class;

    String prev = null;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      String curr = (String) dataI.getObjectNext();
      System.out.println(" coord=" + curr);
      assert (prev == null) || prev.compareTo(curr) < 0;
      prev = curr;
    }
  }

  void move(String filename) {
    File f = new File(filename);
    if (f.exists())
      f.renameTo(new File(filename + ".save"));
  }

  void moveBack(String filename) {
    File f = new File(filename + ".save");
    f.renameTo(new File(filename));
  }

  public static void main(String args[]) throws IOException {
    String dirName = "C:/temp/aggManyFiles/";
    int n = 20000;

    File dir = new File(dirName);
    if (!dir.exists())
      dir.mkdir();

    int count = 0;
    String stub = "Test";
    NetcdfFile org = NetcdfFile.open("C:/data/CM2006172_180000h_u25h.nc");
    for(int i=0; i<n; i++) {
      String fileOut = dirName+stub + i +".nc";

      // munge the time coordinate
      Variable v = org.findVariable("time");
      ArrayDouble.D1 data = (ArrayDouble.D1) v.read();
      data.set(0, (double) count++);
      v.setCachedData(data, false);

      //write the file
      NetcdfFile result = FileWriter.writeToFile(org, fileOut);
      result.close();
    }


  }


}


