/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestRedefine {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testRedefine() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
      ncWriter.addGlobalAttribute("Conventions", "globulate");
      ncWriter.addGlobalAttribute(CDM.HISTORY, "lava");
      ncWriter.addGlobalAttribute("att8", "12345678");

      ncWriter.addDimension(null, "time", 4, false, false);

            /* Add time */
      ncWriter.addVariable("time", DataType.DOUBLE, "time");
      ncWriter.addVariableAttribute("time", "quantity", "time");
      ncWriter.addVariableAttribute("time", "units", "s");

            /* Add a dependent variable */
      ncWriter.addVariable("h", DataType.DOUBLE, "time");
      ncWriter.addVariableAttribute("h", "quantity", "Height");
      ncWriter.addVariableAttribute("h", "units", "m");
      ncWriter.create();

      double td[] = {1.0, 2.0, 3.0, 4.0};
      double hd[] = {0.0, 0.1, 0.3, 0.9};
      ArrayDouble.D1 ta = new ArrayDouble.D1(4);
      ArrayDouble.D1 ha = new ArrayDouble.D1(4);
      for (int i = 0; i < 4; i++) {
        ta.set(i, td[i]);
        ha.set(i, hd[i]);
      }

      ncWriter.write("time", ta);
      ncWriter.write("h", ha);

      //////////////////////////////////////////
      ncWriter.setRedefineMode(true);

      ncWriter.renameGlobalAttribute(CDM.HISTORY, "lamp");
      ncWriter.addGlobalAttribute(CDM.HISTORY, "final");
      ncWriter.deleteGlobalAttribute("Conventions");

      ncWriter.addVariableAttribute("h", "units", "meters"); // duplicates existing
      ncWriter.addVariableAttribute("h", "new", "stuff");
      ncWriter.renameVariableAttribute(ncWriter.findVariable("time"), "quantity", "quality");

      ncWriter.renameVariable("time", "date");
      ncWriter.renameDimension(null, "time", "date");

      /////////////////////////////////////////////////
      ncWriter.setRedefineMode(false);

      Attribute att = ncWriter.findGlobalAttribute("Conventions");
      assert att == null;
      att = ncWriter.findGlobalAttribute(CDM.HISTORY);
      assert att.getStringValue().equals("final");
      att = ncWriter.findGlobalAttribute("lamp");
      assert att.getStringValue().equals("lava");

      Variable v = ncWriter.findVariable("h");
      att = v.findAttribute("units");
      assert att != null;
      assert att.getStringValue().equals("meters");

      assert ncWriter.findVariable("time") == null;
      v = ncWriter.findVariable("date");
      assert v != null;
      assert v.getRank() == 1;
      assert null != v.findAttribute("quality");

      Dimension d = v.getDimension(0);
      assert d.getShortName().equals("date");

      assert ncWriter.findDimension("time") == null;
      Dimension dim = ncWriter.findDimension("date");
      assert dim != null;
      assert dim.getShortName().equals("date");
      assert dim.equals(d);
      assert dim == d;
    }

    try (NetcdfFileWriter ncWriter = NetcdfFileWriter.openExisting(filename)) {
      ncWriter.setRedefineMode(true);

      ncWriter.addGlobalAttribute("att8", "1234567");

      /////////////////////////////////////////////////
      boolean rewriteAll = ncWriter.setRedefineMode(false);
      assert !rewriteAll;

      Attribute att = ncWriter.findGlobalAttribute("att8");
      assert att != null;
      assert att.getStringValue().equals("1234567") : att.getStringValue();
    }

    try (NetcdfFileWriter ncWriter = NetcdfFileWriter.openExisting(filename)) {
      ncWriter.setRedefineMode(true);

      ncWriter.addGlobalAttribute("att8", "123456789");

      /////////////////////////////////////////////////
      boolean rewriteAll = ncWriter.setRedefineMode(false);
      assert rewriteAll;

      Attribute att = ncWriter.findGlobalAttribute("att8");
      assert att != null;
      assert att.getStringValue().equals("123456789") : att.getStringValue();

      ncWriter.close();
    }
  }

  @Test
  public void testRewriteHeader3() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
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
    }
  }

  @Test
  public void testRedefineClose() throws IOException {
    String filename = tempFolder.newFile().getAbsolutePath();

    // Create a new file
    try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
      Attribute attr = new Attribute("att", 5);
      file.addGroupAttribute(null, attr);
      file.create();
    }

    // Re-open file in redefine mode
    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename)) {
      file.setRedefineMode(true);
      Attribute attr = new Attribute("att2", "foobar");
      file.addGroupAttribute(null, attr);
    }

    // Check that attribute is present
    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename)) {
      Assert.assertNotNull(file.findGlobalAttribute("att"));
      Assert.assertNotNull(file.findGlobalAttribute("att2"));
    }
  }
}
