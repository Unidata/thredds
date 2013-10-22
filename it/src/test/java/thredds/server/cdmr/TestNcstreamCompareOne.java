/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.server.cdmr;

import org.junit.Assert;
import org.junit.Test;
import thredds.TestWithLocalServer;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.Formatter;

/**
 * Test a single file compare to cdmremote
 *
 * @author caron
 * @since 10/21/13
 */
public class TestNcstreamCompareOne {
  static String contentRoot = TestDir.cdmUnitTestDir + "formats";
  static String urlPath = TestWithLocalServer.server + "cdmremote/scanCdmUnitTests/formats";

  @Test
   public void problem() throws IOException {
     String problemFile = contentRoot + "/gempak/19580807_upa.gem";
     String name = StringUtil2.substitute(problemFile.substring(contentRoot.length()), "\\", "/");
     String remote = urlPath + name;
     compareDatasets(problemFile, remote);
   }

   private void compareDatasets(String local, String remote) throws IOException {
     NetcdfFile ncfile = null, ncremote = null;
     try {
       ncfile = NetcdfDataset.openFile(local, null);
       ncremote = new CdmRemote(remote);

       Formatter f = new Formatter();
       CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
       boolean ok = mind.compare(ncfile, ncremote, new NcstreamObjFilter(), false, false, false);
       if (!ok) {
         System.out.printf("--Compare %s to %s%n", local, remote);
         System.out.printf("  %s%n", f);
       }
       Assert.assertTrue(local + " != " + remote, ok);
     } finally {
       if (ncfile != null) ncfile.close();
       if (ncremote != null) ncremote.close();
     }
   }

   private class NcstreamObjFilter implements CompareNetcdf2.ObjFilter {

     @Override
     public boolean attOk(Variable v, Attribute att) {
       // if (v != null && v.isMemberOfStructure()) return false;
       String name = att.getShortName();

       if (name.equals(_Coordinate.Axes)) return false;

       return true;
     }

   }
}
