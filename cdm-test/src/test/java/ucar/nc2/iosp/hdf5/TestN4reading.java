/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * Test netcdf-4 reading of misc files
 */
@Category(NeedsCdmUnitTest.class)
public class TestN4reading {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf4/";

  @Test
  public void testGodivaFindsDataHole() throws IOException, InvalidRangeException {
    // this pattern of reads from godiva is finding a data hole - missing data where there shouldn't be any
    Section[] sections = {
      new Section("14:14,0:0,13:170,0:20"),
      new Section("14:14,0:0,170:194,21:167"),
      new Section("14:14,0:0,170:194,168:294"),
      new Section("14:14,0:0,13:170,21:167"),
      new Section("14:14,0:0,170:194,0:20"),
      new Section("14:14,0:0,0:0,0:0"),
      new Section("14:14,0:0,13:170,168:294"),
      new Section("14:14,0:0,0:0,0:0"),
      new Section("14:14,0:0,0:0,0:0"),
      new Section("14:14,0:0,0:12,0:20"),
      new Section("14:14,0:0,0:12,21:167"),
      new Section("14:14,0:0,0:12,168:294"),
    };

    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir+"hiig_forec_20140208.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable("temp");
      for (Section sect : sections) {
        Array data = v.read(sect);
        if (0 < countMissing(data)) {
          Array data2 = v.read(sect);
          countMissing(data2);
          assert false;
        }
      }
      logger.debug("**** testGodivaFindsDataHole read ok on {}", ncfile.getLocation());
    }
  }

  private int countMissing(Array data) {
    int count = 0;
    while (data.hasNext()) {
       float val = data.nextFloat();
       if (val == N3iosp.NC_FILL_FLOAT) {
         count++;
       }
     }
     logger.debug(" missing= {}/{}", count, data.getSize());
    return count;
  }


  @Test
  public void testMultiDimscale() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir+"multiDimscale.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable("siglev");
      v.read();
      v = ncfile.findVariable("siglay");
      v.read();
      logger.debug("**** testMultiDimScale read ok\n{}", ncfile);
    }
  }

  @Test
  public void testGlobalHeapOverun() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir+"globalHeapOverrun.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testGlobalHeapOverun done\n{}", ncfile);
      List<Variable> vars = ncfile.getVariables();
      Collections.sort(vars);
      for (Variable v : vars) logger.debug("  {}", v.getFullName());
      logger.debug("nvars = {}", ncfile.getVariables().size());
    }
  }

  @Test
  public void testEnums() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"tst/tst_enums.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      List<Variable> vars = ncfile.getVariables();
      Collections.sort(vars);
      for (Variable v : vars) logger.debug("  {}", v.getFullName());
      logger.debug("nvars = {}", ncfile.getVariables().size());
    }
  }


  @Test
  public void testVlenStrings() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"tst/tst_strings.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      Variable v = ncfile.findVariable("measure_for_measure_var");
      Array data = v.read();
      logger.debug(NCdumpW.toString(data, "measure_for_measure_var", null));
    }
  }

  @Test
  public void testVlen() throws IOException, InvalidRangeException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    //String filename = "C:/data/work/bruno/fpsc_d1wave_24-11.nc";
    String filename = testDir+"vlen/fpcs_1dwave_2.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testVlen open\n{}", ncfile);
      Variable v = ncfile.findVariable("levels");
      Array data = v.read();
      logger.debug(NCdumpW.toString(data, "read()", null));

      int count = 0;
      while (data.hasNext()) {
        Array as = (Array) data.next();
        logger.debug(NCdumpW.toString(as, " " + count, null));
        count++;
      }

      // try subset
      data = v.read("0:9:2, :");
      logger.debug(NCdumpW.toString(data, "read(0:9:2,:)", null));

      data = v.read(new Section().appendRange(0, 9, 2).appendRange(null));
      logger.debug(NCdumpW.toString(data, "read(Section)", null));

      // fail
      //int[] origin = new int[] {0, 0};
      //int[] size = new int[] {3, -1};
      //data = v.read(origin, size);

      // from bruno
      int initialIndex = 5;
      int finalIndex = 5;
      data = v.read(initialIndex + ":" + finalIndex + ",:");
      //NCdumpW.printArray(data, "read()",  new PrintWriter(System.out), null);

      logger.debug("Size: {}", data.getSize());
      logger.debug("Data: {}", data);
      logger.debug("Class: {}", data.getClass().getName());
      // loop over outer dimension

      while (data.hasNext()) {
        Array as = (Array) data.next(); // inner variable length array of short
        logger.debug("Shape: {}", new Section(as.getShape()));
        logger.debug(as.toString());
      }
    }
  }

  @Test
  public void testVlen2() throws IOException, InvalidRangeException {
    String filename = testDir+"vlen/tst_vlen_data.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testVlen2 open\n{}", ncfile);
      Variable v = ncfile.findVariable("ragged_array");
      Array data = v.read();
      logger.debug(NCdumpW.toString(data, "read()", null));

      assert data instanceof ArrayObject;
      int row = 0;
      while (data.hasNext()) {
        Object vdata = data.next();
        assert vdata instanceof Array;
        assert vdata instanceof ArrayFloat;
        assert vdata instanceof ArrayFloat.D1;
        logger.debug("{} len {}", row++, ((Array) vdata).getSize());
      }

      // try subset
      data = v.read("0:4:2,:");
      logger.debug(NCdumpW.toString(data, "read(0:4:2,:)", null));
      assert data instanceof ArrayObject;
      row = 0;
      while (data.hasNext()) {
        Object vdata = data.next();
        assert vdata instanceof Array;
        assert vdata instanceof ArrayFloat;
        assert vdata instanceof ArrayFloat.D1;
        logger.debug("{} len {}", row++, ((Array) vdata).getSize());
      }

      try {
        // should fail
        data = v.read(":,0");
        assert false;
      } catch (InvalidRangeException e) {
        assert true;
      }

    }

  }

  /*
  netcdf Q:/cdmUnitTest/formats/netcdf4/testNestedStructure.nc {
    variables:
      Structure {
        Structure {
          int x;
          int y;
        } field1;
        Structure {
          int x;
          int y;
        } field2;
      } x;
  }
   */
  @Test
  public void testNestedStructure() throws java.io.IOException, InvalidRangeException {
    String filename = testDir+"testNestedStructure.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {

      Variable dset = ncfile.findVariable("x");
      assert (null != ncfile.findVariable("x"));
      assert (dset.getDataType() == DataType.STRUCTURE);
      assert (dset.getRank() == 0);
      assert (dset.getSize() == 1);

      ArrayStructure data = (ArrayStructure) dset.read();
      StructureMembers.Member m = data.getStructureMembers().findMember("field2");
      assert m != null;
      assert (m.getDataType() == DataType.STRUCTURE);

      logger.debug("{}", NCdumpW.toString(data, "", null));

    }
    logger.debug("*** testNestedStructure ok");
  }

  @Test
  public void testStrings() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"files/nc_test_netcdf4.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      Variable v = ncfile.findVariable("d");
      String attValue = ncfile.findAttValueIgnoreCase(v, "c", null);
      String s = Misc.showBytes(attValue.getBytes(CDM.utf8Charset));
      logger.debug(" d:c = ({}) = {}", attValue, s);
      //Array data = v.read();
      //NCdumpW.printArray(data, "cr", System.out, null);
    }
  }

  @Test
  public void testAttStruct() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestN4reading.testDir + "attributeStruct.nc")) {
      Variable v = ncfile.findVariable("observations");
      assert v != null;
      assert v instanceof Structure;

      Structure s = (Structure) v;
      Variable v2 = s.findVariable("tempMin");
      assert v2 != null;
      assert v2.getDataType() == DataType.FLOAT;

      assert null != v2.findAttribute("units");
      assert null != v2.findAttribute("coordinates");

      Attribute att = v2.findAttribute("units");
      assert att.getStringValue().equals("degF");

    }
  }

  @Test
  public void testAttStruct2() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestN4reading.testDir + "compound-attribute-test.nc")) {
      Variable v = ncfile.findVariable("compound_test");
      assert v != null;
      assert v instanceof Structure;

      Structure s = (Structure) v;
      Variable v2 = s.findVariable("field0");
      assert v2 != null;
      assert v2.getDataType() == DataType.FLOAT;

      Attribute att = v2.findAttribute("att_primitive_test");
      assert !att.isString();
      assert att.getNumericValue().floatValue() == 1.0;

      att = v2.findAttribute("att_string_test");
      assert att.getStringValue().equals("string for field 0");

      att = v2.findAttribute("att_char_array_test");
      assert att.getStringValue().equals("a");

    }
  }

  @Test
  public void testEmptyAtts() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestN4reading.testDir + "testEmptyAtts.nc")) {
      logger.debug("{}", ncfile);
    }
  }

  @Test
  public void testCompoundVlens() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"vlen/cdm_sea_soundings.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}", ncfile);
      Variable v = ncfile.findVariable("fun_soundings");
      Array data = v.read();
      logger.debug(NCdumpW.toString(data, "fun_soundings", null));

      assert data instanceof ArrayStructure;
      ArrayStructure as = (ArrayStructure) data;
      int index = 2;
      String member = "temp_vl";
      StructureData sdata = as.getStructureData(2);
      Array vdata = sdata.getArray("temp_vl");
      assert vdata instanceof ArrayFloat;
      logger.debug("the {} record has {} elements for vlen member {}\n", index, vdata.getSize(), member);
      assert vdata.getSize() == 3;
      Index ii = vdata.getIndex();
      Assert2.assertNearlyEquals(vdata.getFloat(ii.set(2)), 21.5);

      String memberName = "temp_vl";
      int count = 0;
      Structure s = (Structure) v;
      StructureDataIterator siter = s.getStructureIterator();
      siter.reset();
      while (siter.hasNext()) {
        StructureData sdata2 = siter.next();
        Array vdata2 = sdata2.getArray(memberName);
        logger.debug("iter {} has {} elements for vlen member {}", count++, vdata2.getSize(), memberName);
      }
    }
  }

  /*
  Structure {
    int shutterPositionA;
    int shutterPositionD;
    int shutterPositionB;
    int shutterPositionC;
    int dspGainMode;
    int coneActiveStateA;
    int coneActiveStateD;
    int coneActiveStateB;
    int coneActiveStateC;
    int loopDataA(1, *, *);
    int loopDataB(1, *, *);
    long sampleVtcw;
  } tim_records(time=29);

   */
  @Test
  public void testCompoundVlens2() throws IOException {
    String filename = testDir+"vlen/IntTimSciSamp.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      logger.debug("**** testReadNetcdf4 done\n{}" + ncfile);
      Variable v = ncfile.findVariable("tim_records");
      int[] vshape = v.getShape();
      Array data = v.read();
      logger.debug(NCdumpW.toString(data, v.getFullName(), null));

      assert data instanceof ArrayStructure;
      ArrayStructure as = (ArrayStructure) data;
      assert as.getSize() == vshape[0];  //   int loopDataA(1, *, *);
      StructureData sdata = as.getStructureData(0);
      Array vdata = sdata.getArray("loopDataA");
      logger.debug(NCdumpW.toString(vdata, "loopDataA", null));

      assert vdata instanceof ArrayObject;
      Object o1 = vdata.getObject(0);
      assert o1 instanceof Array;
      assert o1 instanceof ArrayInt;       // i thought maybe there would be 2. are we handling this correctly ??

      ArrayInt datai = (ArrayInt)o1;
      assert datai.getSize() == 2;
      Index ii = datai.getIndex();
      assert datai.get(ii.set(1)) == 50334;
    }
  }
}
