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

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.NCdump;

import java.io.*;

/** Test nc2 read JUnit framework. */

public class TestH5ReadStructure extends TestCase {

  public TestH5ReadStructure( String name) {
    super(name);
  }

  File tempFile;
  PrintStream out;
  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintStream( new FileOutputStream( tempFile));
  }
  protected void tearDown() throws Exception {
    out.close();
    tempFile.delete();
  }
  
  /*Structure {
     char a_string(10);
     char b_string(13);
   } Compound String(10);
   */
  public void testStructureArray() throws IOException {
    NetcdfFile ncfile = TestH5.openH5("support/cstr.h5");

    Variable v = ncfile.findVariable("Compound String");
    assert( null != v);
    assert( v.getDataType() == DataType.STRUCTURE);
    assert( v instanceof Structure);
    assert( v.getRank() == 1);
    assert( v.getShape()[0] == 10);

      Array data = v.read();
      assert(data.getElementType() == StructureData.class);
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

      ncfile.close();
  } // */

  /*
   Structure {
     int a_name;
     double c_name;
     float b_name;
   } ArrayOfStructures(30);
   type = Layout(8);  type= 2 (chunked) storageSize = (3,16) dataSize=0 dataAddress=1576
   */
  public void testStructureArrayChunked() throws IOException {
    NetcdfFile ncfile = TestH5.openH5("support/cuslab.h5");

    Variable v = ncfile.findVariable("ArrayOfStructures");
    assert( null != v);
    assert( v.getDataType() == DataType.STRUCTURE);
    assert( v instanceof Structure);
    assert( v.getRank() == 1);
    assert( v.getShape()[0] == 30);

    Array data = null;
    try {
      data = v.read();
    } catch (IOException e) { assert false; } // */

    assert(data.getElementType() == StructureData.class);
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
      NCdump.printArray( arr, "a_name", out, null);

      arr = d.getArray("b_name");
      assert (arr != null);
      assert (arr.getElementType() == float.class);
      assert (arr instanceof ArrayFloat);
      NCdump.printArray( arr, "b_name", out, null);

      arr = d.getArray("c_name");
      assert (arr != null);
      assert (arr.getElementType() == double.class);
      assert (arr instanceof ArrayDouble);
      NCdump.printArray( arr, "c_name", out, null);
    }

    // this tests that we are using the btree ok
    Index ima = data.getIndex();
    StructureData sd = (StructureData) data.getObject( ima.set0(29));
    assert (sd.getScalarDouble("c_name") == 9.0) : sd.getScalarDouble("c_name");

    ncfile.close();
  } // */

  /*
    Structure {
     int a_name;
     float b_name(3);
   } ArrayOfStructures(10);
    type = Layout(8);  type= 1 (contiguous) storageSize = (10,16) dataSize=0 dataAddress=2048
   */
   public void testStructureWithArrayMember() throws IOException {
    NetcdfFile ncfile = TestH5.openH5("support/DSwith_array_member.h5");

    Variable v = ncfile.findVariable("ArrayOfStructures");
    v.setCaching(false);
    assert( null != v);
    assert( v.getDataType() == DataType.STRUCTURE);
    assert( v instanceof Structure);
    assert( v.getRank() == 1);
    assert( v.getShape()[0] == 10);

    try {
      Array data = v.read(new int[] {4}, new int[] {3});
      assert(data.getElementType() == StructureData.class);
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
        assert (arr.getInt( arr.getIndex()) == 4 + count);
        NCdump.printArray( arr, "a_name", out, null);

        arr = d.getArray("b_name");
        assert (arr != null);
        assert (arr.getElementType() == float.class);
        assert (arr instanceof ArrayFloat);
        assert (arr.getSize() == 3);
        assert (arr.getFloat( arr.getIndex()) == (float) 4.0 + count);
        NCdump.printArray( arr, "b_name", out, null);

        count++;
      }

    }
    catch (InvalidRangeException e) { assert false; }
    catch (IOException e) { assert false; }

    try {
      Array data = v.read();
      assert(data.getElementType() == StructureData.class);
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
        assert (arr.getInt( arr.getIndex()) == count);
        NCdump.printArray( arr, "a_name", out, null);

        arr = d.getArray("b_name");
        assert (arr != null);
        assert (arr.getElementType() == float.class);
        assert (arr instanceof ArrayFloat);
        assert (arr.getSize() == 3);
        assert (arr.getFloat( arr.getIndex()) == (float) count);
        NCdump.printArray( arr, "b_name", out, null);

        count++;
      }

    } catch (IOException e) { assert false; }

    ncfile.close();
  } // */
}
