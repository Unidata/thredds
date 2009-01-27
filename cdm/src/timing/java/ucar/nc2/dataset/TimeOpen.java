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
