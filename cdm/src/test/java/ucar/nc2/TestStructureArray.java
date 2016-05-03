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
import ucar.ma2.*;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.*;

/** Test reading record data */

public class TestStructureArray extends TestCase {

  public TestStructureArray( String name) {
    super(name);
  }

  NetcdfFile ncfile;
  protected void setUp() throws Exception {
    ncfile = TestDir.openFileLocal("testStructures.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }
  protected void tearDown() throws Exception {
    ncfile.close();
  }

  public void testNames() {

    List vars = ncfile.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      System.out.println(" "+v.getShortName()+" == "+v.getFullName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    vars = record.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      assert ("record."+v.getShortName()).equals(v.getFullName());
    }
  }

  public void testReadTop() throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("record");
    assert v != null;

    assert( v.getDataType() == DataType.STRUCTURE);
    assert( v instanceof Structure);
    assert( v.getRank() == 1);
    assert( v.getSize() == 1000);

    Array data = v.read(new int[] {4}, new int[] {3});
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureBB);

    assert(data.getElementType() == StructureData.class);
    assert (data.getSize() == 3) : data.getSize();
    assert (data.getRank() == 1);
  }

  /* public void testReadNested() throws IOException, InvalidRangeException {

    Structure v = (Structure) ncfile.findVariable("record");
    assert v != null;

    Variable lat = v.findVariable("lat");
    assert null != lat;

    assert( lat.getDataType() == DataType.DOUBLE);
    assert( lat.getRank() == 0);
    assert( lat.getSize() == 1);

    Array data = lat.readAllStructuresSpec("(4:6,:)", false);
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureMA);

    assert(data.getElementType() == StructureData.class);
    assert (data.getSize() == 3) : data.getSize();
    assert (data.getRank() == 1);

    Array data2 = lat.readAllStructuresSpec("(4:6,:)", true);
    assert( data2 instanceof ArrayDouble);
    assert( data2 instanceof ArrayDouble.D1);

    assert(data2.getElementType() == double.class);
    assert (data2.getSize() == 3) : data.getSize();
    assert (data2.getRank() == 1);
  } */
}
