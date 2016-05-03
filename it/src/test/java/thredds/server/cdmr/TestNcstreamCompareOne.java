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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;

/**
 * Test a single file; compare to cdmremote
 *
 * @author caron
 * @since 10/21/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestNcstreamCompareOne {

  @Test
   public void problem() throws IOException {
  // http://localhost:8081/thredds/cdmremote/scanCdmUnitTests/formats/grib2/SingleRecordNbits0.grib2?req=header
     String problemFile = TestNcstreamCompareWithFiles.contentRoot + "/grib2/SingleRecordNbits0.grib2";
     String name = StringUtil2.substitute(problemFile.substring(TestNcstreamCompareWithFiles.contentRoot.length()), "\\", "/");
     String remote = TestWithLocalServer.withPath(TestNcstreamCompareWithFiles.urlPath + name);
     TestNcstreamCompareWithFiles.compareDatasets(problemFile, remote);
   }
}
