package ucar.nc2;

import java.io.IOException;

import junit.framework.TestCase;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

public class TestRedefine extends TestCase {

  public TestRedefine( String name) {
    super(name);
  }

  String filename = TestLocal.cdmTestDataDir + "testRedefine.nc";
  String filename2 = TestLocal.cdmTestDataDir + "testRedefine2.nc";

  public void testRedefine() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.createNew(filename, true);

    file.addGlobalAttribute("Conventions", "globulate");
    file.addGlobalAttribute("history", "lava");
    file.addGlobalAttribute("att8", "12345678");

    Dimension time = file.addDimension("time", 4, true, false, false);
    Dimension dims[] = {time};

    /* Add time */
    file.addVariable("time", DataType.DOUBLE, dims);
    file.addVariableAttribute("time", "quantity", "time");
    file.addVariableAttribute("time", "units", "s");

    /* Add a dependent variable */
    file.addVariable("h", DataType.DOUBLE, dims);
    file.addVariableAttribute("h", "quantity", "Height");
    file.addVariableAttribute("h", "units", "m");
    try {
      file.create();
    } catch (IOException e) {
      e.printStackTrace(System.err);
      fail("IOException on creation");
    }

    double td[] = {1.0, 2.0, 3.0, 4.0};
    double hd[] = {0.0, 0.1, 0.3, 0.9};
    ArrayDouble.D1 ta = new ArrayDouble.D1(4);
    ArrayDouble.D1 ha = new ArrayDouble.D1(4);
    for (int i = 0; i < 4; i++) {
      ta.set(i, td[i]);
      ha.set(i, hd[i]);
    }

    file.write("time", ta);
    file.write("h", ha);

    //////////////////////////////////////////
    file.setRedefineMode(true);

    file.renameGlobalAttribute("history", "lamp");
    file.addGlobalAttribute("history", "final");
    file.deleteGlobalAttribute("Conventions");

    file.addVariableAttribute("h", "units", "meters"); // duplicates existing
    file.addVariableAttribute("h", "new", "stuff");
    file.renameVariableAttribute("time", "quantity", "quality");

    file.renameVariable("time", "date");
    file.renameDimension("time", "date");

    /////////////////////////////////////////////////
    file.setRedefineMode(false);

    Attribute att = file.findGlobalAttribute("Conventions");
    assert att == null;
    att = file.findGlobalAttribute("history");
    assert att.getStringValue().equals("final");
    att = file.findGlobalAttribute("lamp");
    assert att.getStringValue().equals("lava");
    
    Variable v = file.findVariable("h");
    att = v.findAttribute("units");
    assert att != null;
    assert att.getStringValue().equals("meters");

    assert file.findVariable("time") == null;
    v = file.findVariable("date");
    assert v != null;
    assert v.getRank() == 1;
    assert null != v.findAttribute("quality");
    
    Dimension d = v.getDimension(0);
    assert d.getName().equals("date");

    assert file.findDimension("time") == null;
    Dimension dim = file.findDimension("date");
    assert dim != null;
    assert dim.getName().equals("date");
    assert dim.equals(d);
    assert dim == d;

    file.close();
  }

  public void testRewriteHeader() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.openExisting(filename, true);
    file.setRedefineMode(true);

    file.addGlobalAttribute("att8", "1234567");

    /////////////////////////////////////////////////
    boolean rewriteAll = file.setRedefineMode(false);
    assert !rewriteAll;

    Attribute att = file.findGlobalAttribute("att8");
    assert att != null;
    assert att.getStringValue().equals("1234567") : att.getStringValue();

    file.close();
  }

  public void testRewriteHeader2() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.openExisting(filename, true);
    file.setRedefineMode(true);

    file.addGlobalAttribute("att8", "123456789");

    /////////////////////////////////////////////////
    boolean rewriteAll = file.setRedefineMode(false);
    assert rewriteAll;


    Attribute att = file.findGlobalAttribute("att8");
    assert att != null;
    assert att.getStringValue().equals("123456789") : att.getStringValue();

    file.close();
  }

  public void testRewriteHeader3() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file;
    file = NetcdfFileWriteable.createNew(filename2, true);
    file.addGlobalAttribute("att8", "1234567890");
    file.setExtraHeaderBytes(10);
    file.create();

    file.setRedefineMode(true);
    file.addGlobalAttribute("att8", "123456789012345");
    boolean rewriteAll = file.setRedefineMode(false);
    assert !rewriteAll;

    Attribute att = file.findGlobalAttribute("att8");
    assert att != null;
    assert att.getStringValue().equals("123456789012345") : att.getStringValue();

    file.close();
  }
}
