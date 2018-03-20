/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UtilsTestStructureArray;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test reading record data */

public class TestStructureArray2 extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  UtilsTestStructureArray test;

  public TestStructureArray2( String name) {
    super(name);
    test = new UtilsTestStructureArray();
  }

  public void testBB() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWriteRecord.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    Structure v = (Structure) ncfile.findVariable("record");
    assert v != null;

    assert( v.getDataType() == DataType.STRUCTURE);

    Array data = v.read();
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureBB);
    assert(data.getElementType() == StructureData.class);

    test.testArrayStructure( (ArrayStructure) data);

    ncfile.close();
  }

  public void testMA() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestDir.openFileLocal("jan.nc");
    NetcdfDataset ncd = new NetcdfDataset( ncfile);
    Dimension dim = ncd.findDimension("time");
    assert dim != null;

    Structure p = new ucar.nc2.dataset.StructurePseudoDS( ncd, null, "Psuedo", null, dim);

    assert( p.getDataType() == DataType.STRUCTURE);

    Array data = p.read();
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureMA);
    assert(data.getElementType() == StructureData.class);

    test.testArrayStructure( (ArrayStructure) data);

    ncfile.close();
  }

}
