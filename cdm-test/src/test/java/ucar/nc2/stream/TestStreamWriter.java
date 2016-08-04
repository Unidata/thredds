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
package ucar.nc2.stream;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * test FileWriting, then reading back and comparing to original.
 */

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestStreamWriter {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/Surface_METAR_20080205_0000.nc"});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/grid/RUC2_CONUS_40km_20070709_1800.nc"});

    return result;
  }

  String endpoint;

  public TestStreamWriter(String endpoint) {
    this.endpoint = endpoint;
  }

  @Test
  public void testN3outputStreamWriter() throws IOException {
    System.out.println("\nFile= " + endpoint + " size=" + new File(endpoint).length());
    NetcdfFile fileIn = NetcdfFile.open(endpoint);

    long start = System.currentTimeMillis();
    String fileOut = TestDir.temporaryLocalDataDir + "/testStream.nc";
    N3outputStreamWriter.writeFromFile(fileIn, fileOut);
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");

    NetcdfFile file2 = NetcdfFile.open(fileOut);
    assert ucar.unidata.util.test.CompareNetcdf.compareFiles(fileIn, file2, true, false, false);

    fileIn.close();
    file2.close();
    System.out.println("testFileWriter took " + took + " msecs");
  }

  @Test
  public void testN3channelWriter() throws IOException, InvalidRangeException {
    System.out.println("\nFile= " + endpoint + " size=" + new File(endpoint).length());
    NetcdfFile fileIn = NetcdfFile.open(endpoint);

    long start = System.currentTimeMillis();
    String fileOut = TestDir.temporaryLocalDataDir + "/testChannel.nc";
    N3channelWriter.writeFromFile(fileIn, fileOut);
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");

    NetcdfFile file2 = NetcdfFile.open(fileOut);
    assert ucar.unidata.util.test.CompareNetcdf.compareFiles(fileIn, file2, true, false, false);

    fileIn.close();
    file2.close();
    System.out.println("N3channelWriter took " + took + " msecs");
  }

  @Test
  public void testFileWriter() throws IOException, InvalidRangeException {
    System.out.println("\nFile= " + endpoint + " size=" + new File(endpoint).length());
    NetcdfFile fileIn = NetcdfFile.open(endpoint);

    long start = System.currentTimeMillis();
    String fileOut = TestDir.temporaryLocalDataDir + "/testStream.nc";
    //   public FileWriter2(NetcdfFile fileIn, String fileOutName, NetcdfFileWriter.Version version, Nc4Chunking chunker) throws IOException {
    FileWriter2 writer = new FileWriter2(fileIn, fileOut, NetcdfFileWriter.Version.netcdf3, null);
    NetcdfFile ncout2 = writer.write();
    ncout2.close();
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");

    NetcdfFile file2 = NetcdfFile.open(fileOut);
    assert ucar.unidata.util.test.CompareNetcdf.compareFiles(fileIn, file2, true, false, false);

    fileIn.close();
    file2.close();
    System.out.println("N3streamWriter took " + took + " msecs");
  }

}
