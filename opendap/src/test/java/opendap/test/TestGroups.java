/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package opendap.test;

import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.util.UnitTestCommon;
import ucar.nc2.util.rc.RC;
import ucar.unidata.test.Diff;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TestGroups extends UnitTestCommon
{

  static final String DFALTTESTSERVER = "thredds-test.ucar.edu";
  //static final String DFALTTESTSERVER = "localhost:8080";


  // Collect results locally
  static private class Testcase {
    String title;
    String url;
    String cdl;

    public Testcase(String title, String url, String cdl) {
      this.title = title;
      this.url = url;
      this.cdl = cdl;
    }
  }

  boolean usegroups = RC.getUseGroups();
  String testserver = null;
  List<Testcase> testcases = null;

  public TestGroups()
      throws Exception
  {
    super("DAP Group tests");
    // Check if user specified server.
    testserver = System.getProperty("testserver");
    if (testserver == null) testserver = DFALTTESTSERVER;
    definetestcases();
  }

  void
  definetestcases()
  {
     testcases = new ArrayList<Testcase>();
     if(false) {// use this arm to do debugging
         testcases.add(new Testcase("External user provided group example",
                     "http://"+testserver+"/thredds/dodsC/testdods/K1VHR.nc",
                     "file://"+threddsRoot + "/opendap/src/test/data/baselinemisc/K1VHR.cdl")
                     );
    } else {

     testcases.add(new Testcase("Simple multiple groups",
             "dods://" + testserver + "/dts/group.test1",
             "netcdf dods://" + testserver + "/dts/group.test1 {\ngroup: g1 {\nvariables:\nint i32;\n}\ngroup: g2 {\nvariables:\nfloat f32;\n}\n}\n")
     );
     testcases.add(new Testcase("Duplicate variable names in different groups",
             "dods://" + testserver + "/dts/group.test2",
             "netcdf dods://" + testserver + "/dts/group.test2 {\ngroup: g1 {\nvariables:\nint i32;\n}\ngroup: g2 {\nvariables:\nint i32;\n}\n}\n"));
     testcases.add(new Testcase("Duplicate coordinate variable names in Grid",
             "dods://" + testserver + "/dts/docExample",
             "netcdf dods://" + testserver + "/dts/docExample {\n dimensions:\n lat = 180;\n lon = 360;\n time = 404;\n variables:\n double lat(lat=180);\n :fullName = \"latitude\";\n :units = \"degrees North\";\n double lon(lon=360);\n :fullName = \"longitude\";\n :units = \"degrees East\";\n double time(time=404);\n :units = \"seconds\";\n int sst(time=404, lat=180, lon=360);\n :_CoordinateAxes = \"time lat lon \";\n :fullName = \"Sea Surface Temperature\";\n :units = \"degrees centigrade\";\n}\n"));
     /* Not currently available
        testcases.add(new Testcase("TestTDSLocal Failure",
             "http://" + testserver + "/thredds/dodsC/ExampleNcML/Agg.nc",
             ""));
        testcases.add(new Testcase("TestTDSLocal Failure",
             "http://localhost:8080/dts/structdupname",
             ""));
     */
     testcases.add(new Testcase("External user provided group example",
             "http://"+testserver+"/thredds/dodsC/testdods/K1VHR.nc",
             "file://"+threddsRoot + "/opendap/src/test/data/baselinemisc/K1VHR.cdl")
             );
     }
  }

    public void
  testGroup() throws Exception
  {
    // Run  with usegroups == true
    if(!usegroups)
        assertTrue("TestGroups: Group Support not enabled",false);
    System.out.println("TestGroups:");
    for (Testcase testcase : testcases) {
      System.out.println("url: " + testcase.url);
      boolean pass = process1(testcase);
      if (!pass)
        assertTrue("Testing " + testcase.title, pass);
    }
  }

  boolean process1(Testcase testcase)
          throws Exception
  {
      DODSNetcdfFile ncfile = new DODSNetcdfFile(testcase.url);
      if (ncfile == null)
        throw new Exception("Cannot read: " + testcase.url);
      StringWriter ow = new StringWriter();
      PrintWriter pw = new PrintWriter(ow);
      ncfile.writeCDL(pw, false);
      try {
        pw.close();
        ow.close();
      } catch (IOException ioe) {};
      String captured = ow.toString();
      visual(testcase.title,captured);

      // See if the cdl is in a file or a string.
      Reader baserdr = null;
      if(testcase.cdl.startsWith("file://")) {
          File f = new File(testcase.cdl.substring("file://".length(),testcase.cdl.length()));
          try {
              baserdr = new FileReader(f);
          }  catch (Exception e) {
              return false;
          }
      } else
          baserdr = new StringReader(testcase.cdl);
      StringReader resultrdr = new StringReader(captured);
      // Diff the two files
      Diff diff = new Diff("Testing " + testcase.title);
      boolean pass = !diff.doDiff(baserdr, resultrdr);
      baserdr.close();
      resultrdr.close();
      return pass;
  }

  void visual(String title, String captured)
  {
    // Dump the output for visual comparison
    if (System.getProperty("visual") != null) {
        System.out.println("Testing " + title + " visual:");
        System.out.println("---------------");
        System.out.print(captured);
        System.out.println("---------------");
    }
  }

  /**
   Remove any _lastModified attributes
   @param s The string to be modified
   @return The modified string
   */
  String nolastmodified(String s)
  {
    StringReader sr = new StringReader(s);
    StringWriter sw = new StringWriter();
    BufferedReader br = new BufferedReader(sr);
    String line;
    try {
    while((line=br.readLine()) != null) {
      if(line.contains("odified"))
          continue;
      sw.write(line+"\n");
    }
    br.close();
    sr.close();
    sw.close();
    return sw.toString();
    } catch (IOException ioe) {return null;}
  }

}
