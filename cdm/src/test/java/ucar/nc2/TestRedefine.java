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
package ucar.nc2;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

public class TestRedefine{
  String filename = TestLocal.temporaryDataDir + "testRedefine.nc";
  String filename2 = TestLocal.temporaryDataDir + "testRedefine2.nc";
  String filename3 = TestLocal.temporaryDataDir + "testRedefine3.nc";

  @Test
  public void testRedefine() throws IOException, InvalidRangeException {
    try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {

      file.addGlobalAttribute("Conventions", "globulate");
      file.addGlobalAttribute(CDM.HISTORY, "lava");
      file.addGlobalAttribute("att8", "12345678");

      file.addDimension(null, "time", 4, true, false, false);

    /* Add time */
      file.addVariable("time", DataType.DOUBLE, "time");
      file.addVariableAttribute("time", "quantity", "time");
      file.addVariableAttribute("time", "units", "s");

    /* Add a dependent variable */
    file.addVariable("h", DataType.DOUBLE, dims);
    file.addVariableAttribute("h", "quantity", "Height");
    file.addVariableAttribute("h", "units", "m");
    file.create();

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

      file.renameGlobalAttribute(CDM.HISTORY, "lamp");
      file.addGlobalAttribute(CDM.HISTORY, "final");
      file.deleteGlobalAttribute("Conventions");

      file.addVariableAttribute("h", "units", "meters"); // duplicates existing
      file.addVariableAttribute("h", "new", "stuff");
      file.renameVariableAttribute(file.findVariable("time"), "quantity", "quality");

      file.renameVariable("time", "date");
      file.renameDimension(null, "time", "date");

      /////////////////////////////////////////////////
      file.setRedefineMode(false);

      Attribute att = file.findGlobalAttribute("Conventions");
      assert att == null;
      att = file.findGlobalAttribute(CDM.HISTORY);
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
      assert d.getShortName().equals("date");

      assert file.findDimension("time") == null;
      Dimension dim = file.findDimension("date");
      assert dim != null;
      assert dim.getShortName().equals("date");
      assert dim.equals(d);
      assert dim == d;
    }

    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename)) {
      file.setRedefineMode(true);

      file.addGlobalAttribute("att8", "1234567");

      /////////////////////////////////////////////////
      boolean rewriteAll = file.setRedefineMode(false);
      assert !rewriteAll;

      Attribute att = file.findGlobalAttribute("att8");
      assert att != null;
      assert att.getStringValue().equals("1234567") : att.getStringValue();
    }

    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename)) {
      file.setRedefineMode(true);

      file.addGlobalAttribute("att8", "123456789");

      /////////////////////////////////////////////////
      boolean rewritten = file.setRedefineMode(false);
      assert rewritten;

      Attribute att = file.findGlobalAttribute("att8");
      assert att != null;
      assert att.getStringValue().equals("123456789") : att.getStringValue();
    }
  }

  @Test
  public void testRewriteHeader3() throws IOException, InvalidRangeException {
    try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename2)) {
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
    // Create a new file
    try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3,
            filename3)) {
      Attribute attr = new Attribute("att", 5);
      file.addGroupAttribute(null, attr);
      file.create();
    }

    // Re-open file in redefine mode
    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename3)) {
      file.setRedefineMode(true);
      Attribute attr = new Attribute("att2", "foobar");
      file.addGroupAttribute(null, attr);
    }

    // Check that attribute is present
    try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename3)) {
      Assert.assertNotNull(file.findGlobalAttribute("att"));
      Assert.assertNotNull(file.findGlobalAttribute("att2"));
    }
  }
}
