/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/**
 * test reading a ncfile with long offsets "large format".
 */
public class TestLongOffset extends TestCase  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestLongOffset( String name) {
    super(name);
  }

  File tempFile;
  FileOutputStream out;
  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new FileOutputStream( tempFile);
  }
  protected void tearDown() throws Exception {
    out.close();
    if (!tempFile.delete())
      logger.debug("delete failed on {}",tempFile);
  }

  public void testReadLongOffset() throws IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("longOffset.nc")) {
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      StringWriter sw = new StringWriter();
      NCdumpW.print(ncfile, "-vall", sw, null);
      logger.debug(sw.toString());
    }
  }

  public void testReadLongOffsetV3mode() throws IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal( "longOffset.nc")) {
      StringWriter sw = new StringWriter();
      NCdumpW.print(ncfile, "-vall", sw, null);
      logger.debug(sw.toString());
    }
  }
}
