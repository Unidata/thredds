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

import junit.framework.TestCase;
import org.junit.Test;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.util.rc.RC;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TestGroups extends ucar.nc2.util.TestCommon
{
    static final String DFALTTESTSERVER = "motherlode.ucar.edu:8080";

    public TestGroups(String name, String testdir)
    {
        super(name);
	setTitle("DAP Group tests");
    }

    public TestGroups(String name)
    {
        this(name, null);
    }

    // Collect results locally
    static public class Result {
        String title;
        String url;
        String cdl;
        public Result(String title,String url,String cdl) {this.title=title;this.url=url;this.cdl=cdl;}
    }

    @Test
    public void
    testGroup() throws Exception
   {
        // Check if we are running against motherlode or localhost, or what.
        String testserver = System.getProperty("testserver");
        if(testserver == null) testserver = DFALTTESTSERVER;

       List<Result> results = new ArrayList<Result>();
       if(true) {
           results.add(new Result("Simple multiple groups",
                   "dods://"+testserver+"/dts/group.test1",
                   "netcdf dods://"+testserver+"/dts/group.test1 {\nGroup g1 {\nvariables:\nint i32;\n}\nGroup g2 {\nvariables:\nfloat f32;\n}\n}\n"));
           results.add(new Result("Duplicate variable names in different groups",
                          "dods://"+testserver+"/dts/group.test2",
                          "netcdf dods://"+testserver+"/dts/group.test2 {\nGroup g1 {\nvariables:\nint i32;\n}\nGroup g2 {\nvariables:\nint i32;\n}\n}\n"));
           results.add(new Result("Duplicate coordinate variable names in Grid",
                          "dods://"+testserver+"/dts/docExample",
                          "netcdf dods://"+testserver+"/dts/docExample {\n dimensions:\n lat = 180;\n lon = 360;\n time = 404;\n variables:\n double lat(lat=180);\n :fullName = \"latitude\";\n :units = \"degrees North\";\n double lon(lon=360);\n :fullName = \"longitude\";\n :units = \"degrees East\";\n double time(time=404);\n :units = \"seconds\";\n int sst(time=404, lat=180, lon=360);\n :_CoordinateAxes = \"time lat lon \";\n :fullName = \"Sea Surface Temperature\";\n :units = \"degrees centigrade\";\n}\n"));
       }
       if(false) {
           results.add(new Result("TestTDSLocal Failure",
                   "http://motherlode.ucar.edu:8080/thredds/dodsC/ExampleNcML/Agg.nc",
                   ""));
       }
       if(false) {
           results.add(new Result("TestTDSLocal Failure",
                   "http://localhost:8080/dts/structdupname",
                   ""));
       }

        if(!RC.getUseGroups())   {
            System.out.println("Groups not supported; continuing");
            //junit.framework.Assert.assertTrue("Groups not supported; continuing", true);
            //return; // do not run if groups are not being supported
        }

        for(Result result: results) {
            System.out.println("TestGroups: "+result.url);
            DODSNetcdfFile ncfile = new DODSNetcdfFile(result.url);
	    if(ncfile == null)
	        throw new Exception("Cannot read: "+result.url);
            StringWriter ow = new StringWriter();
            PrintWriter pw = new PrintWriter(ow);
	        ncfile.writeCDL(pw, false);
            try {pw.close(); ow.close();} catch (IOException ioe) {};
            StringReader baserdr = new StringReader(result.cdl);
            String captured = ow.toString();
            StringReader resultrdr = new StringReader(captured);
            // Diff the two files
            Diff diff = new Diff("Testing "+result.title);
            boolean pass = !diff.doDiff(baserdr, resultrdr);
            baserdr.close(); resultrdr.close();
            if (!pass) {
                junit.framework.Assert.assertTrue("Testing "+result.title, pass);
            }
            // Dump the output for visual comparison
            if(System.getProperty("visual") != null) {
                System.out.println("Testing "+result.title+" visual:");
                System.out.println("---------------");
                System.out.print(captured);
                System.out.println("---------------");
            }
        }
        System.out.flush();
        System.err.flush();
    }


}
