/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 16, 2008
 */
public class TestWriteMiscProblems {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWriteBigString() throws IOException {
    String filename = tempFolder.newFile().getAbsolutePath();
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

  @Test
  public void testCharMultidim() throws IOException, InvalidRangeException {
    /* dimension lengths */
    final int Time_len = 0;
    final int DateStrLen_len = 19;

    /* enter define mode */
    String filename = tempFolder.newFile().getAbsolutePath();
    try (NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(filename, true)) {
      /* define dimensions */
      Dimension Time_dim       = ncfile.addUnlimitedDimension("Time");
      Dimension DateStrLen_dim = ncfile.addDimension("DateStrLen", DateStrLen_len);

      /* define variables */
      List<Dimension> Times_dimlist = new ArrayList<Dimension>();
      Times_dimlist.add(Time_dim);
      Times_dimlist.add(DateStrLen_dim);
      ncfile.addVariable("Times", DataType.CHAR, Times_dimlist);
      ncfile.create();

      /* assign variable data */
      String        contents = "2005-04-11_12:00:002005-04-11_13:00:00";
      ArrayChar     data     = new ArrayChar(new int[] { 2, 19 });
      IndexIterator iter     = data.getIndexIterator();
      int           count    = 0;
      while (iter.hasNext()) {
        iter.setCharNext(contents.charAt(count++));
      }

      ncfile.write("Times", data);
    }

    try (NetcdfFile nc = NetcdfFile.open(filename, null)) {
      Variable v        = nc.findVariable("Times");
      Array    dataRead = v.read();
      assert dataRead instanceof ArrayChar;
      ArrayChar dataC = (ArrayChar) dataRead;

      assert dataC.getString(0).equals("2005-04-11_12:00:00");
      assert dataC.getString(1).equals("2005-04-11_13:00:00");
    }
  }

  @Test
  public void testRemove() throws IOException, InvalidRangeException {
    String inName = TestDir.cdmLocalTestDataDir + "testWrite.nc";
    String outName = tempFolder.newFile().getAbsolutePath();

    DatasetUrl durl = new DatasetUrl(null, inName);
    try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(durl, true, null)) {
      assert ncd.removeVariable(null, "temperature");
      ncd.finish();

      FileWriter2 writer = new FileWriter2(ncd, outName, NetcdfFileWriter.Version.netcdf3, null);
      try (NetcdfFile ncdnew = writer.write()) {
        // ok empty
      }
    }

    DatasetUrl durl2 = new DatasetUrl(null, outName);
    try (NetcdfDataset ncdnew2 = NetcdfDataset.acquireDataset(durl2, true, null)) {
      assert ncdnew2.findVariable("temperature") == null;
    }
  }

  @Test
  public void testRedefine() throws IOException, InvalidRangeException {
    String org = TestDir.cdmLocalTestDataDir + "testWriteRecord.nc";
    String path = tempFolder.newFile().getAbsolutePath();
    File orgFile = new File(org);
    File newFile = new File(path);
    if (newFile.exists()) newFile.delete();
    IO.copyFile(orgFile, newFile);

    try (NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(path)) {
      System.out.println(ncfile);

      ncfile.setRedefineMode(true);

      Variable tvar = ncfile.findVariable("T");
      ncfile.setExtraHeaderBytes(1024);
      ncfile.addVariable("header_data", DataType.FLOAT, tvar.getDimensions());
      System.out.println(ncfile);
    }
  }
}
