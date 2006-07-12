// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dods;

import junit.framework.TestSuite;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.Array;

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