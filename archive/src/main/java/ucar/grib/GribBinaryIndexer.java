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
 *
 * By:   Robb Kambic
 * Date: Feb 15, 2009
 * Time: 11:21:10 AM
 *
 */

package ucar.grib;

import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grib.grib1.Grib1WriteIndex;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.*;

// Purpose: 	walks directory structure making Grib Indexes as needed.
//            Uses a configuration file to designate which dirs to index
//

public final class GribBinaryIndexer {

  /**
   * delete all indexes, it makes a complete rebuild
   */
  private static boolean removeGBX = false;

  /*
  * dirs to inspect
  */
  private List<String> dirs = new ArrayList<String>();

  /*
  * reads in the configuration file
  *
  */
  private boolean readConf(String conf) throws IOException {

    InputStream ios = new FileInputStream(conf);
    BufferedReader dataIS =
        new BufferedReader(new InputStreamReader(ios));

    while (true) {
      String line = dataIS.readLine();
      if (line == null) {
        break;
      }
      if (line.startsWith("#")) {
        continue;
      }
      dirs.add(line);
      //System.out.println( line );
    }
    ios.close();
    return true;
  }


  /*
  * clears all IndexLock in the directories
  *
  */
  private void clearLocks() {

    for (String dir : dirs) {
      File f = new File(dir + "/IndexLock");
      if (f.exists()) {
        f.delete();
        System.out.println("Cleared lock " + dir + "/IndexLock");
      } else {
        System.out.println("In directory " + dir);
      }
    }
  }

  /*
  * walks the directory trees setting IndexLocks
  *
  */
  private void indexer() throws IOException {

    System.out.println("Start " + Calendar.getInstance().getTime().toString());
    long start = System.currentTimeMillis();
    for (String dir : dirs) {
      File d = new File(dir);
      if (!d.exists()) {
        System.out.println("Dir " + dir + " doesn't exists");
        continue;
      }
      File dl = new File(dir + "/IndexLock");
      if (dl.exists()) {
        System.out.println("Exiting " + dir + " another Indexer working here");
        continue;
      }
      //System.out.println( "In directory "+ dir );
      dl.createNewFile(); // create a lock while indexing dir

      checkDirs(d);

      dl.delete();  // delete lock when done
    }
    System.out.println("End " + Calendar.getInstance().getTime().toString());
    System.out.println("Total time in ms " + (System.currentTimeMillis() - start ));
  }

