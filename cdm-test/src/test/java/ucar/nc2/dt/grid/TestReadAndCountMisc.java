/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.test.util.NeedsExternalResource;
import ucar.unidata.test.util.TestDir;

/**
 * Describe
 *
 * @author caron
 * @since 3/5/2015
 */
@Category(NeedsExternalResource.class)
public class TestReadAndCountMisc {
  // For some reason, this fails on Travis after a 10-minute timeout. It succeeds everywhere else.
  // We assume that thredds.ucar.edu is unreachable due to some limitation and/or bug in Travis.
  @Test
  public void testLiveServer() throws Exception {
    TestReadandCount.doOne("thredds:resolve:http://"+TestDir.threddsServer+"/thredds/",
            "catalog/grib/NCEP/NAM/CONUS_20km/noaaport/files/latest.xml", 33, 9, 11, 7);  // flips between 40 and 33
  }

  @Test
  public void testTestServer() throws Exception {
    TestReadandCount.doOne("thredds:resolve:http://"+TestDir.threddsTestServer+"/thredds/",
            "catalog/grib/NCEP/NAM/CONUS_20km/noaaport/files/latest.xml", 33, 9, 11, 7);
  }

  @Test
  public void testDevServer() throws Exception {
    TestReadandCount.doOne("thredds:resolve:http://"+TestDir.threddsDevServer+"/thredds/",
            "catalog/grib/NCEP/NAM/CONUS_20km/noaaport/files/latest.xml", 33, 9, 11, 7);
  }
}
