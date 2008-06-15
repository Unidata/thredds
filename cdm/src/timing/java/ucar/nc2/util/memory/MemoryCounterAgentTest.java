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

package ucar.nc2.util.memory;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
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
    System.out.printf("%n %4d %6d %s (%s)%n", memShallow, memDeep, name, o.getClass().getName() );
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
    testN3();
    System.out.println("======================");
    //testNcd();
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

  static void testGrib() throws IOException {
    String filename = "C:/data/NAM_CONUS_80km_20070322_0000.grib1";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    measureSize(filename, ncfile, null, true);

    Variable vv = ncfile.findVariable("Absolute_vorticity");
    measureSize(vv.getName(), vv, Group.class, true);

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
    NetcdfDataset ncfile = NetcdfDataset.openDataset("C:/data/test2.nc");
    measureSize("C:/data/test2.nc", ncfile, null, true);

    measureSize( "info", ncfile.getInfo(), null, true);
    StringBuilder pifo = ncfile.getInfo().getParseInfo();
    System.out.println("info= "+pifo);
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
}
