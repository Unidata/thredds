/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * test FileWriting, then reading back and comparing to original.
 */

@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestStreamWriter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

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
    String fileOut = tempFolder.newFile().getAbsolutePath();
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
    String fileOut = tempFolder.newFile().getAbsolutePath();
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
    String fileOut = tempFolder.newFile().getAbsolutePath();
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
