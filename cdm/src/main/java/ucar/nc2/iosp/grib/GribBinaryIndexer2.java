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
 * Date: May 6, 2009
 * Time: 2:44:44 PM
 */

package ucar.nc2.iosp.grib;

import ucar.grib.GribChecker;
import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grib.grib1.Grib1WriteIndex;
//import ucar.nc2.dt.fmrc.ForecastModelRunInventory;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.*;

public class GribBinaryIndexer2 {

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
        //System.out.println( "children i ="+ children[ i ]);
        File child = new File(dir, aChildren);
        //System.out.println( "child ="+ child.getName() );
        if (child.isDirectory()) {
          checkDirs(child);
          // skip index *gbx and inventory *xml files
        } else if (aChildren.endsWith("gbx") ||
            aChildren.endsWith("xml") ||
            aChildren.endsWith("gbx2") ||
            aChildren.endsWith("tmp") || //index in creation process
            aChildren.length() == 0) { // zero length file, ugh...
        } else {
          checkIndex(dir, child);
        }
      }
    } else {
    }

  }

  /*
  * checks the status of index files
  *
  */
  private void checkIndex(File dir, File grib)
      throws IOException {

    String[] args = new String[2];
    File gbx = new File(dir, grib.getName() + ".gbx2");
    //System.out.println( "index ="+ gbx.getName() );
    if (removeGBX && gbx.exists())
        gbx.delete();

    args[0] = grib.getParent() + "/" + grib.getName();
    args[1] = grib.getParent() + "/" + gbx.getName();
    //System.out.println( "args ="+ args[ 0] +" "+ args[ 1 ] );
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
      //System.out.println("Grib "+ args[ 0 ] );
      int result = GribChecker.getEdition(raf);
      if (result == 2) {
        //System.out.println("Valid Grib Edition 2 File");
        grib2check(grib, gbx, args);
      } else if (result == 1) {
        //System.out.println("Valid Grib Edition 1 File");
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
        // gbx older than grib, no need to check
        if (grib.lastModified() < gbx.lastModified())
          return;
        System.out.println("IndexExtending " + grib.getName() + " " +
            Calendar.getInstance().getTime().toString());
        // grib, gbx, gribName, gbxName, false(make index)
        new Grib1WriteIndex().extendGribIndex(grib, gbx, args[0], args[1], false);
        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      } else {  // create index
        System.out.println("Indexing " + grib.getName() + " " +
            Calendar.getInstance().getTime().toString());
        // grib, gribName, gbxName, false(make index)
        new Grib1WriteIndex().writeGribIndex(grib, args[0], args[1], false);
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
//          File invFile = new File( args[ 0 ] +".fmrInv.xml");
//          // sometimes an index without inventory file
//          if( invFile.exists() )
//            return;
          //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
          return;
        }
        System.out.println("IndexExtending " + grib.getName() + " " +
            Calendar.getInstance().getTime().toString());
        // grib, gbx, gribName, gbxName, false(make index)
        new Grib2WriteIndex().extendGribIndex(grib, gbx, args[0], args[1], false);

        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      } else {  // create index
        System.out.println("Indexing " + grib.getName() + " " +
            Calendar.getInstance().getTime().toString());
        // grib, gribName, gbxName, false(make index)
        new Grib2WriteIndex().writeGribIndex(
            grib, args[0], args[1], false);
        //ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Caught Exception doing index or inventory for " + grib.getName());
    }

  }

  static public boolean test() throws IOException {
    GribBinaryIndexer2 gi2 = new GribBinaryIndexer2();
    String[] args = new String[ 2 ];
    args[ 0 ] = "C:/data/gefs/GFS_Global_1p0deg_Ensemble_20090303_0000.grib2";
    args[ 1 ] = args[ 0 ] +".gbx";
    File grib = new File( args[ 0 ]);
    File gbx = new File( args[ 1 ]);
    gi2.grib2check( grib, gbx, args);
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

    GribBinaryIndexer2 gi2 = new GribBinaryIndexer2();

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
      gi2.readConf(arg);
    }
    if (clear) {
      gi2.clearLocks();
      return;
    }
    // Grib Index files in dirs
    gi2.indexer();

  }

}
