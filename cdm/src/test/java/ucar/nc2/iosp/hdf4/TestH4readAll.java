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

package ucar.nc2.iosp.hdf4;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestH4readAll extends TestCase {

  public TestH4readAll(String name) {
    super(name);
  }

  public void testReadAll() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = 0;
    count += TestAll.readAllDir("R:/testdata/hdf4/", new MyFileFilter());
    System.out.println("***READ "+count+" files");
    count += TestAll.readAllDir("D:/hdf4/", null);
    System.out.println("***READ "+count+" files");
  }

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".hdf") || pathname.getName().endsWith(".eos");
    }
  }

  public void testProblems() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = TestAll.readAllDir("E:/problem/", null);
    System.out.println("***READ "+count+" files");
  }


  public void problem() throws IOException {
    H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag1 H4header/tagDetail H4header/linked H4header/construct"));
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag2 H4header/tagDetail H4header/construct"));
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/linked"));

    //TestAll.readAll("E:/problem/MAC021S0.A2007287.1920.002.2007289002404.hdf");

    NetcdfFile ncfile = NetcdfFile.open("R:\\testdata\\hdf4\\c402_rp_02.diag.sfc.20020122_0130z.hdf");
    Variable v = ncfile.findVariable("MOD_Grid_MOD17A2/Data Fields/PsnNet_1km");
    assert v != null;
    v.read();
    ncfile.close();
  }
}
