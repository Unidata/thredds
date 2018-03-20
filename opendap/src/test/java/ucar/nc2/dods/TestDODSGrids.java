/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Variable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;

/** Test nc2 dods in the JUnit framework. */
public class TestDODSGrids {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
