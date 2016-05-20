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
package ucar.nc2.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Test netcdf-4 reading of misc files
 */
@Category(NeedsCdmUnitTest.class)
public class TestN4reading {
  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf4/";


  @Test
  public void testGodivaFindsDataHole() throws IOException, InvalidRangeException {
    // this pattern of reads from godiva is finding a data hole - missing data where therre shouldnt be any
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
      System.out.printf("**** testGodivaFindsDataHole read ok on %s%n", ncfile.getLocation());
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
     System.out.printf(" missing= %d/%d%n", count, data.getSize());
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
      System.out.println("\n**** testMultiDimScale read ok\n\n" + ncfile);
    }
  }

  @Test
  public void testGlobalHeapOverun() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir+"globalHeapOverrun.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      System.out.println("\n**** testGlobalHeapOverun done\n\n" + ncfile);
      List<Variable> vars = ncfile.getVariables();
      Collections.sort(vars);
      for (Variable v : vars) System.out.println(" " + v.getFullName());
      System.out.println("nvars = " + ncfile.getVariables().size());
    }
  }

  @Test
  public void testEnums() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"tst/tst_enums.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      List<Variable> vars = ncfile.getVariables();
      Collections.sort(vars);
      for (Variable v : vars) System.out.println(" " + v.getFullName());
      System.out.println("nvars = " + ncfile.getVariables().size());
    }
  }


  @Test
  public void testVlenStrings() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"tst/tst_strings.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      Variable v = ncfile.findVariable("measure_for_measure_var");
      Array data = v.read();
      NCdumpW.printArray(data, "measure_for_measure_var", new PrintWriter(System.out), null);
    }
  }

  @Test
  public void testVlen() throws IOException, InvalidRangeException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    //String filename = "C:/data/work/bruno/fpsc_d1wave_24-11.nc";
    String filename = testDir+"vlen/fpcs_1dwave_2.nc";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      Variable v = ncfile.findVariable("levels");
      Array data = v.read();
      NCdumpW.printArray(data, "read()", new PrintWriter(System.out), null);

      int count = 0;
      while (data.hasNext()) {
        Array as = (Array) data.next();
        NCdumpW.printArray(as, " " + count, new PrintWriter(System.out), null);
        count++;
      }

      // try subset
      data = v.read("0:9:2, :");
      NCdumpW.printArray(data, "read(0:9:2,:)", new PrintWriter(System.out), null);

      data = v.read(new Section().appendRange(0, 9, 2).appendRange(null));
      NCdumpW.printArray(data, "read(Section)", new PrintWriter(System.out), null);

      // fail
      //int[] origin = new int[] {0, 0};
      //int[] size = new int[] {3, -1};
      //data = v.read(origin, size);

      // from bruno
      int initialIndex = 5;
      int finalIndex = 5;
      data = v.read(initialIndex + ":" + finalIndex + ",:");
      //NCdumpW.printArray(data, "read()",  new PrintWriter(System.out), null);

      System.out.println("Size: " + data.getSize());
      System.out.println("Data: " + data);
      System.out.println("Class: " + data.getClass().getName());
      // loop over outer dimension

      int x = 0;
      while (data.hasNext()) {
        Array as = (Array) data.next(); // inner variable length array of short
        System.out.println("Shape: " + new Section(as.getShape()));
        System.out.println(as);
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

      System.out.println(NCdumpW.toString(data, "", null));

    }
    System.out.println("*** testNestedStructure ok");
  }

  @Test
  public void testStrings() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"files/nc_test_netcdf4.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      Variable v = ncfile.findVariable("d");
      String attValue = ncfile.findAttValueIgnoreCase(v, "c", null);
      String s = Misc.showBytes(attValue.getBytes(CDM.utf8Charset));
      System.out.println(" d:c= (" + attValue + ") = " + s);
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
      System.out.printf("%s%n", ncfile);
    }
  }

  @Test
  public void testCompoundVlens() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"vlen/cdm_sea_soundings.nc4";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      Variable v = ncfile.findVariable("fun_soundings");
      Array data = v.read();
      NCdumpW.printArray(data, "fun_soundings", new PrintWriter(System.out), null);

      assert data instanceof ArrayStructure;
      ArrayStructure as = (ArrayStructure) data;
      int index = 2;
      String member = "temp_vl";
      StructureData sdata = as.getStructureData(2);
      Array vdata = sdata.getArray("temp_vl");
      assert vdata instanceof ArrayFloat;
      System.out.printf("the %d record has %d elements for vlen member %s%n%n", index, vdata.getSize(), member);
      assert vdata.getSize() == 3;
      Index ii = vdata.getIndex();
      assert Misc.closeEnough(vdata.getFloat(ii.set(2)), 21.5);

      String memberName = "temp_vl";
      int count = 0;
      Structure s = (Structure) v;
      StructureDataIterator siter = s.getStructureIterator();
      siter.reset();
      while (siter.hasNext()) {
        StructureData sdata2 = siter.next();
        Array vdata2 = sdata2.getArray(memberName);
        System.out.printf("iter %d  has %d elements for vlen member %s%n", count++, vdata2.getSize(), memberName);
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
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      Variable v = ncfile.findVariable("tim_records");
      int[] vshape = v.getShape();
      Array data = v.read();
      NCdumpW.printArray(data, v.getFullName(), new PrintWriter(System.out), null);

      assert data instanceof ArrayStructure;
      ArrayStructure as = (ArrayStructure) data;
      assert as.getSize() == vshape[0];  //   int loopDataA(1, *, *);
      StructureData sdata = as.getStructureData(0);
      Array vdata = sdata.getArray("loopDataA");
      NCdumpW.printArray(vdata, "loopDataA", new PrintWriter(System.out), null);

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
