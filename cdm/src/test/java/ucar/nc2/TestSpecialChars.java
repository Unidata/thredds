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

import junit.framework.TestCase;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * @author caron
 * @since Aug 7, 2007
 */
public class TestSpecialChars extends TestCase {
  private boolean show = false;

  public TestSpecialChars( String name) {
    super(name);
  }

  String trouble = "here is a &, <, >, \', \", \n, \r, \t, to handle";

  public void testWrite() throws IOException, InvalidRangeException {
    String filename = TestLocal.cdmTestDataDir +"testSpecialChars.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, true);
    ncfile.addGlobalAttribute("omy", trouble);

    ncfile.addDimension("t", 1);

    // define Variables
    ncfile.addStringVariable("t",new ArrayList<Dimension>(), trouble.length());
    ncfile.addVariableAttribute("t", "yow", trouble);

    ncfile.create();

    Array data = Array.factory(DataType.STRING, new int[0]);
    data.setObject( data.getIndex(), trouble);
    ncfile.writeStringData("t", data);
    ncfile.close();
  }

  public void testRead() throws IOException {
    String filename = TestLocal.cdmTestDataDir +"testSpecialChars.nc";
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    String val = ncfile.findAttValueIgnoreCase(null, "omy", null);
    assert val != null;
    assert val.equals(trouble);

    Variable v = ncfile.findVariable("t");
    v.setCachedData( v.read(), true);

    val = ncfile.findAttValueIgnoreCase(v, "yow", null);
    assert val != null;
    assert val.equals(trouble);

    ncfile.writeCDL(System.out, false);
    ncfile.writeNcML(System.out, null);

    NcMLWriter w = new NcMLWriter();
    w.writeXML(ncfile, System.out, null);

    OutputStream out = new FileOutputStream(TestLocal.cdmTestDataDir +"testSpecialChars.ncml");
    w.writeXML(ncfile, out, null);
    out.close();

    ncfile.close();
  }

  public void testReadNcML() throws IOException {
    String filename = TestLocal.cdmTestDataDir +"testSpecialChars.ncml";
    NetcdfFile ncfile = NetcdfDataset.openFile(filename, null);
    System.out.println("ncml= "+ncfile.getLocation());

    String val = ncfile.findAttValueIgnoreCase(null, "omy", null);
    assert val != null;
    assert val.equals(trouble);

    Variable v = ncfile.findVariable("t");
    v.setCachedData( v.read(), true);

    val = ncfile.findAttValueIgnoreCase(v, "yow", null);
    assert val != null;
    assert val.equals(trouble);

    NcMLWriter w = new NcMLWriter();
    w.writeXML(ncfile, System.out, null);

    ncfile.close();
  }
}
