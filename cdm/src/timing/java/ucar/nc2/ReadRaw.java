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
package ucar.nc2;

import ucar.nc2.util.IO;

import java.io.*;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.nio.channels.FileChannel;

/**
 * Class Description
 *
 * @author caron
 * @since Aug 8, 2009
 */


public class ReadRaw {
  interface MyClosure {
    void run(String filename) throws IOException;
  }

  interface Filter {
    boolean pass(String filename);
  }

  void test(String filename, Filter filter, MyClosure closure) throws IOException {
    File f = new File(filename);
    if (!f.exists()) {
      if (show) System.out.println(filename + " does not exist");
      return;
    }
    if (!filter.pass(filename)) {
      if (show) System.out.println(filename + " does not pass filter");
      return;
    }
    try {
      closure.run(f.getPath());
    } catch (Exception ioe) {
      System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }


  void testAllInDir(File dir, Filter filter, MyClosure closure) {
    List<File> list = Arrays.asList(dir.listFiles());
    Collections.sort(list);

    for (File f : list) {
      if (f.isDirectory())
        testAllInDir(f, filter, closure);
      else {
        try {
          test(f.getPath(), filter, closure);
        } catch (Exception ioe) {
          System.out.println("Failed on " + f.getPath() + ": " + ioe.getMessage());
          ioe.printStackTrace();
        }
      }
    }
  }

  void run(String what, File dir, Filter filter, MyClosure closure) {
    nfiles = 0;
    nbytes = 0;
    long start = System.nanoTime();
    testAllInDir(dir, filter, closure);
    long stop = System.nanoTime();
    double secs = (stop - start) / nano;
    double rate = nbytes / Mbytes / secs;

    out.format("%s %s buffersize= %d Nfiles = %d Mbytes = %f time = %f Rate = %f Mb/sec %n",
        what, dir, buffersize, nfiles, nbytes / Mbytes, secs, rate);
  }

  ////////////////////////////
  double Mbytes = 1000 * 1000;
  double nano = 1000 * 1000 * 1000;
  Formatter out = new Formatter(System.out);

  int nfiles = 0;
  long nbytes = 0;

  boolean show = false;
  int buffersize = 100000;
  String dirName = "C:\\data\\work\\hawaii";
  String ssdName = "G:/";

  ReadRaw() throws IOException {

    Filter netcdf = new Filter() {
      public boolean pass(String filename) {
        return filename.endsWith(".nc");
      }
    };

    MyClosure readInputStream = new MyClosure() {
      public void run(String filename) throws IOException {
        if (show) out.format("read %s %n", filename);
        InputStream is = new BufferedInputStream ( new FileInputStream(filename), buffersize);
        nbytes += IO.copy2null(is, buffersize);
        nfiles++;
        is.close();
      }
    };

    run("readInputStream", new File(ssdName), netcdf, readInputStream);

    MyClosure readFileChannel = new MyClosure() {
      public void run(String filename) throws IOException {
        if (show) out.format("read %s %n", filename);
        FileInputStream fis = new FileInputStream(filename);
        FileChannel fc = fis.getChannel();
        nbytes += IO.copy2null(fc, buffersize);
        nfiles++;
        fis.close();
      }
    };

    run("readFileChannel", new File(ssdName), netcdf, readFileChannel);
    run("readInputStream", new File(ssdName), netcdf, readInputStream);
    run("readFileChannel", new File(ssdName), netcdf, readFileChannel);
    
  }

   static public void main(String args[]) throws IOException {
     new ReadRaw();
   }
}
