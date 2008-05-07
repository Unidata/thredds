package ucar.nc2.dods;

import junit.framework.*;

import java.io.*;

/** Test nc2 dods in the JUnit framework.
 *  Open and read various test datasets.
 */

public class TestDODSRead extends TestCase {

  public static String testServer = TestDODS.server;
  public static boolean showFile = false, showFileDebug= false;

  static DODSNetcdfFile open(String name) throws IOException {
    String filename = testServer+name;
    return openAbs( filename);
  }

  static DODSNetcdfFile openAbs(String filename) throws IOException {
    System.out.println("TestDODSRead = "+filename);
      DODSNetcdfFile dodsfile = new DODSNetcdfFile(filename);
      if (showFileDebug) System.out.println(dodsfile.toStringDebug());
      if (showFile) System.out.println(dodsfile.toString());
      return dodsfile;
  }

  public TestDODSRead( String name) {
    super(name);
  }

  public void testRead() throws IOException {
    // simple
    open( "test.01");
    open( "test.02");
    open( "test.03");
    open( "test.04");
    open( "test.05");
    open( "test.06");
    open( "test.06a");
    open( "test.07");
    open( "test.07a");

    // nested
    open( "test.21");
    open( "test.22");
    //open( "test.23");
    //open( "test.31");
    //open( "test.32");

    open( "test.50"); // structure array
    open( "test.53"); // nested structure in structure array
    open( "test.vs5"); // structure array */

  }

  public static void main( String arg[]) throws IOException {
    showFile = true;
    open("test.07");

    //openAbs("http://motherlode.ucar.edu:8088/thredds/dodsC/cf1.nc");
    //openAbs("http://motherlode.ucar.edu/cgi-bin/dods/DODS-3.2.1/nph-dods/dods/model/2004110500_eta_211.nc");
  }

}
