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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.Diff;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

public class TestDuplicates extends UnitTestCommon
{
  public TestDuplicates() {
    setTitle("DAP duplicate names tests");
  }

  // Collect results locally
  static public class Result {
    String title;
    String url;
    String cdl;

    public Result(String title, String url, String cdl) {
      this.title = title;
      this.url = url;
      this.cdl = cdl;
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testDuplicates() throws Exception {
    // Check if we are running against remote or localhost, or what.
    String testserver = TestDir.dap2TestServer;

    List<Result> results = new ArrayList<Result>();
    if (true) {
      results.add(new Result("Top and field vars have same names",
              "http://" + testserver + "/dts/structdupname",
              "netcdf dods://" + testserver + "/dts/structdupname {\n" +
                      " variables:\n" +
                      "   int time;\n" +
                      "Structure {\n" +
                      "   float time;\n" +
                      "} record;\n" +
                      "}"));
    }
    if (true) {
      results.add(new Result("TestFailure",
              "http://" + testserver + "/dts/simplestruct",
              "netcdf dods://" + testserver + "/dts/simplestruct {\n" +
                      " variables:\n" +
                      "Structure {\n" +
                      "   int i32;\n" +
                      "} types;\n" +
                      "}"));
    }
    boolean pass = true;
    for (Result result : results) {
      System.out.println("TestDuplicates: " + result.url);
      boolean localpass = true;
      try {
        DODSNetcdfFile ncfile = new DODSNetcdfFile(result.url);
        if (ncfile == null)
          throw new Exception("Cannot read: " + result.url);
        StringWriter ow = new StringWriter();
        PrintWriter pw = new PrintWriter(ow);
        ncfile.writeCDL(pw, false);
        try {
          pw.close();
          ow.close();
        } catch (IOException ioe) {
        }
        ;
        StringReader baserdr = new StringReader(result.cdl);
        String captured = ow.toString();
        StringReader resultrdr = new StringReader(captured);
        // Diff the two files
        Diff diff = new Diff("Testing " + result.title);
        localpass = !diff.doDiff(baserdr, resultrdr);
        baserdr.close();
        resultrdr.close();
        // Dump the output for visual comparison
        if (System.getProperty("visual") != null) {
          System.out.println("Testing " + result.title + " visual:");
          System.out.println("---------------");
          System.out.print(captured);
          System.out.println("---------------");
        }
      } catch (IllegalArgumentException e) {
        localpass = false;
      }
      if (!localpass)
        pass = false;
    }
    System.out.flush();
    System.err.flush();
    Assert.assertTrue("Testing " + getTitle(), pass);
  }


}
