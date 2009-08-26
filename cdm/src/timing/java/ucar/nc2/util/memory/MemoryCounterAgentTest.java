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

package ucar.nc2.util.memory;

import ucar.nc2.*;
import ucar.nc2.ncml.TestNcML;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.ma2.Array;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.IOException;

public class MemoryCounterAgentTest {

  public static void measureSize(Object o) {
    long memShallow = MemoryCounterAgent.sizeOf(o);
    long memDeep = MemoryCounterAgent.deepSizeOf(o);
    System.out.printf("%s, shallow=%d, deep=%d%n",
        o.getClass().getSimpleName(), memShallow, memDeep);
  }

  public static void measureSize(String name, Object o, Class skipClass, boolean show) {
    if (o == null) return;
    long memShallow = MemoryCounterAgent.sizeOf(o);
    long memDeep = MemoryCounterAgent.deepSizeOf3(name, o, skipClass, show);
    System.out.printf("%n %4d %6d %s (%s)%n", memShallow, memDeep, name, o.getClass().getName());
  }

  public static void main(String[] args) throws IOException {
    /* measureSize(new Object());
    measureSize(new HashMap());
    measureSize(new LinkedHashMap());
    measureSize(new ReentrantReadWriteLock());
    measureSize(new byte[1000]);
    measureSize(new boolean[1000]);
    measureSize(new String("Hello World".toCharArray()));
    measureSize("Hello World");
    measureSize(10);
    measureSize(100);
    measureSize(1000);
    measureSize(new Parent());
    measureSize(new Kid());
    measureSize(Thread.State.TERMINATED);  // */

    System.out.println("======================");
    //testGrib1();
    System.out.println("======================");
    testGrib2();
  }

  private static class Parent {
    private int i;
    private boolean b;
    private long l;
  }

  private static class Kid extends Parent {
    private boolean b;
    private float f;
  }

  static void testHDF4() throws IOException {
    String filename = "C:/data/hdf4/MI1B2T_B54_O003734_AN_05.hdf";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    measureSize(filename, ncfile, null, true);
    ncfile.close();
  }

  static void testHDF5() throws IOException {
    String filename = "C:/data/hdf5/HIRDLS2-Aura12h_b033_2000d275.he5";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    //measureSize(filename, ncfile, null, true);

    Variable vv = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Geolocation Fields/SpacecraftAltitude");
    measureSize(vv.getName(), vv, Group.class, true);

    //showAll(ncfile);
    ncfile.close();
  }

  static void testBufr() throws IOException {
    String filename = "C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    measureSize(filename, ncfile, null, true);

    //Variable vv = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Geolocation Fields/SpacecraftAltitude");
    //measureSize(vv.getName(), vv, Group.class, true);

    //showAll(ncfile);
    ncfile.close();
  }

  static void testGrib1() throws IOException {
    String filename = "D:\\data\\grib\\nam\\conus80/NAM_CONUS_80km_20060811_0000.grib1";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    measureSize(filename, ncfile, null, false);

    Variable vv = ncfile.findVariable("Absolute_vorticity");
    measureSize(vv.getName(), vv, Group.class, false);

    ncfile.close();
  }

  static void testGrib2() throws IOException {
    String filename = "D:/datasets/ncep/gfs/global0p5/GFS_Global_0p5deg_20060824_0000.grib2";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    measureSize(filename, ncfile, null, true);

    ncfile.close();
  }

  static void showAll(NetcdfFile ncfile) {
    for (Dimension dim : ncfile.getDimensions())
      measureSize(dim.getName(), dim, Group.class, false);

    for (Variable v : ncfile.getVariables()) {
      measureSize(v.getName(), v, Group.class, false);
      for (Attribute att : v.getAttributes())
        measureSize(att.getName(), att, null, false);
    }

    for (Attribute att : ncfile.getGlobalAttributes())
      measureSize(att.getName(), att, null, false);

    Group root = ncfile.getRootGroup();
    measureSize("rootGroup", root, null, false);
    for (Group g : root.getGroups())
      measureSize(g.getName(), g, null, false);
  }

  static void testNcd() throws IOException {
    String filename = "C:/data/test2.nc";
    NetcdfDataset ncfile = NetcdfDataset.openDataset(filename);
    measureSize("C:/data/test2.nc", ncfile, null, true);

    NetcdfDatasetInfo info = new NetcdfDatasetInfo(filename);
    measureSize("info", info, null, true);
    String pifo = info.getParseInfo();
    System.out.println("info= " + pifo);
    ncfile.close();
  }

  static void testN3() throws IOException {
    NetcdfFile ncfile = NetcdfDataset.openFile("C:/data/test2.nc", null);
    measureSize("beforeRead", ncfile, null, true);

    for (Variable v : ncfile.getVariables()) {
      v.read();
    }
    measureSize("afterRead", ncfile, null, true);

    ncfile.close();
  }

  static void testNcml() throws IOException {
    String filename = "C:/dev/tds/thredds/cdm/src/test/data/ncml/aggUnionSimple.xml";
    NetcdfDataset ncfile = NetcdfDataset.openDataset(filename, false, null);
    measureSize("aggUnionSimple", ncfile, null, true);
    ncfile.close();
  }

}
