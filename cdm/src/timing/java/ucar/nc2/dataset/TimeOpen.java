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

package ucar.nc2.dataset;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.*;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.NcMLWriter;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;
import timing.Average;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 4, 2008
 */
public class TimeOpen extends TestCase {

  public TimeOpen( String name) {
    super(name);
  }

  interface MClosure {
    void run(String filename) throws IOException, InvalidRangeException;
  }

  static void testAllInDir(File dir, MClosure closure) throws IOException, InvalidRangeException {
    File[] fa = dir.listFiles();
    if (fa == null || fa.length == 0) return;

    List<File> list = Arrays.asList(fa);
    Collections.sort(list);

    for (File f : list) {
      if (f.isDirectory())
        testAllInDir(f, closure);
      else {
        closure.run(f.getPath());
      }
    }
  }

  public void testWriteNcml() throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    final NcMLWriter writer = new NcMLWriter();

    testAllInDir( new File("C:/data/grib/"), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith("grib1")) return;
        NetcdfFile ncfile = NetcdfDataset.openFile(filename, null);
        File fileout = new File(filename+".ncml");
        if (fileout.exists()) fileout.delete();
        writer.writeXMLexplicit( ncfile, new FileOutputStream(fileout), null);
        System.out.println(" wrote ncml file  ="+fileout);
      }
    });
  }

  public void testOpenFile() throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    //
    testAllInDir( new File("C:/data/grib/"), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith("ncml")) return;
        System.out.println(" open ncml file  ="+filename);
        openFile( filename, fileAvg, true);
      }
    });
    System.out.println(" open ncml file  ="+fileAvg);
  }

  static void openFile(String filename, Average avg, boolean enhance) throws IOException, InvalidRangeException {
    try {
    long start = System.nanoTime();
    NetcdfFile ncfile = enhance ? NetcdfDataset.openDataset(filename) : NetcdfDataset.openFile(filename, null);
    long end = System.nanoTime();
    double took = (double)((end - start))/1000/1000/1000;
    ncfile.close();
    if (avg != null) avg.add(took);
    } catch (Exception e) {
      System.out.println("BAD "+filename);
      e.printStackTrace();
    }
  }

  // testing on remote machines like motherlode
  static void testOpenFile(String dir, final String suffix) throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    //
    testAllInDir( new File(dir), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith(suffix)) return;
        //System.out.println(" open "+suffix+" file  ="+filename);
        openFile( filename, fileAvg, false);
      }
    });
    System.out.println("*** open "+suffix+" files  ="+fileAvg);
  }

  static void testOpenDataset(String dir, final String suffix) throws IOException, InvalidRangeException {
    final Average fileAvg = new Average();
    //
    testAllInDir( new File(dir), new MClosure() {
      public void run(String filename) throws IOException, InvalidRangeException {
        if (!filename.endsWith(suffix)) return;
        //System.out.println(" open "+suffix+" file  ="+filename);
        openFile( filename, fileAvg, true);
      }
    });
    System.out.println("*** open "+suffix+" datasets  ="+fileAvg);
  }

  public static void main(String args[]) throws IOException, InvalidRangeException {
    String dir = args[0];
    String suffix = args[1];
    testOpenFile( dir, suffix);
    testOpenDataset( dir, suffix);
  }


}
