package ucar.nc2;

import junit.framework.*;
import java.io.*;

/**
 * Simple example to print contents of an existing netCDF file of
 * unknown structure, much like ncdump.  A difference is the nesting of
 * multidimensional array data is represented by nested brackets, so the
 * output is not legal CDL that can be used as input for ncgen.
 *
 * @author: Russ Rew
 * @version: $Id: TestLongOffset.java 51 2006-07-12 17:13:13Z caron $ */

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
    NetcdfFile ncfile = TestNC2.openFile( "LongOffset.nc");
    ncfile.addRecordStructure();

    NCdump.print(ncfile, "-vall", out, null);
    ncfile.close();
  }

  public void testReadLongOffsetV3mode() throws IOException {
    NetcdfFile ncfile = TestNC2.openFile( "LongOffset.nc");

    NCdump.print(ncfile, "-vall", out, null);
    ncfile.close();
  }

}
