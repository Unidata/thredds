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
package ucar.nc2.iosp.grib;

import junit.framework.*;

import java.io.*;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

/** Test writing and reading back. */

public class TestJustRead extends TestCase {
  private boolean show = false;

  public TestJustRead( String name) {
    super(name);
  }

  public void testReadGrib1Files() throws Exception {
    readAllDir( TestAll.testdataDir + "motherlode/grid", "grib1");
  }

  public void testReadGrib2Files() throws Exception {
    readAllDir( TestAll.testdataDir + "motherlode/grid", "grib2");
  }

  public void testReadFiles() throws Exception {
    long start = System.currentTimeMillis();
    readAllDir( "D:\\data\\grib\\nam\\c20s", "");
    long end = System.currentTimeMillis();
    System.out.printf(" that took %d msecs%n", (end-start));
  }

  void readAllDir(String dirName, String suffix) throws Exception {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      String name = f.getAbsolutePath();
      if (!f.isDirectory() && name.endsWith(suffix) && !name.endsWith(".gbx"))
        doOne(name);
    }

    /* for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath(), suffix);
    } */

  }

  private void doOne(String filename) throws Exception {
    System.out.println("read file= "+filename);
    NetcdfFile ncfile = NetcdfDataset.openFile( filename, null);
    System.out.println(" Generating_Process_or_Model="+ncfile.findAttValueIgnoreCase(null, "Generating_Process_or_Model", "NONE"));
    ncfile.close();
  }
}
