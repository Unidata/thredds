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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.IO;
import ucar.unidata.test.util.TestDir;

public class TestWriteMiscProblems extends TestCase {

  public TestWriteMiscProblems(String name) {
    super(name);
  }

  public void testWriteBigString() throws IOException {
    String filename = TestLocal.temporaryDataDir + "testWriteMisc.nc";
    try (NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(filename, false)) {

      int len = 120000;
      ArrayChar.D1 arrayCharD1 = new ArrayChar.D1(len);
      for (int i = 0; i < len; i++)
        arrayCharD1.set(i, '1');
      ncfile.addGlobalAttribute( new Attribute("tooLongChar", arrayCharD1));

      char[] carray = new char[len];
      for (int i = 0; i < len; i++)
        carray[i] = '2';
      String val = new String(carray);
      ncfile.addGlobalAttribute("tooLongString", val);
      ncfile.create();
    }
  }


  public void testCharMultidim() throws IOException, InvalidRangeException {

    /* dimension lengths */
    final int Time_len = 0;
    final int DateStrLen_len = 19;

    /* enter define mode */
    String filename = TestLocal.temporaryDataDir + "testCharMultidim.nc";
    NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(filename, true);

    /* define dimensions */
    Dimension Time_dim = ncfile.addUnlimitedDimension("Time");
    Dimension DateStrLen_dim = ncfile.addDimension("DateStrLen", DateStrLen_len);

    /* define variables */
    ncfile.addVariable("Times", DataType.CHAR, "Time DateStrLen");
    ncfile.create();

    /* assign variable data */
    String contents = "2005-04-11_12:00:002005-04-11_13:00:00";
    ArrayChar data = new ArrayChar(new int[]{2, 19});
    IndexIterator iter = data.getIndexIterator();
    int count = 0;
    while (iter.hasNext())
      iter.setCharNext(contents.charAt(count++));
    ncfile.write("Times", data);
    ncfile.close();

    NetcdfFile nc = NetcdfFile.open(filename, null);
    Variable v = nc.findVariable("Times");
    Array dataRead = v.read();
    assert dataRead instanceof ArrayChar;
    ArrayChar dataC = (ArrayChar) dataRead;
    for (int i=0; i<2; i++)
      System.out.printf("%d == %s %n", i, dataC.getString(i));
    assert dataC.getString(0).equals("2005-04-11_12:00:00");
    assert dataC.getString(1).equals("2005-04-11_13:00:00");

    nc.close();
  }

  public void testRemove() throws IOException, InvalidRangeException {
    try {
      String inName = TestDir.cdmLocalTestDataDir + "testWrite.nc";
      String outName = TestLocal.temporaryDataDir + "testRemove.nc";

      DatasetUrl durl = new DatasetUrl(null, inName);
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(durl, true, null);
      System.out.println(ncd);
      System.out.println(ncd.removeVariable(null, "temperature"));
      System.out.println(ncd);

      ncd.finish();

      FileWriter2 writer = new FileWriter2(ncd, outName, NetcdfFileWriter.Version.netcdf3, null);
      NetcdfFile ncdnew = writer.write();
      ncdnew.close();
      ncd.close();

      DatasetUrl durl2 = new DatasetUrl(null, outName);
      NetcdfDataset ncdnew2 = NetcdfDataset.acquireDataset(durl2, true, null);
      assert ncdnew2.findVariable("temperature") == null;
      ncdnew2.close();
      
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  public void testRedefine() throws IOException, InvalidRangeException {
    String org = TestDir.cdmLocalTestDataDir + "testWriteRecord.nc";
    String path = TestLocal.temporaryDataDir + "testWriteRecordRedefine.nc";
    File orgFile = new File(org);
    File newFile = new File(path);
    if (newFile.exists()) newFile.delete();
    IO.copyFile(orgFile, newFile);

    NetcdfFileWriter ncfile = null;
    try {
      ncfile = NetcdfFileWriter.openExisting(path);
      System.out.println(ncfile);
      
      ncfile.setRedefineMode(true);

      Variable tvar = ncfile.findVariable("T");
      ncfile.setExtraHeaderBytes(1024);
      ncfile.addVariable("header_data", DataType.FLOAT, tvar.getDimensions());
      System.out.println(ncfile);

    } finally {
      try {
        if (ncfile != null) {
          ncfile.setRedefineMode(false);
          ncfile.flush();
          ncfile.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
