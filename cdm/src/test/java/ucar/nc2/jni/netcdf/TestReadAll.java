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

package ucar.nc2.jni.netcdf;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.TestCompare;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 31, 2008
 */
public class TestReadAll extends TestCase {
  private JniIosp iosp;
  private boolean showFile = true;
  private boolean showDetail = false;

  public void setUp() {
    iosp = new JniIosp();
  }

  public TestReadAll(String name) {
    super(name);
  }

  public void testReadAll() throws IOException {
    int count = 0;
    count += scanAllDir("C:/testdata/", new NetcdfFileFilter(), new ReadAllData());
    System.out.println("***READ " + count + " files");
  }

  public void testReadOne() throws IOException {
    new ReadAllData().doClosure("C:/testdata/netcdf4/tst_enum_data.nc");
    //new ReadAllData().doClosure("C:/data/test2.nc");
  }

  public void testCompareAll() throws IOException {
    int count = 0;
    count += scanAllDir("C:/data/", new NetcdfFileFilter(), new CompareData());
    System.out.println("***READ " + count + " files");
  }

  public void testCompareOne() throws IOException {
    new CompareData().doClosure("C:/testdata/netcdf4/tst_solar_1.nc");
  }


  class NetcdfFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".nc");
    }
  }

  private int scanAllDir(String dirName, FileFilter ff, Closure c) {
    int count = 0;

    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude"))
        count += c.doClosure(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += scanAllDir(f.getAbsolutePath(), ff, c);
    }

    return count;
  }

  private interface Closure {
    int doClosure(String filename);
  }

  private class CompareData implements Closure {
    public int doClosure(String filename) {
      System.out.println("\n------Compare filename " + filename);
      NetcdfFile ncfile = null;
      NetcdfFile ncfileC = null;
      try {
        ncfileC = iosp.open(filename);
        ncfile = NetcdfFile.open(filename);
        TestCompare.compareFiles(ncfile, ncfileC, true, false, false);

      } catch (Exception e) {
        e.printStackTrace();

      } finally {

        if (ncfileC != null)
          try {
            ncfileC.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }

        if (ncfile != null)
          try {
            ncfile.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }

      }
      return 1;
    }
  }

  private class ReadAllData implements Closure {
    public int doClosure(String filename) {
      System.out.println("\n------Reading filename " + filename);
      NetcdfFile ncfile = null;
      try {
        ncfile = iosp.open(filename);
        if (showFile) System.out.println(ncfile.toString());

        for (Variable v : ncfile.getVariables()) {
          if (v.getSize() > max_size) {
            Section s = makeSubset(v);
            if (showDetail)
              System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
            v.read(s);
          } else {
            if (showDetail)
              System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
            v.read();
          }
        }

      } catch (Exception e) {
        e.printStackTrace();

      } finally {
        if (ncfile != null)
          try {
            ncfile.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }
      }

      return 1;
    }

    int max_size = 1000 * 1000 * 10;

    Section makeSubset(Variable v) throws InvalidRangeException {
      int[] shape = v.getShape();
      shape[0] = 1;
      Section s = new Section(shape);
      long size = s.computeSize();
      shape[0] = (int) Math.max(1, max_size / size);
      return new Section(shape);
    }
  }


}
