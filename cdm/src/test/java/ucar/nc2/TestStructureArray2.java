package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.*;
import java.util.*;

/** Test reading record data */

public class TestStructureArray2 extends TestCase {
  ucar.ma2.TestStructureArray test;

  public TestStructureArray2( String name) {
    super(name);
    test = new ucar.ma2.TestStructureArray();
  }

  public void testBB() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestNC2.openFile("testWriteRecord.nc");
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
    NetcdfFile ncfile = TestNC2.openFile("jan.nc");
    Dimension dim = ncfile.findDimension("time");
    assert dim != null;

    Structure p = new StructurePseudo( ncfile, null, "Psuedo", dim);

    assert( p.getDataType() == DataType.STRUCTURE);

    Array data = p.read();
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureMA);
    assert(data.getElementType() == StructureData.class);

    test.testArrayStructure( (ArrayStructure) data);

    ncfile.close();
  }

  public void utestBufr() throws IOException, InvalidRangeException {
    //String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    String fileIn = TestAll.upcShareTestDataDir+"bufr/edition3/ecmwf/synop.bufr";
    NetcdfFile ncf = NetcdfFile.open(fileIn);
    System.out.println(ncf.toString());

    Structure s = (Structure) ncf.findVariable("obsRecord");
    Array data = s.read();
    test.testArrayStructure( (ArrayStructure) data);

    Array data2 = s.read(new Section().appendRange(1,3));
    assert data2.getSize() == 3;
    test.testArrayStructure( (ArrayStructure) data2);
    System.out.println( NCdumpW.printArray(data2, "testArrayStructure", null));

    // test nested
  }

  public void utestBufrEnhanced() throws IOException, InvalidRangeException {
    //String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    String fileIn = TestAll.upcShareTestDataDir+"bufr/edition3/ecmwf/synop.bufr";
    NetcdfDataset ncf = NetcdfDataset.openDataset(fileIn);
    System.out.println(ncf.toString());

    Structure s = (Structure) ncf.findVariable("obsRecord");
    Array data = s.read();
    test.testArrayStructure( (ArrayStructure) data);

    Array data2 = s.read(new Section().appendRange(1,3));
    assert data2.getSize() == 3;
    test.testArrayStructure( (ArrayStructure) data2);
    System.out.println( NCdumpW.printArray(data2, "testArrayStructure", null));

    // test nested 
  }

}
