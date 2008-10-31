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

  public void setUp() {
    iosp = new JniIosp();
  }

  public TestReadAll(String name) {
    super(name);
  }

  public void testReadAll() throws IOException {
    int count = 0;
    count += readAllDir("R:/testdata/grid/netcdf/", new NetcdfFileFilter());
    System.out.println("***READ " + count + " files");
  }

  public void testReadOne() throws IOException {
    readAll("R:\\testdata\\grid\\netcdf\\cf\\air.2m.2002.nc");
  }


  class NetcdfFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".nc");
    }
  }

  public static long startTime;

  private void openAllInDir(String dirName, FileFilter ff) throws IOException {
    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if ((ff == null) || ff.accept(f)) {
        System.out.println("  try to open " + name);
        NetcdfFile ncfile = NetcdfFile.open(name);
        ncfile.close();
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        openAllInDir(f.getAbsolutePath(), ff);
    }

  }

  private int readAllDir(String dirName, FileFilter ff) {
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
        count += readAll(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += readAllDir(f.getAbsolutePath(), ff);
    }

    return count;
  }

  private int readAll(String filename) {
    System.out.println("\n------Reading filename " + filename);
    try {
      NetcdfFile ncfile = iosp.open(filename);

      for (Variable v : ncfile.getVariables()) {
        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
          v.read(s);
        } else {
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
          v.read();
        }
      }
      ncfile.close();
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

    return 1;
  }

  static int max_size = 1000 * 1000 * 10;

  static Section makeSubset(Variable v) throws InvalidRangeException {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }


}
