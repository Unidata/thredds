// $Id: TestLocalDodsServer.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.dods;

import junit.framework.TestSuite;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.util.List;

/**
 * @author john
 */
public class TestLocalDodsServer {

  public static String alldata = "dods://localhost:8080/thredds/dodsC/ncdodsTest/";
  public static String testdata = "dods://localhost:8080/thredds/dodsC/ncdodsTest/";

  public static junit.framework.Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new TestSuite(TestTDSselect.class)); //
    suite.addTest(new TestSuite(TestTDScompareWithFiles.class)); //

    return suite;
  }

  public static void doit() throws IOException {
    NetcdfFile ncd = NetcdfDataset.openFile("thredds:resolve:http://motherlode.ucar.edu:9080/thredds/dodsC/station/metar/latest.xml",null);

    List vars = ncd.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v =  (Variable) vars.get(i);
      if (!v.getName().equals("record")) {
        Array data = v.read();
        System.out.println(" read "+v.getName()+" size = "+ data.getSize());
      }
    }

    ncd.close();

  }

  public static void main(String args[]) throws IOException {
    while (true) doit();
  }


}