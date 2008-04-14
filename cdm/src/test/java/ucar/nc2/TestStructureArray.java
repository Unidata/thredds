package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;

import java.io.*;
import java.util.*;

/** Test reading record data */

public class TestStructureArray extends TestCase {

  public TestStructureArray( String name) {
    super(name);
  }

  NetcdfFile ncfile;
  protected void setUp() throws Exception {
    ncfile = TestLocalNC2.open(TestLocal.cdmTestDataDir +"testStructures.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }
  protected void tearDown() throws Exception {
    ncfile.close();
  }

  public void testNames() {

    List vars = ncfile.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      System.out.println(" "+v.getShortName()+" == "+v.getName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    vars = record.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      assert ("record."+v.getShortName()).equals(v.getName());
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
