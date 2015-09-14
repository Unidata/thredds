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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLWriter;

public class TestSpecialChars extends TestCase {

  public TestSpecialChars( String name) {
    super(name);
  }

  String trouble = "here is a &, <, >, \', \", \n, \r, \t, to handle";

  public void testWriteAndRead() throws IOException, InvalidRangeException {
    String filename = TestLocal.temporaryDataDir +"testSpecialChars.nc";
    NetcdfFileWriter ncfilew = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);
    ncfilew.addGlobalAttribute("omy", trouble);

    ncfilew.addDimension("t", 1);

    // define Variables
    Variable tvar = ncfilew.addStringVariable(null, "t", new ArrayList<>(), trouble.length());
    ncfilew.addVariableAttribute("t", "yow", trouble);

    ncfilew.create();

    Array data = Array.factory(DataType.STRING, new int[0]);
    data.setObject( data.getIndex(), trouble);
    ncfilew.writeStringData(tvar, data);
    ncfilew.close();

    try (NetcdfFile ncfile = NetcdfFile.open(filename, null)) {
      String val = ncfile.findAttValueIgnoreCase(null, "omy", null);
      assert val != null;
      assert val.equals(trouble);

      Variable v = ncfile.findVariable("t");
      v.setCachedData(v.read(), true);

      val = ncfile.findAttValueIgnoreCase(v, "yow", null);
      assert val != null;
      assert val.equals(trouble);

      ncfile.writeCDL(System.out, false);
      ncfile.writeNcML(System.out, null);

      NcMLWriter ncmlWriter = new NcMLWriter();
      Element netcdfElem = ncmlWriter.makeNetcdfElement(ncfile, null);

      ncmlWriter.writeToStream(netcdfElem, System.out);
      ncmlWriter.writeToFile(netcdfElem, new File(TestLocal.temporaryDataDir, "testSpecialChars.ncml"));
    }

    String filename2 = TestLocal.temporaryDataDir + "testSpecialChars.ncml";

    try (NetcdfFile ncfile = NetcdfDataset.openFile(filename2, null)) {
      System.out.println("ncml= " + ncfile.getLocation());

      String val = ncfile.findAttValueIgnoreCase(null, "omy", null);
      assert val != null;
      assert val.equals(trouble);

      Variable v = ncfile.findVariable("t");
      v.setCachedData(v.read(), true);

      val = ncfile.findAttValueIgnoreCase(v, "yow", null);
      assert val != null;
      assert val.equals(trouble);

      NcMLWriter ncmlWriter = new NcMLWriter();
      Element netcdfElem = ncmlWriter.makeNetcdfElement(ncfile, null);
      ncmlWriter.writeToStream(netcdfElem, System.out);
    }
  }
}
