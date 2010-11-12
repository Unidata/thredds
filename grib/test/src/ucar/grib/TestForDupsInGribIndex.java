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
/**
 * By:   Robb Kambic
 * Date: Jan 28, 2009
 * Time: 2:18:31 PM
 */

package ucar.grib;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grib.grib1.Grib1WriteIndex;
import ucar.grid.GridIndex;
import java.io.IOException;
import java.io.File;

/*
 * junit test for checking for duplicate records in a index by comparing a newly
 * created index against a known index that has the duplicates removed.
 */
public final class TestForDupsInGribIndex extends TestCase {
  /*
   * what dir to index
   */
  private String testPath;
  /*
   * Grib1 Index Writer
   */
  private Grib1WriteIndex g1wi;
  /*
   * Grib2 Index Writer
   */
  private Grib2WriteIndex g2wi;

  /*
   * init
   */
  protected final void setUp() {
    testPath = TestAll.testdataDir + "grid/grib/duplicates/";
    g1wi = new Grib1WriteIndex();
    g2wi = new Grib2WriteIndex();
  }

  /**
   *
   * @return TestSuite results
   */
  public static Test suite() {
    return new TestSuite(TestForDupsInGribIndex.class);
  }


  /*
   * indexes grib files and check for duplicate records against known chk file.
   */
  public final void testIndexer() {

    try {
      File dir = new File( testPath );
      if (dir.isDirectory()) {
        //System.out.println("In directory " + dir.getParent() +File.separator+ dir.getName());
        boolean same = true;
        String[] children = dir.list();
        for (String child : children) {
          String[] args = new String[2];
          args[0] = dir +File.separator+ child;
          if ( child.endsWith("grib2")) {
            args[1] = dir +File.separator+ child + GribIndexName.currentSuffix;
            StringBuilder sb = new StringBuilder();
            sb.append( args[0] );
            File gbx = new File( args[1] );
            if(  gbx.exists() )
              gbx.delete();
            File grib = new File( args[0] );
            g2wi.writeGribIndex(grib, args[0], args[1], false);
            same = checkForDuplicates( args, sb );
            System.out.println( sb.toString() );
            assert( same );
          } else if ( child.endsWith("grib1")) {
            args[1] = dir +File.separator+ child + GribIndexName.currentSuffix;
            StringBuilder sb = new StringBuilder();
            sb.append( args[0] );
            File gbx = new File( args[1] );
            if(  gbx.exists() )
              gbx.delete();
            File grib = new File( args[0] );
            g1wi.writeGribIndex(grib, args[0], args[1], false);
            same = checkForDuplicates( args, sb );
            System.out.println( sb.toString() );
            assert( same );
          }
        }
      }
    } catch (IOException e) {

    }
  }

  /*
   * Reads in newly created index and checks it against know index that has the
   * duplicate records removed.
   * @param args  Grib file name and index name
   * @param sb  log message storage
   */
  private boolean checkForDuplicates( String[] args, StringBuilder sb )
      throws IOException {

    String chk =  args[1].replace( GribIndexName.currentSuffix, ".chk");

    boolean same = true;
    // read in Indexes and compare record count
    GridIndex gbxIdx = new GribIndexReader().open( args[1] );
    GridIndex chkIdx = new GribIndexReader().open( chk );
    if ( gbxIdx.getGridCount() != chkIdx.getGridCount() ) {
      sb.append("\n\tDuplicates Not removed" );
      sb.append( "\n\tNumber of Grid records differ: " );
      sb.append( gbxIdx.getGridCount() );
      sb.append( " not equal to check record count " );
      sb.append( chkIdx.getGridCount() );
      same = false;
    }
    return same;
  }
  
  /**
   * main.
   *
   * @param args dir to check for duplicate records by comparing PDSs
   * @throws IOException on io error
   */
  static public void main(String[] args) throws IOException {
    junit.textui.TestRunner.run(suite());
  }
}
