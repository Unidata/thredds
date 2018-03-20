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

/** Test nc2 dods in the JUnit framework.
 * Dataset {
    Grid {
     ARRAY:
        Float64 amp[10];
     MAPS:
        Float64 x[10];
    } OneD;
} Simple;*/
public class TestDODSGrid {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @org.junit.Test
  public void testGrid() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.06a");

    Variable v = null;

    // arrays
    assert(null != (v = dodsfile.findVariable("OneD")));
    assert v instanceof DODSVariable;
    checkArray(v);

    // map
    assert(null != (v = dodsfile.findVariable("x")));
    checkArray(v);
  }

  void checkArray( Variable v) throws IOException {
    assert v.getRank() == 1;
    assert v.getSize() == 10;
    assert v.getDataType() == DataType.DOUBLE;
    Array a = v.read();
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

}
