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
package ucar.unidata.util.test;

import ucar.ma2.*;

import java.util.List;
import java.io.IOException;

/**
 * @author caron
 * @since Apr 14, 2008
 */
public class UtilsTestStructureArray {

  public void testArrayStructure(ArrayStructure as) {
    try {
      testArrayStructureByRecno(as);
      testArrayStructureByIterator(as);
      testArrayStructureByMemberArray(as);

    } catch (IOException e) {
      assert false;
    }
  }

  private double testArrayStructureByMemberArray(ArrayStructure as) throws IOException {
    List<StructureMembers.Member> members = as.getMembers();

    for (StructureMembers.Member m : members) {
      Array memberArray = as.extractMemberArray(m);
      assert (memberArray.getElementType() == m.getDataType().getPrimitiveClassType());

      // random tests
      if (m.getDataType().isNumeric()) {
        double sum = 0.0;
        while (memberArray.hasNext())
          sum += memberArray.nextDouble();
        return sum;
      } else if (m.getDataType().isString()) {
        while (memberArray.hasNext())
          System.out.println(" " + memberArray.next());
      }
    }
    return 0.0;
  }

  private void testArrayStructureByIterator(ArrayStructure as) throws IOException {
    StructureDataIterator si = as.getStructureDataIterator();
    try {
      while (si.hasNext()) {
        StructureData sdata = si.next();

        // run through each member on the StructureData
        List<StructureMembers.Member> members = sdata.getMembers();
        for (StructureMembers.Member m : members) {
          Array sdataArray = sdata.getArray(m);
          assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());
        }

        testStructureData(sdata);
      }
    } finally {
      si.finish();
    }
  }

  private void testArrayStructureByRecno(ArrayStructure as) {
    // run through each StructureData
    for (int recno = 0; recno < as.getSize(); recno++) {
      Object o = as.getObject(recno);
      assert (o instanceof StructureData);
      StructureData sdata = as.getStructureData(recno);
      assert (o == sdata);

      // run through each member on the StructureData
      List<StructureMembers.Member> members = sdata.getMembers();
      for (StructureMembers.Member m : members) {
        Array sdataArray = sdata.getArray(m);
        assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType()) :
                sdataArray.getElementType() +" != "+m.getDataType().getPrimitiveClassType();

        Array sdataArray2 = sdata.getArray(m.getName());
        UtilsMa2Test.testEquals(sdataArray, sdataArray2);

        Array a = as.getArray(recno, m);
        assert (a.getElementType() == m.getDataType().getPrimitiveClassType());
        UtilsMa2Test.testEquals(sdataArray, a);

        testGetArrayByType(as, recno, m, a);
      }

      testStructureData(sdata);
    }
  }

  private void testGetArrayByType(ArrayStructure as, int recno, StructureMembers.Member m, Array a) {
    DataType dtype = m.getDataType();
    Object data = null;
    if (dtype == DataType.DOUBLE) {
      assert a.getElementType() == double.class;
      data = as.getJavaArrayDouble(recno, m);

    } else if (dtype == DataType.FLOAT) {
      assert a.getElementType() == float.class;
      data = as.getJavaArrayFloat(recno, m);

    } else if (dtype == DataType.LONG) {
      assert a.getElementType() == long.class;
      data = as.getJavaArrayLong(recno, m);

    } else if (dtype == DataType.INT) {
      assert a.getElementType() == int.class;
      data = as.getJavaArrayInt(recno, m);

    } else if (dtype == DataType.SHORT) {
      assert a.getElementType() == short.class;
      data = as.getJavaArrayShort(recno, m);

    } else if (dtype == DataType.BYTE) {
      assert a.getElementType() == byte.class;
      data = as.getJavaArrayByte(recno, m);

    } else if (dtype == DataType.CHAR) {
      assert a.getElementType() == char.class;
      data = as.getJavaArrayChar(recno, m);

    } else if (dtype == DataType.STRING) {
      assert a.getElementType() == String.class;
      data = as.getJavaArrayString(recno, m);

    } else if (dtype == DataType.STRUCTURE) {
      assert a.getElementType() == StructureData.class;
      ArrayStructure nested = as.getArrayStructure(recno, m);
      testArrayStructure(nested);
    }

    if (data != null)
      UtilsMa2Test.testJarrayEquals(data, a.getStorage(), m.getSize());
  }

  private void testStructureData(StructureData sdata) {

    List<StructureMembers.Member> members = sdata.getMembers();
    for (StructureMembers.Member m : members) {
      Array sdataArray = sdata.getArray(m);
      assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

      Array sdataArray2 = sdata.getArray(m.getName());
      UtilsMa2Test.testEquals(sdataArray, sdataArray2);

      //NCdump.printArray(sdataArray, m.getName(), System.out, null);

      testGetArrayByType(sdata, m, sdataArray);
    }
  }

  private void testGetArrayByType(StructureData sdata, StructureMembers.Member m, Array a) {
    DataType dtype = m.getDataType();
    Object data = null;
    if (dtype == DataType.DOUBLE) {
      assert a.getElementType() == double.class;
      data = sdata.getJavaArrayDouble(m);

    } else if (dtype == DataType.FLOAT) {
      assert a.getElementType() == float.class;
      data = sdata.getJavaArrayFloat(m);

    } else if (dtype == DataType.LONG) {
      assert a.getElementType() == long.class;
      data = sdata.getJavaArrayLong(m);

    } else if (dtype == DataType.INT) {
      assert a.getElementType() == int.class;
      data = sdata.getJavaArrayInt(m);

    } else if (dtype == DataType.SHORT) {
      assert a.getElementType() == short.class;
      data = sdata.getJavaArrayShort(m);

    } else if (dtype == DataType.BYTE) {
      assert a.getElementType() == byte.class;
      data = sdata.getJavaArrayByte(m);

    } else if (dtype == DataType.CHAR) {
      assert a.getElementType() == char.class;
      data = sdata.getJavaArrayChar(m);

    } else if (dtype == DataType.STRING) {
      assert a.getElementType() == String.class;
      data = sdata.getJavaArrayString(m);

    } else if (dtype == DataType.STRUCTURE) {
      assert a.getElementType() == StructureData.class;
      ArrayStructure nested = sdata.getArrayStructure(m);
      testArrayStructure(nested);
    }

    if (data != null)
      UtilsMa2Test.testJarrayEquals(data, a.getStorage(), m.getSize());
  }

}
