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
package ucar.nc2;

import junit.framework.*;

import java.io.*;

import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.nc2.util.CompareNetcdf;
import ucar.ma2.InvalidRangeException;

/**
 * test FileWriting, then reading back and comparing to original.
 */

public class TestStreamWriter extends TestCase {
  static boolean showCompare = true, showEach = false;

  public TestStreamWriter(String name) {
    super(name);
  }

  public void test() throws IOException, InvalidRangeException {

    testChannelWriter("C:/data/metars/Surface_METAR_20070326_0000.nc");
    testChannelWriter("C:/data/RUC2_CONUS_40km_20070709_1800.nc");

    testOutputStreamWriter("C:/data/metars/Surface_METAR_20070326_0000.nc");
    testOutputStreamWriter("C:/data/RUC2_CONUS_40km_20070709_1800.nc");
  }

  private void testOutputStreamWriter(String fileInName) throws IOException {
    System.out.println("\nFile= " + fileInName + " size=" + new File(fileInName).length());
    NetcdfFile fileIn = NetcdfFile.open(fileInName);

    long start = System.currentTimeMillis();
    N3outputStreamWriter.writeFromFile(fileIn, "C:/data/testStream.nc");
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");

    NetcdfFile file2 = NetcdfFile.open("C:/data/testStream.nc");
    CompareNetcdf.compareFiles(fileIn, file2, true, true, false);

    fileIn.close();
    file2.close();
  }

  private void testChannelWriter(String fileInName) throws IOException, InvalidRangeException {
    System.out.println("\nFile= " + fileInName + " size=" + new File(fileInName).length());
    NetcdfFile fileIn = NetcdfFile.open(fileInName);

    long start = System.currentTimeMillis();
    N3channelWriter.writeFromFile(fileIn, "C:/data/testStream.nc");
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");

    NetcdfFile file2 = NetcdfFile.open("C:/data/testStream.nc");
    CompareNetcdf.compareFiles(fileIn, file2, true, true, false);

    fileIn.close();
    file2.close();
  }

  public void utestTime() throws IOException, InvalidRangeException {
    testTime(new String[] {"C:/data/RUC2_CONUS_40km_20070709_1800.nc",
        "C:/data/CopyRUC2_CONUS_40km_20070709_1800.nc",
        "C:/data/Copy2RUC2_CONUS_40km_20070709_1800.nc"});

    testTime(new String[] {"C:/data/metars/Surface_METAR_20070329_0000.nc",
        "C:/data/metars/Surface_METAR_20070330_0000.nc",
        "C:/data/metars/Surface_METAR_20070326_0000.nc"});
  }

  public void testTime(String[] filename) throws IOException, InvalidRangeException {
    System.out.println("\nFile= " + filename[0] + " size=" + new File(filename[0]).length());
 
    NetcdfFile fileIn = NetcdfFile.open(filename[0]);
    File fileOut = File.createTempFile("Test", ".tmp", new File("C:/data/"));
    long start = System.currentTimeMillis();
    N3outputStreamWriter.writeFromFile(fileIn, fileOut.getPath());
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");  // */
    fileIn.close();

    NetcdfFile fileIn2 = NetcdfFile.open(filename[1]);
    File fileOut2 = File.createTempFile("Test", ".tmp", new File("C:/data/"));
    long start2 = System.currentTimeMillis();
    NetcdfFile ncout2 = FileWriter.writeToFile(fileIn2, fileOut2.getPath());
    ncout2.close();
    long took2 = System.currentTimeMillis() - start2;
    System.out.println("FileWriter took " + took2 + " msecs");
    fileIn2.close();

    NetcdfFile fileIn3 = NetcdfFile.open(filename[2]);
    File fileOut3 = File.createTempFile("Test", ".tmp", new File("C:/data/"));
    long start3 = System.currentTimeMillis();
    N3channelWriter.writeFromFile(fileIn3, fileOut3.getPath());
    long took3 = System.currentTimeMillis() - start3;
    System.out.println("N3channelWriter took " + took3 + " msecs");
    fileIn3.close();
  }

}