/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggDirDateFormat {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static GridDataset gds;
  private final int ntimes = 8;

  @BeforeClass
  public static void setupClass() throws IOException {
    String filename = "file:"+ TestDir.cdmUnitTestDir + "formats/gini/aggDateFormat.ncml";
    gds = ucar.nc2.dt.grid.GridDataset.open( filename);
  }

  @AfterClass
  public static void teardownClass() throws IOException {
    if (gds != null) {  // Could be null if setupClass() throws an exception.
      gds.close();
    }
  }

  @Test
  public void testDimensions() {
    Dimension latDim = gds.getNetcdfFile().findDimension("y");
    assert null != latDim;
    assert latDim.getShortName().equals("y");
    assert latDim.getLength() == 1008 : latDim.getLength() +"!="+ 1008;
    assert !latDim.isUnlimited();

    Dimension lonDim = gds.getNetcdfFile().findDimension("x");
    assert null != lonDim;
    assert lonDim.getShortName().equals("x");
    assert lonDim.getLength() == 1536;
    assert !lonDim.isUnlimited();

    Dimension timeDim = gds.getNetcdfFile().findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == ntimes : timeDim.getLength() +"!="+ ntimes;
  }

  @Test
  public void testAggCoordVar() throws IOException {
    Variable time = gds.getNetcdfFile().findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == ntimes;
    assert time.getShape()[0] == ntimes;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == gds.getNetcdfFile().findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == ntimes;
    assert data.getShape()[0] == ntimes;
    assert data.getElementType() == double.class;

    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      logger.debug("coord = {}", dataI.getObjectNext());
  }

  @Test
  public void testReadData() throws IOException {
    GridDatatype g = gds.findGridDatatype("IR_WV");
    assert null != g;
    assert g.getFullName().equals("IR_WV");
    assert g.getRank() == 3;
    assert g.getShape()[0] == ntimes;
    assert g.getShape()[1] == 1008;
    assert g.getShape()[2] == 1536;
    assert g.getDataType() == DataType.USHORT : g.getDataType();

    GridCoordSystem gsys = g.getCoordinateSystem();
    assert gsys.getXHorizAxis() != null;
    assert gsys.getYHorizAxis() != null;
    assert gsys.getTimeAxis() != null;
    assert gsys.getVerticalAxis() == null;
    assert gsys.getProjection() != null;

    Array data = g.readVolumeData(0);
    assert data.getRank() == 2;
    assert data.getShape()[0] == 1008;
    assert data.getShape()[1] == 1536;
    assert data.getElementType() == short.class;
  }
}
