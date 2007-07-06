package ucar.nc2;

import junit.framework.*;
import java.io.*;

/**
 * test reading a ncfile with long offsets "large format".
 */

public class TestLongOffset extends TestCase  {

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
    tempFile.delete();
  }

  public void testReadLongOffset() throws IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile( "LongOffset.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    NCdump.print(ncfile, "-vall", out, null);
    ncfile.close();
  }

  public void testReadLongOffsetV3mode() throws IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile( "LongOffset.nc");

    NCdump.print(ncfile, "-vall", out, null);
    ncfile.close();
  }

}
