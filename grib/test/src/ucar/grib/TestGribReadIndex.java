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
 * User: rkambic
 * Date: Jan 29, 2009
 * Time: 10:19:14 AM
 */

package ucar.grib;

//import ucar.grib.Index;

import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Calendar;

/*
 * Times the reading of binary and text indexes
 */
public class TestGribReadIndex {
    /*
   *  read indexes of grib2 files
   *
   */
  private void readIndex( int stop, String sDir) throws IOException {

    long bstart, btime;
    long tstart, ttime;
    Date nowB = Calendar.getInstance().getTime();
    Date nowT = Calendar.getInstance().getTime();

    System.out.println(nowB.toString() + " ... Start of TestGribReadIndex\n");
    String bDir = sDir+ File.separator +"binary";
    String tDir = sDir+ File.separator +"text";
    File bdir = new File(bDir);
    File tdir = new File(tDir);
    // get all indexes to check
    int count = 0;
    long btotal = 0;
    long ttotal = 0;
    String[] bargs = new String[2];
    String[] targs = new String[2];
    System.out.println("\nIndex file             Binary read time  Text Read Time\n" );
    if (bdir.isDirectory()) {
      //System.out.println("In directory " + bdir.getParent() +File.separator+ bdir.getName());
      String[] children = bdir.list();
      for (String child : children) {
        if ( !child.endsWith(GribIndexName.currentSuffix) )
          continue;
        if (count == stop)
          break;
        count++;
        System.out.print( child );
        bargs[0] = bdir +File.separator+ child;
        //bargs[1] = bdir  +File.separator+ child + GribIndexName.currentSuffix;
        bstart = System.currentTimeMillis();
        GribReadIndex.main(bargs);
        btime = System.currentTimeMillis() - bstart;
        btotal += btime;
        System.out.print( "  "+ btime );

        targs[0] = tdir +File.separator+ child;
        //targs[1] = tdir +File.separator+ child + GribIndexName.currentSuffix;
        tstart = System.currentTimeMillis();
        //Index.main(targs);
        ttime = System.currentTimeMillis() - tstart;
        ttotal += ttime;
        System.out.println( "  "+ ttime );  
      }
    }
    System.out.println("\nTotal number of indexes read "+ count );  
    System.out.println("\nStatistics for Binary gbx reads" );
    System.out.println("Total time = " + btotal + " msec");
    System.out.println("Avg time = " + btotal / count + " msec");

    System.out.println("\nStatistics for Text gbx reads" );
    System.out.println("Total time = " + ttotal + " msec");
    System.out.println("Avg time = " + ttotal / count + " msec");

  }

  /**
   * main.
   *
   * @param args dir to index
   * @throws IOException on io error
   */
  // process command line switches
  static public void main(String[] args) throws IOException {
    TestGribReadIndex tg2ri = new TestGribReadIndex();
    //   int numberOfIndexes, String DirToRead
    tg2ri.readIndex( Integer.parseInt(args[0]), args[1]);
  }
}
