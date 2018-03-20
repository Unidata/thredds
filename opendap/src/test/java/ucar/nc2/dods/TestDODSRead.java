/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import opendap.test.TestSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test nc2 dods in the JUnit framework.
 *  Open and read various test datasets from the dts server.
 */
public class TestDODSRead {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static boolean showFile = false, showFileDebug= false;

  static DODSNetcdfFile open(String name) throws IOException {
    String filename = TestSources.XURL1 + "/" + name;
    return openAbs( filename);
  }

  static DODSNetcdfFile openAbs(String filename) throws IOException {
    System.out.println("TestDODSRead = "+filename);
      DODSNetcdfFile dodsfile = new DODSNetcdfFile(filename);
      if (showFileDebug) System.out.println(dodsfile.getDetailInfo());
      if (showFile) System.out.println(dodsfile.toString());
      return dodsfile;
  }

  @org.junit.Test
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

}
