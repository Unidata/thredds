/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTaskImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test writing structure data into netcdf4.
 *
 * @author caron
 * @since 5/12/14
 */
@Category(NeedsCdmUnitTest.class)
public class TestNc4Structures {
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
  public void writeStructureFromNids() throws IOException, InvalidRangeException {
    //String datasetIn = TestDir.cdmUnitTestDir  + "formats/nexrad/level3/KBMX_SDUS64_NTVBMX_201104272341";
    String datasetIn = TestDir.cdmUnitTestDir  + "formats/nexrad/level3/NVW_20041117_1657";
    String datasetOut = tempFolder.newFile().getAbsolutePath();
    writeStructure(datasetIn, datasetOut);
  }

  @Test
  public void writeStructure() throws IOException, InvalidRangeException {
    String datasetIn = TestDir.cdmUnitTestDir  + "formats/netcdf4/compound/tst_compounds.nc4";
    String datasetOut = tempFolder.newFile().getAbsolutePath();
    writeStructure(datasetIn, datasetOut);
  }

  private void writeStructure(String datasetIn, String datasetOut) throws IOException {
    CancelTaskImpl cancel = new CancelTaskImpl();
    NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, cancel);
    System.out.printf("NetcdfDatataset read from %s write to %s %n", datasetIn, datasetOut);

    FileWriter2 writer = new ucar.nc2.FileWriter2(ncfileIn, datasetOut, NetcdfFileWriter.Version.netcdf4, null);
    NetcdfFile ncfileOut = writer.write(cancel);
    if (ncfileOut != null) ncfileOut.close();
    ncfileIn.close();
    cancel.setDone(true);
    System.out.printf("%s%n", cancel);
  }


  // Demonstrates GitHub issue #296.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeStringMember() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeStringMember", ".nc4");
    try {
      try (NetcdfFileWriter ncFileWriter = NetcdfFileWriter.createNew(
              NetcdfFileWriter.Version.netcdf4, outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileWriter.addVariable(null, "struct", DataType.STRUCTURE, "");
        ncFileWriter.addStructureMember(struct, "foo", DataType.STRING, null);

        ncFileWriter.create();

        // Write data
        ArrayString.D2 fooArray = new ArrayString.D2(1, 1);
        fooArray.set(0, 0, "bar");

        ArrayStructureMA arrayStruct = new ArrayStructureMA(struct.makeStructureMembers(), struct.getShape());
        arrayStruct.setMemberArray("foo", fooArray);

        ncFileWriter.write(struct, arrayStruct);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFile.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.findVariable(null, "struct");
        Assert.assertEquals("bar", struct.readScalarString());
      }
    } finally {
      outFile.delete();
    }
  }

  // Demonstrates GitHub issue #298.
  // I did my best to write test code here that SHOULD work when NetcdfFileWriter and Nc4Iosp are fixed, using
  // TestStructureArrayW as a reference. However, failure happens very early in the test, so it's hard to know if
  // the unreached code is 100% correct. It may need to be fixed slightly.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeNestedStructure() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeNestedStructure", ".nc4");
    try {
      try (NetcdfFileWriter ncFileWriter = NetcdfFileWriter.createNew(
              NetcdfFileWriter.Version.netcdf4, outFile.getAbsolutePath())) {
        Structure outer = (Structure) ncFileWriter.addVariable(null, "outer", DataType.STRUCTURE, "");
        Structure inner = (Structure) ncFileWriter.addStructureMember(outer, "inner", DataType.STRUCTURE, "");
        ncFileWriter.addStructureMember(inner, "foo", DataType.INT, null);

        ncFileWriter.create();

        // Write data
        ArrayInt.D0 fooArray = new ArrayInt.D0(false);
        fooArray.set(42);

        StructureDataW innerSdw = new StructureDataW(inner.makeStructureMembers());
        innerSdw.setMemberData("foo", fooArray);
        ArrayStructureW innerAsw = new ArrayStructureW(innerSdw);

        StructureDataW outerSdw = new StructureDataW(outer.makeStructureMembers());
        outerSdw.setMemberData("inner", innerAsw);
        ArrayStructureW outerAsw = new ArrayStructureW(outerSdw);

        ncFileWriter.write(outer, outerAsw);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFile.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.findVariable(null, "outer");

        StructureData outerStructureData = struct.readStructure();
        StructureData innerStructureData = outerStructureData.getScalarStructure("inner");
        int foo = innerStructureData.getScalarInt("foo");

        Assert.assertEquals(42, foo);
      }
    } finally {
      outFile.delete();
    }
  }

  // Demonstrates GitHub issue #299.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeUnlimitedLengthStructure() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeUnlimitedLengthStructure", ".nc4");
    try {
      try (NetcdfFileWriter ncFileWriter = NetcdfFileWriter.createNew(
              NetcdfFileWriter.Version.netcdf4, outFile.getAbsolutePath())) {
        // Test passes if we do "isUnlimited == false".
        Dimension dim = ncFileWriter.addDimension(null, "dim", 5, true /*false*/, false);

        Structure struct = (Structure) ncFileWriter.addVariable(null, "struct", DataType.STRUCTURE, "dim");
        ncFileWriter.addStructureMember(struct, "foo", DataType.INT, null);

        ncFileWriter.create();

        // Write data
        ArrayInt.D2 fooArray = new ArrayInt.D2(5, 1, false);
        fooArray.set(0, 0, 2);
        fooArray.set(1, 0, 3);
        fooArray.set(2, 0, 5);
        fooArray.set(3, 0, 7);
        fooArray.set(4, 0, 11);

        ArrayStructureMA arrayStruct = new ArrayStructureMA(struct.makeStructureMembers(), struct.getShape());
        arrayStruct.setMemberArray("foo", fooArray);

        ncFileWriter.write(struct, arrayStruct);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFile.open(outFile.getAbsolutePath())) {
        Array fooArray = ncFileIn.readSection("struct.foo");

        int[] expecteds = new int[] { 2, 3, 5, 7, 11 };
        int[] actuals = (int[]) fooArray.copyToNDJavaArray();
        Assert.assertArrayEquals(expecteds, actuals);
      }
    } finally {
      outFile.delete();
    }
  }
}
