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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.CompareNetcdf;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;

/**
 * Test nc2 read JUnit framework.
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadStructure {
  File tempFile;
  PrintWriter out;

  @Before
  public void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintWriter(new FileOutputStream(tempFile));
  }

  @After
  public void tearDown() throws Exception {
    out.close();
    if (!tempFile.delete())
      System.out.printf("Delete failed on %s%n", tempFile);
  }

  /*Structure {
     char a_string(10);
     char b_string(13);
   } Compound String(10);
   */
  @Test
  public void testStructureArray() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/cstr.h5")) {

      Variable v = ncfile.findVariable("Compound_String");
      assert (null != v);
      assert (v.getDataType() == DataType.STRUCTURE);
      assert (v instanceof Structure);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 10);

      Array data = v.read();
      assert (data.getElementType() == StructureData.class);
      assert (data instanceof ArrayStructure);
      assert (data.getSize() == 10);
      assert (data.getRank() == 1);

      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
        Object o = iter.next();
        assert (o instanceof StructureData);
        StructureData d = (StructureData) o;
        Array arr = d.getArray("a_string");
        assert (arr != null);
        assert (arr.getElementType() == char.class);
        assert (arr instanceof ArrayChar);
        ArrayChar arrc = (ArrayChar) arr;
        out.println(arrc.getString());
        assert arrc.getString().equals("Astronomy") : arrc.getString();

        arr = d.getArray("b_string");
        assert (arr != null);
        assert (arr.getElementType() == char.class);
        assert (arr instanceof ArrayChar);
        arrc = (ArrayChar) arr;
        out.println(arrc.getString());
        assert arrc.getString().equals("Biochemistry") : arrc.getString();
      }

    }
  }

  /*
   Structure {
     int a_name;
     double c_name;
     float b_name;
   } ArrayOfStructures(30);
   type = Layout(8);  type= 2 (chunked) storageSize = (3,16) dataSize=0 dataAddress=1576
   */
  @Test
  public void testStructureArrayChunked() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/cuslab.h5")) {

      Variable v = ncfile.findVariable("ArrayOfStructures");
      assert (null != v);
      assert (v.getDataType() == DataType.STRUCTURE);
      assert (v instanceof Structure);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 30);

      Array data = null;
      try {
        data = v.read();
      } catch (IOException e) {
        assert false;
      } // */

      assert (data.getElementType() == StructureData.class);
      assert (data instanceof ArrayStructure);
      assert (data.getSize() == 30);
      assert (data.getRank() == 1);

      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
        Object o = iter.next();
        assert (o instanceof StructureData) : o;
        StructureData d = (StructureData) o;
        Array arr = d.getArray("a_name");
        assert (arr != null);
        assert (arr.getElementType() == int.class);
        assert (arr instanceof ArrayInt);
        NCdumpW.printArray(arr, "a_name", out, null);

        arr = d.getArray("b_name");
        assert (arr != null);
        assert (arr.getElementType() == float.class);
        assert (arr instanceof ArrayFloat);
        NCdumpW.printArray(arr, "b_name", out, null);

        arr = d.getArray("c_name");
        assert (arr != null);
        assert (arr.getElementType() == double.class);
        assert (arr instanceof ArrayDouble);
        NCdumpW.printArray(arr, "c_name", out, null);
      }

      // this tests that we are using the btree ok
      Index ima = data.getIndex();
      StructureData sd = (StructureData) data.getObject(ima.set0(29));
      assert (sd.getScalarDouble("c_name") == 9.0) : sd.getScalarDouble("c_name");

    }
  }

  /*
    Structure {
     int a_name;
     float b_name(3);
   } ArrayOfStructures(10);
    type = Layout(8);  type= 1 (contiguous) storageSize = (10,16) dataSize=0 dataAddress=2048
   */
  @Test
  public void testStructureWithArrayMember() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/DSwith_array_member.h5")) {

      Variable v = ncfile.findVariable("ArrayOfStructures");
      v.setCaching(false);
      assert (null != v);
      assert (v.getDataType() == DataType.STRUCTURE);
      assert (v instanceof Structure);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 10);

      try {
        Array data = v.read(new int[]{4}, new int[]{3});
        assert (data.getElementType() == StructureData.class);
        assert (data instanceof ArrayStructure);
        assert (data.getSize() == 3) : data.getSize();
        assert (data.getRank() == 1);

        int count = 0;
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          Object o = iter.next();
          assert (o instanceof StructureData);
          StructureData d = (StructureData) o;

          Array arr = d.getArray("a_name");
          assert (arr != null);
          assert (arr.getElementType() == int.class);
          assert (arr instanceof ArrayInt);
          assert (arr.getInt(arr.getIndex()) == 4 + count);
          NCdumpW.printArray(arr, "a_name", out, null);

          arr = d.getArray("b_name");
          assert (arr != null);
          assert (arr.getElementType() == float.class);
          assert (arr instanceof ArrayFloat);
          assert (arr.getSize() == 3);
          assert (arr.getFloat(arr.getIndex()) == (float) 4.0 + count);
          NCdumpW.printArray(arr, "b_name", out, null);

          count++;
        }

      } catch (InvalidRangeException | IOException e) {
        assert false;
      }

      try {
        Array data = v.read();
        assert (data.getElementType() == StructureData.class);
        assert (data instanceof ArrayStructure);
        assert (data.getSize() == 10);
        assert (data.getRank() == 1);

        int count = 0;
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          Object o = iter.next();
          assert (o instanceof StructureData);
          StructureData d = (StructureData) o;

          Array arr = d.getArray("a_name");
          assert (arr != null);
          assert (arr.getElementType() == int.class);
          assert (arr instanceof ArrayInt);
          assert (arr.getInt(arr.getIndex()) == count);
          NCdumpW.printArray(arr, "a_name", out, null);

          arr = d.getArray("b_name");
          assert (arr != null);
          assert (arr.getElementType() == float.class);
          assert (arr instanceof ArrayFloat);
          assert (arr.getSize() == 3);
          assert (arr.getFloat(arr.getIndex()) == (float) count);
          NCdumpW.printArray(arr, "b_name", out, null);

          count++;
        }

      } catch (IOException e) {
        assert false;
      }

    }
  }

  @Test
  public void testMemberVariable() throws java.io.IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestH5.openH5("20130212_CN021_P3_222k_B02_WD7195FBPAT10231Nat_Nat_Std_CHTNWD_OP3_14.mip222k.oschp")) {

      Variable v = ncfile.findVariable("/Chromosomes/Summary.StartIndex");
      System.out.printf("%s%n", v);

//Section section=new Section(new int[]{2}, new int[]{5});
      Array a1 = v.read(); // section); // different from int[] a1 and different from Arrays a1
      System.out.printf("size = %d%n", a1.getSize());
      System.out.printf("%s%n", a1);

      Index ii = a1.getIndex();
      assert a1.getInt(ii.set(3)) == 52203 : a1.getInt(ii.set(3));

      Array a2 = ncfile.readSection("/Chromosomes/Summary.StartIndex");
      CompareNetcdf.compareData(a1, a2);
      System.out.printf("size = %d%n", a2.getSize());
      System.out.printf("%s%n", a2);

      Array a3 = ncfile.readSection("/Chromosomes/Summary(12:20).StartIndex");
      System.out.printf("size = %d%n", a3.getSize());
      assert (a3.getSize() == 9);
      System.out.printf("%s%n", a3);

      Array a4 = a1.section(new int[]{12}, new int[]{9});
      System.out.printf("a3 = %s%n", a3);
      System.out.printf("a4 = %s%n", a4);

      // the two sections should not be the same object
      assert (!a3.equals(a4));

      // the two sections should be the same length
      assert (a3.getSize() == a4.getSize());

      // each value in each section should be equal
      while (a3.hasNext() && a4.hasNext()) {
        assert (a3.next().equals(a4.next()));
      }

      // this low-level test fails
      //for (int i = 0; i < a3.getSize(); i++) {
      //   System.out.printf("%s%n %s%n", a3.getLong(i), a4.getLong(i));
      //   assert (a3.getLong(i) == a4.getLong(i));
      //}

      //assert (Arrays.equals(a3, a4));
    }
  }
}
