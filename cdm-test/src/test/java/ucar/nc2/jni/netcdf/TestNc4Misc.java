package ucar.nc2.jni.netcdf;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Test miscellaneous netcdf4 writing
 *
 * @author caron
 * @since 7/30/13
 */
public class TestNc4Misc {

  @Before
  public void setLibrary() {
    Nc4Iosp.setLibraryAndPath("/opt/netcdf/lib", "netcdf");
    System.out.printf("Nc4Iosp.isClibraryPresent = %s%n", Nc4Iosp.isClibraryPresent());
  }

  @Test
  public void testUnlimitedDimension() throws IOException, InvalidRangeException {

    String location = TestLocal.temporaryDataDir + "testNc4UnlimitedDim.nc";
    // String location = "C:/temp/testNc4UnlimitedDim.nc";
    File f = new File(location);
    System.out.printf("%s%n", f.exists());
    boolean ok = f.delete();
    System.out.printf("%s%n", ok);

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, location);
    System.out.printf("write to file = %s%n", new File(location).getAbsolutePath());

    Dimension timeDim = writer.addUnlimitedDimension("time");
    List<Dimension> dims = new ArrayList<>();
    dims.add(timeDim);
    Variable time = writer.addVariable(null, "time", DataType.DOUBLE, dims);

    writer.create();

    Array data = Array.factory(new double[]{0, 1, 2, 3});
    writer.write(time, data);
    writer.close();

    NetcdfFileWriter writer2 = NetcdfFileWriter.openExisting(location);

    time = writer2.findVariable("time");
    int[] origin = new int[1];
    origin[0] = (int) time.getSize();
    writer2.write(time, origin, data);
    writer2.close();

