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

import ucar.grib.GribIndexName;
import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grib.grib1.Grib1WriteIndex;

import java.util.Calendar;
import java.util.Date;
import java.io.IOException;
import java.io.File;

public class TestGribWriteIndex {

  /*
   * indexes grib files
   * @param type binary or text
   * @param stop number of files to index
   * @param what dir to index, puts indexes in sub-dir binary or text
   *
   */
  private void indexer(  String sDir) throws IOException {

    long start = System.currentTimeMillis();
    Date now = Calendar.getInstance().getTime();
    //System.out.println(now.toString() + " ... Start of TestGribWriteIndex");
    File dir = new File(sDir);
    int count =0;
    if (dir.isDirectory()) {
      //System.out.println("In directory " + dir.getParent() +File.separator+ dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        count++;
        String[] args = new String[2];
        args[0] = dir +File.separator+ child;
        if ( child.endsWith("grib2")) {
          args[1] = dir +File.separator+ child + GribIndexName.currentSuffix;
          File gbx = new File( args[1] );
          if(  gbx.exists() )
            gbx.delete();
          Grib2WriteIndex.main(args);
        } else if ( child.endsWith("grib1")) {
          args[1] = dir +File.separator+ child + GribIndexName.currentSuffix;
          File gbx = new File( args[1] );
          if(  gbx.exists() )
            gbx.delete();
          Grib1WriteIndex.main(args);
        }
      }
    }
    long total = System.currentTimeMillis() - start;
    System.out.println("Total time = " + total + " msec");
    //System.out.println("Avg time = " + total / count + " msec");
  }

  /**
   * main.
   *
   * @param args dir to index  type number and dir to index
   * @throws IOException on io error
   */
  // process command line switches
  static public void main(String[] args) throws IOException {
    TestGribWriteIndex tg2wi = new TestGribWriteIndex();
    // String type(binary|text), int numberOfIndexes, String DirToIndex
    for( int rafBufSize = 256; rafBufSize < 8192; rafBufSize += 256 ) {
      Grib2WriteIndex.indexRafBufferSize = rafBufSize;
      //Grib2WriteIndex.indexRafBufferSize = 8092;
      System.out.println("Setting indexRafBufferSize ="+ rafBufSize );
      tg2wi.indexer( args[0] );
      //break;
    }  
  }
}
