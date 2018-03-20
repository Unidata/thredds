/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
