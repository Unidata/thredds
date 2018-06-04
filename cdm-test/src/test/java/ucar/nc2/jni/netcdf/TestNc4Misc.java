package ucar.nc2.jni.netcdf;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Test miscellaneous netcdf4 writing
 *
 * @author caron
 * @since 7/30/13
 */
public class TestNc4Misc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
  }

  @Test
  public void testUnlimitedDimension() throws IOException, InvalidRangeException {
    String location = tempFolder.newFile().getAbsolutePath();
    File f = new File(location);
    assert f.delete();

    Variable time;
    try (NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, location)) {
      logger.debug("write to file = {}", new File(location).getAbsolutePath());

      Dimension timeDim = writer.addUnlimitedDimension("time");
      List<Dimension> dims = new ArrayList<>();
      dims.add(timeDim);
      time = writer.addVariable(null, "time", DataType.DOUBLE, dims);

      writer.create();

      Array data = Array.makeFromJavaArray(new double[]{0, 1, 2, 3});
      writer.write(time, data);
      writer.close();

      try (NetcdfFileWriter writer2 = NetcdfFileWriter.openExisting(location)) {
        time = writer2.findVariable("time");
        int[] origin = new int[1];
        origin[0] = (int) time.getSize();
        writer2.write(time, origin, data);
      }

      try (NetcdfFile file = NetcdfFile.open(location)) {
        time = file.findVariable("time");
        assert time.getSize() == 8 : "failed to append to unlimited dimension";
      }
    }
  }


  // from  Jeff Johnson  jeff.m.johnson@noaa.gov   5/2/2014
  @Test
  public void testChunkStandard() throws IOException, InvalidRangeException {
    // define the file
    String location = tempFolder.newFile().getAbsolutePath();

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
    logger.debug("Wrote data file {} size={}", location, resultFile.length());
    assert resultFile.length() < 100 * 1000 : resultFile.length();

    try (NetcdfFile file = NetcdfFile.open(location)) {
      Variable  time  = file.findVariable("time");
      Attribute chunk = time.findAttribute(CDM.CHUNK_SIZES);
      assert chunk != null;
      assert chunk.getNumericValue().equals(1024) : "chunk failed= " + chunk;
    }
  }

  @Ignore("remove ability to set chunk with attribute 2/10/2016")
  @Test
  public void testChunkFromAttribute() throws IOException, InvalidRangeException {
    // define the file
    String location = tempFolder.newFile().getAbsolutePath();

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
    logger.debug("Wrote data file {} size={}", location, resultFile.length());
    assert resultFile.length() < 100 * 1000 : resultFile.length();

    try (NetcdfFile file = NetcdfFile.open(location)) {
      Variable  time  = file.findVariable("time");
      Attribute chunk = time.findAttribute(CDM.CHUNK_SIZES);
      assert chunk != null;
      assert chunk.getNumericValue().equals(2000) : "chunk failed= " + chunk;
    }
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

    NetcdfFileWriter.Version[] versions = new NetcdfFileWriter.Version[]{
            NetcdfFileWriter.Version.netcdf3,
            NetcdfFileWriter.Version.netcdf4
    };

    /**
     * We are looping over the two formats here to test writing of each format.
     */
    for (NetcdfFileWriter.Version version : versions) {

      logger.debug("Writing version {}", version);

      /**
       * Create the file writer and reserve some extra space in the header
       * so that we can switch between define mode and write mode without
       * copying the file (this may work or may not).
       */
      String fileName = tempFolder.newFile().getAbsolutePath();
      NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, fileName);
      writer.setExtraHeaderBytes(64 * 1024);

      /**
       * Create a variable in the root group.
       */
      Group root = null;
      Variable coordVar = writer.addVariable(root, "coord_ref", DataType.INT, "");

      /**
       * Now create the file and close it.
       */
      writer.create();
      writer.flush();
      writer.close();
      logger.debug("File written {}", fileName);

      /**
       * Now we're going to detect the file format using the HDF 5 library.
       */

      /**
       * Now delete the file, getting ready for the next format.
       */
      File file = new File(fileName);
      logger.debug("file.exists() = {}", file.exists());
      logger.debug("file.length() = {}", file.length());
      logger.debug("file.delete() = {}", file.delete());
  
    } // for
  
  }
  
  @Test
  public void testAttributeChangeNc4() throws IOException {
    Path source = Paths.get(TestDir.cdmLocalTestDataDir + "dataset/testRename.nc4");
    Path target = tempFolder.newFile().toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    doRename(target.toString());
  }
  
  @Test
  public void testAttributeChangeNc3() throws IOException {
    Path source = Paths.get(TestDir.cdmLocalTestDataDir + "dataset/testRename.nc3");
    Path target = tempFolder.newFile().toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    doRename(target.toString());
  }
  
  private void doRename(String filename) throws IOException {
    logger.debug("Rename {}", filename);
    // old and new name of variable
    String oldVarName = "Pressure_reduced_to_MSL_msl";
    String newVarName = "Pressure_MSL";
    // name and value of attribute to change
    String attrToChange = "long_name";
    String newAttrValue = "Long name changed!";
    Array orgData;
    
    try (NetcdfFileWriter ncWriter = NetcdfFileWriter.openExisting(filename)) {
      ncWriter.setRedefineMode(true);
      // rename the variable
      ncWriter.renameVariable(oldVarName, newVarName);
      // get the variable whoes attribute you wish to change
      Variable var = ncWriter.findVariable(newVarName);
      orgData = var.read();
      // create the new attribute (overwrite if it already exists)
      // and add the attribute to the variable
      Attribute newAttr = new Attribute(attrToChange, newAttrValue);
      ncWriter.addVariableAttribute(var, newAttr);
      ncWriter.setRedefineMode(false);
      // write the above changes to the file
    }
    
    try (NetcdfFile ncd = NetcdfFile.open(filename)) {
      Variable var = ncd.findVariable(newVarName);
      Assert.assertNotNull(var);
      logger.debug(" check {}", var.getNameAndDimensions());
      String attValue = ncd.findAttValueIgnoreCase(var, attrToChange, "");
      Assert.assertEquals(attValue, newAttrValue);
      
      Array data = var.read();
      logger.debug("{}", data);
      orgData.resetLocalIterator();
      data.resetLocalIterator();
      while (data.hasNext() && orgData.hasNext()) {
        float val = data.nextFloat();
        float orgval = orgData.nextFloat();
        Assert2.assertNearlyEquals(orgval, val);
      }
    }
  }

  // Demonstrates GitHub issue #718: https://github.com/Unidata/thredds/issues/718
  @Test
  public void testCloseNc4inDefineMode() throws IOException {
    String location = "/some/non/existent/file.nc4";  // We won't actually be creating this, so path doesn't matter.
    Nc4Chunking chunking = Nc4ChunkingDefault.factory(Nc4Chunking.Strategy.standard, 5, true);

    // Should be able to open and close file without an exception. Would fail before the bug fix in this commit.
    try (NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, location, chunking)) {}
  }
}