  /*
  * checkDirs is a recursive routine used to walk the directory tree in a
  * depth first search checking the index of GRIB files .
  */
  private void checkDirs(File dir) throws IOException {

    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String aChildren : children) {
        if (aChildren.equals("IndexLock"))
          continue;
        File child = new File(dir, aChildren);
        if (child.isDirectory()) {
          checkDirs(child);
          // skip index *gbx and inventory *xml files
        } else if (aChildren.endsWith(GribIndexName.oldSuffix ) ||
            aChildren.endsWith(GribIndexName.currentSuffix) ||
            aChildren.endsWith("xml") ||
            aChildren.endsWith("tmp") || //index in creation process
            aChildren.length() == 0) { // zero length file, ugh...
        } else {
          checkIndex(dir, child);
        }
      }
    }
  }

  /*
  * checks the status of index files
  *
  */
  private void checkIndex(File dir, File grib)
      throws IOException {

    String[] args = new String[2];
    File gbx = new File( GribIndexName.getCurrentSuffix( grib.getPath() ));
    if (removeGBX && gbx.exists())
        gbx.delete();
    args[0] = grib.getPath();
    args[1] = gbx.getPath();
    if (gbx.exists()) {
      // skip files older than 3 hours
      if ((System.currentTimeMillis() - grib.lastModified()) > 10800000)
        return;
      // skip indexes that have a length of 0, most likely there is a index problem
      if (gbx.length() == 0) {
        System.out.println("ERROR " + args[1] + " has length zero");
        return;
      }
    }

    if (grib.getName().endsWith("grib1")) {
      grib1check(grib, gbx, args);
    } else if (grib.getName().endsWith("grib2")) {
      grib2check(grib, gbx, args);
    } else { // else check file for Grib version
      ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(args[0], "r");
      int result = GribChecker.getEdition(raf);
      if (result == 2) {
        grib2check(grib, gbx, args);
      } else if (result == 1) {
        grib1check(grib, gbx, args);
      } else {
        System.out.println("Not a Grib File " + args[0]);
      }
      raf.close();
    }
  }

  /*
  * indexes or extends Grib1 files plus creates inventories files
  *
  */
  private void grib1check(File grib, File gbx, String[] args) {
    // args 0  grib name, args 1 grib index name

    try {
      if (gbx.exists()) {
        // gbx older then grib
        if (grib.lastModified() < gbx.lastModified())
          return;
        // grib, gbx, gribName, gbxName, false(make index)
        long start = System.currentTimeMillis();
        new Grib1WriteIndex().extendGribIndex(grib, gbx, args[0], args[1], false);
        System.out.println("IndexExtending " + grib.getName() +" took "+
        (System.currentTimeMillis() - start) +" ms BufferSize "+ Grib2WriteIndex.indexRafBufferSize);
        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      } else {  // create index
        // grib, gribName, gbxName, false(make index)
        long start = System.currentTimeMillis();
        new Grib1WriteIndex().writeGribIndex(grib, args[0], args[1], false);
        System.out.println("Indexing " + grib.getName() +" took "+
        (System.currentTimeMillis() - start) +" ms BufferSize "+ Grib2WriteIndex.indexRafBufferSize);
        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Caught Exception doing index or inventory for " + grib.getName());
    }
  }

  /*
  * indexes or extends Grib2 files plus creates inventories files
  *
  */
  private void grib2check(File grib, File gbx, String[] args) {

    try {
      if (gbx.exists()) {
        // gbx older than grib, no need to check
        if (grib.lastModified() < gbx.lastModified()) {
          return;
        }
        // grib, gbx, gribName, gbxName, false(make index)
        long start = System.currentTimeMillis();
        new Grib2WriteIndex().extendGribIndex(grib, gbx, args[0], args[1], false);
        System.out.println("IndexExtending " + grib.getName() +" took "+
        (System.currentTimeMillis() - start) +" ms BufferSize "+ Grib2WriteIndex.indexRafBufferSize);
        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      } else {  // create index
        // grib, gribName, gbxName, false(make index)
        long start = System.currentTimeMillis();
        new Grib2WriteIndex().writeGribIndex(grib, args[0], args[1], false);
        System.out.println("Indexing " + grib.getName() +" took "+
        (System.currentTimeMillis() - start) +" ms BufferSize "+ Grib2WriteIndex.indexRafBufferSize);
        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Caught Exception doing index or inventory for " + grib.getName());
    }

  }

  static public boolean test() throws IOException {
    GribBinaryIndexer gi = new GribBinaryIndexer();
    String[] args = new String[ 2 ];
    args[ 0 ] = "C:/data/grib/g2p5U.grib2";
    args[ 1 ] = GribIndexName.getCurrentSuffix( args[ 0 ] );
    File grib = new File( args[ 0 ]);
    File gbx = new File( args[ 1 ]);
    gi.grib2check( grib, gbx, args);
    return true;
  }

  /**
   * main.
   *
   * @param args can be clear and the GribIndexer.conf file
   * @throws IOException on io error
   */
  // process command line switches
  static public void main(String[] args) throws IOException {
    //if( test() )
    //  return;
    
    GribBinaryIndexer gbi = new GribBinaryIndexer();

    boolean clear = false;
    for (String arg : args) {
      if (arg.equals("clear")) {
        clear = true;
        System.out.println("Clearing Index locks");
        continue;
      } else if (arg.equals("remove")) {
        removeGBX = true;
        System.out.println("Removing all indexes");
        continue;
      }
      // else conf file
      File f = new File(arg);
      if (!f.exists()) {
        System.out.println("Conf file " + arg + " doesn't exist: ");
        return;
      }
      // read in conf file
      gbi.readConf(arg);
    }
    if (clear) {
      gbi.clearLocks();
      return;
    }
    // Grib Index files in dirs
    gbi.indexer();
  }
}
