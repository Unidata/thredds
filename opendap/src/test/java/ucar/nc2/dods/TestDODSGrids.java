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
package ucar.nc2.dods;

import java.io.IOException;

import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

import static org.junit.Assert.assertEquals;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsExternalResource.class)
public class TestDODSGrids {

  @org.junit.Test
  public void testGrid() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.06");

    Variable v = null;

    assert(null != (v = dodsfile.findVariable("OneD")));
    checkVariable(v);
    assert(null != (v = dodsfile.findVariable("x")));
    checkVariable(v);
    assert(null != (v = dodsfile.findVariable("y")));
    checkVariable(v);

    assert(null != (v = dodsfile.findVariable("FourD")));
    assert v instanceof DODSGrid;
    checkVariable2(v);
  }

  void checkVariable( Variable v) throws IOException {
    assert v.getRank() == 1;
    assert v.getSize() == 10;
    assert v.getDataType() == DataType.DOUBLE;
    Array a = v.read();

    checkArray( a);
  }

  void checkArray( Array a) throws IOException {
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D1;
    double[] tFloat64 = new double[] {1.0, 0.9999500004166653, 0.9998000066665778,
      0.9995500337489875, 0.9992001066609779, 0.9987502603949663, 0.9982005399352042,
      0.9975510002532796, 0.9968017063026194, 0.9959527330119943};

    IndexIterator iter = a.getIndexIterator();
    int count = 0;
    while (iter.hasNext()) {
      assertEquals(iter.getDoubleNext(), tFloat64[count], 1.0e-9);
      count++;
    }
  }

  void checkVariable2( Variable v) throws IOException {
    assert v.getRank() == 4;
    assert v.getSize() == 10000;
    assert v.getDataType() == DataType.DOUBLE;
    Array a = v.read();
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D4;
    ArrayDouble.D4 a4 = (ArrayDouble.D4) a;

    assertEquals( a4.get(0,0,0,0), 1.0 ,1.0e-9);
    assertEquals( a4.get(0,1,3,0), 0.26749882862458735 ,1.0e-9);
    assertEquals( a4.get(1,1,1,1), 0.1141761752318889 ,1.0e-9);
    assertEquals( a4.get(9, 6, 4, 2), -0.5658172324454206 ,1.0e-9);
  }

}