    NetcdfFile file = NetcdfFile.open(location);
    time = file.findVariable("time");
    assert time.getSize() == 8 : "failed to append to unlimited dimension";
    file.close();
  }


  // from  Jeff Johnson  jeff.m.johnson@noaa.gov   5/2/2014
  @Test
  public void testChunkStandard() throws IOException, InvalidRangeException {
    // define the file
    String location = TestLocal.temporaryDataDir +"testSizeWriting2.nc4";
    // String location = "C:/temp/testSizeWriting.nc4";

    NetcdfFileWriter dataFile = null;

    try {
      // delete the file if it already exists
      Path path = FileSystems.getDefault().getPath(location);
      Files.deleteIfExists(path);

      // default chunking
      // dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filePathName);

      // define chunking
      Nc4Chunking chunkingStrategy = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, false);
      dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, location, chunkingStrategy);

      // create the root group
      Group rootGroup = dataFile.addGroup(null, null);

      // define dimensions, in this case only one: time
      Dimension timeDim = dataFile.addUnlimitedDimension("time");
      List<Dimension> dimList = new ArrayList<>();
      dimList.add(timeDim);

      // define variables
      Variable time = dataFile.addVariable(rootGroup, "time", DataType.DOUBLE, dimList);
      dataFile.addVariableAttribute(time, new Attribute("units", "milliseconds since 1970-01-01T00:00:00Z"));

      // create the file
      dataFile.create();

      // create 1-D arrays to hold data values (time is the dimension)
      ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);

      int[] origin = new int[]{0};
      long startTime = 1398978611132L;

      // write the records to the file
      for (int i = 0; i < 10000; i++) {
        // load data into array variables
        double value = startTime++;
        timeArray.set(timeArray.getIndex(), value);

        origin[0] = i;

        // write a record
        dataFile.write(time, origin, timeArray);
      }

    } finally {
      if (null != dataFile) {
        dataFile.close();
      }
    }

    File resultFile = new File(location);
    System.out.printf("Wrote data file %s size=%d%n", location, resultFile.length());
    assert resultFile.length() < 100 * 1000 :  resultFile.length();

    NetcdfFile file = NetcdfFile.open(location);
    Variable time = file.findVariable("time");
    Attribute chunk = time.findAttribute(CDM.CHUNK_SIZES);
    assert chunk != null;
    assert chunk.getNumericValue().equals(1024) : "chunk failed= "+ chunk;
    file.close();
  }

   // from  Jeff Johnson  jeff.m.johnson@noaa.gov   5/2/2014
  @Test
  public void testChunkFromAttribute() throws IOException, InvalidRangeException {
    // define the file
    String location = TestLocal.temporaryDataDir +"testSizeWriting2.nc4";
    // String location = "C:/temp/testSizeWriting.nc4";

    NetcdfFileWriter dataFile = null;

    try {
      // delete the file if it already exists
      Path path = FileSystems.getDefault().getPath(location);
      Files.deleteIfExists(path);

      // default chunking
      // dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filePathName);

      // define chunking
      Nc4Chunking chunkingStrategy = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, false);
      dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, location, chunkingStrategy);

      // create the root group
      Group rootGroup = dataFile.addGroup(null, null);

      // define dimensions, in this case only one: time
      Dimension timeDim = dataFile.addUnlimitedDimension("time");
      List<Dimension> dimList = new ArrayList<>();
      dimList.add(timeDim);

      // define variables
      Variable time = dataFile.addVariable(rootGroup, "time", DataType.DOUBLE, dimList);
      dataFile.addVariableAttribute(time, new Attribute("units", "milliseconds since 1970-01-01T00:00:00Z"));
      dataFile.addVariableAttribute(time, new Attribute(CDM.CHUNK_SIZES, 2000));

      // create the file
      dataFile.create();

      // create 1-D arrays to hold data values (time is the dimension)
      ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);

      int[] origin = new int[]{0};
      long startTime = 1398978611132L;

      // write the records to the file
      for (int i = 0; i < 10000; i++) {
        // load data into array variables
        double value = startTime++;
        timeArray.set(timeArray.getIndex(), value);

        origin[0] = i;

        // write a record
        dataFile.write(time, origin, timeArray);
      }

    } finally {
      if (null != dataFile) {
        dataFile.close();
      }
    }

    File resultFile = new File(location);
    System.out.printf("Wrote data file %s size=%d%n", location, resultFile.length());
    assert resultFile.length() < 100 * 1000 :  resultFile.length();

    NetcdfFile file = NetcdfFile.open(location);
    Variable time = file.findVariable("time");
    Attribute chunk = time.findAttribute(CDM.CHUNK_SIZES);
    assert chunk != null;
    assert chunk.getNumericValue().equals(2000) : "chunk failed= "+ chunk;
    file.close();
  }


  /* from peter@terrenus.ca
  > > Writing version netcdf3
  > > Jan 22, 2015 12:27:49 PM ncsa.hdf.hdf5lib.H5 loadH5Lib
  > > INFO: HDF5 library: jhdf5
  > > Jan 22, 2015 12:27:49 PM ncsa.hdf.hdf5lib.H5 loadH5Lib
  > > INFO: successfully loaded from java.library.path
  > > Found HDF 5 version = false
  > > file.exists() = true
  > > file.length() = 65612
  > > file.delete() = true                       testGri
  > > Writing version netcdf4
  > > Netcdf nc_inq_libvers='4.3.2 of Oct 20 2014 09:49:08 $' isProtected=false
  > > Exception in thread "main" java.io.IOException: -101: NetCDF: HDF error
  > > at ucar.nc2.jni.netcdf.Nc4Iosp.create(Nc4Iosp.java:2253)
  > > at ucar.nc2.NetcdfFileWriter.create(NetcdfFileWriter.java:794)
  > > at NetCDFTest.main(NetCDFTest.java:39)
   */
  @Test
  public void testInvalidCfdid() throws Exception {

    NetcdfFileWriter.Version[] versions = new NetcdfFileWriter.Version[] {
      NetcdfFileWriter.Version.netcdf3,
      NetcdfFileWriter.Version.netcdf4
    };

    /**
     * We are looping over the two formats here to test writing of each format.
     */
    for (NetcdfFileWriter.Version version : versions) {

      System.out.println ("Writing version " + version);

      /**
       * Create the file writer and reserve some extra space in the header
       * so that we can switch between define mode and write mode without
       * copying the file (this may work or may not).
       */
      String fileName = TestDir.temporaryLocalDataDir + "test.nc";
      NetcdfFileWriter writer = NetcdfFileWriter.createNew (version, fileName);
      writer.setExtraHeaderBytes (64*1024);

      /**
       * Create a variable in the root group.
       */
      Group root = null;
      Variable coordVar = writer.addVariable (root, "coord_ref", DataType.INT, "");

      /**
       * Now create the file and close it.
       */
      writer.create();
      writer.flush();
      writer.close();
      System.out.println ("File written " + fileName);

      /**
       * Now we're going to detect the file format using the HDF 5 library.
       */

      /**
       * Now delete the file, getting ready for the next format.
       */
      File file = new File (fileName);
      System.out.println ("file.exists() = " + file.exists());
      System.out.println ("file.length() = " + file.length());
      System.out.println ("file.delete() = " + file.delete());

    } // for

  } // main
}