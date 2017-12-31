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
package ucar.nc2;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/**
 * Test NcdumpW.
 */
public class TestNcDump {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Asserts that the issue identified in PCM-232977 has been fixed.
  // See https://andy.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=28658.
  // Asserts that GitHub issue #929 has been fixed. See https://github.com/Unidata/thredds/issues/929
  @Test
  public void testUnsignedFillValue() throws IOException {
    try (StringWriter sw = new StringWriter()) {
      NCdumpW.print(TestDir.cdmLocalTestDataDir + "testUnsignedFillValue.ncml",
              sw, true, true, false, false, null, null);

      File expectedOutputFile = new File(TestDir.cdmLocalTestDataDir, "testUnsignedFillValue.dump");
      String expectedOutput = Files.toString(expectedOutputFile, Charsets.UTF_8);

      Assert.assertEquals(toUnixEOLs(expectedOutput), toUnixEOLs(sw.toString()));
    }
  }

  // Make sure the indentation is correct with a complex, nested structure.
  @Test
  public void testNestedGroups() throws IOException {
    try (StringWriter sw = new StringWriter()) {
      NCdumpW.print(TestDir.cdmLocalTestDataDir + "testNestedGroups.ncml",
              sw, true, true, false, false, null, null);

      File expectedOutputFile = new File(TestDir.cdmLocalTestDataDir, "testNestedGroups.dump");
      String expectedOutput = Files.toString(expectedOutputFile, Charsets.UTF_8);

      Assert.assertEquals(toUnixEOLs(expectedOutput), toUnixEOLs(sw.toString()));
    }
  }

  public static String toUnixEOLs(String input) {
    return input.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
  }
}
