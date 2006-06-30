package ucar.nc2;

import junit.framework.*;

import java.io.*;

/**
 * Simple example to print contents of an existing netCDF file of
 * unknown structure, much like ncdump.  A difference is the nesting of
 * multidimensional array data is represented by nested brackets, so the
 * output is not legal CDL that can be used as input for ncgen.
 *
 * @author Russ Rew
 * @version $Id: TestDump.java,v 1.8 2005/12/15 00:24:37 caron Exp $ */

public class TestDump extends TestCase  {

  public TestDump( String name) {
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

  public void testNCdump() {
    try {
      NCdump.print(TestNC2.topDir+"testWrite.nc", out, false, true, false, false, null, null);
      NCdump.printNcML(TestNC2.topDir+"testWriteRecord.nc", out);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert (false);
    }

    System.out.println( "**** testNCdump done");
  }

}
