/*
 * $Id:Cinrad2VolumeScan.java 63 2006-07-12 21:50:51Z edavis $
 *
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


package ucar.nc2.iosp.cinrad;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.nexrad2.NexradStationDB;

import java.io.*;
import java.util.*;

import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.unidata.io.bzip2.BZip2ReadException;
//import org.apache.tools.bzip2.CBZip2InputStream;


/**
 * This class reads a CINRAD level II data file.
 * It can handle NCDC archives (ARCHIVE2), as well as CRAFT/IDD compressed files (AR2V0001).
 * <p/>
 * Adapted with permission from the Java Iras software developed by David Priegnitz at NSSL.<p>
 * <p/>
 * Documentation on Archive Level II data format can be found at:
 * <a href="http://www.ncdc.noaa.gov/oa/radar/leveliidoc.html">
 * http://www.ncdc.noaa.gov/oa/radar/leveliidoc.html</a>
 *
 * @author caron
 * @author David Priegnitz
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class Cinrad2VolumeScan {

  // data formats
  static public final String ARCHIVE2 = "ARCHIVE2";
  static public final String AR2V0001 = "AR2V0001";

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Cinrad2VolumeScan.class);
  ////////////////////////////////////////////////////////////////////////////////////

  // Data file
  RandomAccessFile raf;

  private String dataFormat = null; // ARCHIVE2 or AR2V0001
  private String volumeNo = null;  // volume number (1 to 999)
  private int title_julianDay; // days since 1/1/70
  private int title_msecs; // milliseconds since midnight
  private String stationId; // 4 letter station assigned by ICAO
  private NexradStationDB.Station station; // from lookup table, may be null
  private Cinrad2Record first, last;

  private int vcp = 0; // Volume coverage pattern
  private int max_radials = 0;
  private int min_radials = Integer.MAX_VALUE;
  private int dopplarResolution;
  private boolean hasDifferentDopplarResolutions;

  // List of List of Cinrad2Record
  private ArrayList reflectivityGroups, dopplerGroups;

  private boolean showMessages = false, showData = false, debugScans = false, debugGroups2 = false, debugRadials = false;

  Cinrad2VolumeScan(RandomAccessFile orgRaf, CancelTask cancelTask) throws IOException {
    this.raf = orgRaf;

    boolean debug = log.isDebugEnabled();
    if (debug)
      log.debug("Cinrad2VolumeScan on " + raf.getLocation());

    raf.seek(0);
    raf.order(RandomAccessFile.LITTLE_ENDIAN); //.BIG_ENDIAN);
     // try to get it from the filename LOOK
    String loc = raf.getLocation();
    stationId = getStationID(loc);
    // volume scan header
    dataFormat = raf.readString(8);
    raf.skipBytes(1);
    volumeNo = raf.readString(3);
    title_julianDay = raf.readInt(); // since 1/1/70
    title_msecs = raf.readInt();
    //stationId = raf.readString(4).trim(); // only in AR2V0001
    if (debug) log.debug(" dataFormat= " + dataFormat + " stationId= " + stationId);

    if (stationId.length() == 0) {
      // try to get it from the filename LOOK
      stationId = null;
    }

    // try to find the station
    if (stationId != null) {
      station = NexradStationDB.get("K"+ stationId);
      dataFormat = "CINRAD-SA";
    }

   //   if(station == null) {
  //        station = new NexradStationDB.Station();
 //     stationId = "CHGZ";
  //    station.id = "CHGZ";
  //    station.name = "CHINA, GuanZhou";
  //    station.lat = parseDegree("23:0:14");
  //    station.lon = parseDegree("113:21:18");
   //   station.elev = Double.parseDouble("180.3");
  //    dataFormat = "CINRAD-SA";
  //    }

    //see if we have to uncompress
    if (dataFormat.equals(AR2V0001)) {
      raf.skipBytes(4);
      String BZ = raf.readString(2);
      if (BZ.equals("BZ")) {
        RandomAccessFile uraf = null;
        File uncompressedFile = DiskCache.getFileStandardPolicy(raf.getLocation() + ".uncompress");
        if (uncompressedFile.exists()) {
          uraf = new ucar.unidata.io.RandomAccessFile(uncompressedFile.getPath(), "r");
        } else {
          // nope, gotta uncompress it
          uraf = uncompress(raf, uncompressedFile.getPath(), debug);
          uraf.flush();
          if (debug) log.debug("flushed uncompressed file= " + uncompressedFile.getPath());
        }
        // switch to uncompressed file
        raf.close();
        raf = uraf;
        raf.order(RandomAccessFile.BIG_ENDIAN);
      }

      raf.seek(Cinrad2Record.FILE_HEADER_SIZE);
    }

    ArrayList reflectivity = new ArrayList();
    ArrayList doppler = new ArrayList();

    int recno = 0;
    while (true) {

      Cinrad2Record r = Cinrad2Record.factory(raf, recno++);
      if (r == null) break;

      // skip non-data messages
      if (r.message_type != 1) {
        if (showMessages) r.dumpMessage(System.out, null);
        continue;
      }

      if (showData) r.dump2(System.out);

      /* skip bad
      if (!r.checkOk()) {
        r.dump(System.out);
        continue;
      } */

      // some global params
      if (vcp == 0) vcp = r.vcp;
      if (first == null) first = r;
      last = r;

      if (!r.checkOk()) {
        continue;
      }

      if (r.hasReflectData)
        reflectivity.add(r);
      if (r.hasDopplerData)
        doppler.add(r);

      if ((cancelTask != null) && cancelTask.isCancel()) return;
    }
    if (debugRadials) System.out.println(" reflect ok= " + reflectivity.size() + " doppler ok= " + doppler.size());

    reflectivityGroups = sortScans("reflect", reflectivity);
    dopplerGroups = sortScans("doppler", doppler);
  }
    public String getStationID( String location) {
       String stationID;
       // posFirst: last '/' if it exists
       int posFirst = location.lastIndexOf('/') + 1;
       if (posFirst < 0) posFirst = 0;
       stationID = location.substring(posFirst,posFirst+4);
       return  stationID;
     }

    private static double parseDegree( String s) {
      StringTokenizer stoke = new StringTokenizer(s, ":");
      String degS = stoke.nextToken();
      String minS = stoke.nextToken();
      String secS = stoke.nextToken();

      try {
        double deg = Double.parseDouble( degS);
        double min = Double.parseDouble( minS);
        double sec = Double.parseDouble( secS);
        if (deg < 0)
          return deg - min/60 - sec/3600;
        else
          return deg + min/60 + sec/3600;
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
      return 0.0;
    }
  private ArrayList sortScans(String name, List scans) {

    // now group by elevation_num
    HashMap groupHash = new HashMap(600);
    for (int i = 0; i < scans.size(); i++) {
      Cinrad2Record record = (Cinrad2Record) scans.get(i);
      Integer groupNo = new Integer(record.elevation_num);

      ArrayList group = (ArrayList) groupHash.get(groupNo);
      if (null == group) {
        group = new ArrayList();
        groupHash.put(groupNo, group);
      }

      group.add(record);
    }

    // sort the groups by elevation_num
    ArrayList groups = new ArrayList(groupHash.values());
    Collections.sort(groups, new GroupComparator());

    // use the maximum radials
    for (int i = 0; i < groups.size(); i++) {
      ArrayList group = (ArrayList) groups.get(i);
      testScan(name, group);
      max_radials = Math.max(max_radials, group.size());
      min_radials = Math.min(min_radials, group.size());
    }
    if (debugRadials) {
      System.out.println(name + " min_radials= " + min_radials + " max_radials= " + max_radials);
      for (int i = 0; i < groups.size(); i++) {
        ArrayList group = (ArrayList) groups.get(i);
        Cinrad2Record lastr = (Cinrad2Record) group.get(0);
        for (int j = 1; j < group.size(); j++) {
          Cinrad2Record r = (Cinrad2Record) group.get(j);
          if (r.data_msecs < lastr.data_msecs)
            System.out.println(" out of order " + j);
          lastr = r;
        }
      }
    }

    testVariable(name, groups);
    if (debugScans) System.out.println("-----------------------------");

    return groups;
  }

  public int getMaxRadials() {
    return max_radials;
  }

  public int getMinRadials() {
    return min_radials;
  }

  public int getDopplarResolution() {
    return dopplarResolution;
  }

  public boolean hasDifferentDopplarResolutions() {
    return hasDifferentDopplarResolutions;
  }

  // do we have same characteristics for all records in a scan?
  private int MAX_RADIAL = 401;
  private int[] radial = new int[MAX_RADIAL];

  private boolean testScan(String name, ArrayList group) {
    int datatype = name.equals("reflect") ? Cinrad2Record.REFLECTIVITY : Cinrad2Record.VELOCITY_HI;
    Cinrad2Record first = (Cinrad2Record) group.get(0);

    int n = group.size();
    if (debugScans) {
      boolean hasBoth = first.hasDopplerData && first.hasReflectData;
      System.out.println(name + " " + first + " has " + n + " radials resolution= " + first.resolution + " has both = " + hasBoth);
    }

    boolean ok = true;
    double sum = 0.0;
    double sum2 = 0.0;

    for (int i = 0; i < MAX_RADIAL; i++)
      radial[i] = 0;

    for (int i = 0; i < group.size(); i++) {
      Cinrad2Record r = (Cinrad2Record) group.get(i);

      /* this appears to be common - seems to be ok, we put missing values in 
      if (r.getGateCount(datatype) != first.getGateCount(datatype)) {
        log.error(raf.getLocation()+" different number of gates ("+r.getGateCount(datatype)+
                "!="+first.getGateCount(datatype)+") in record "+name+ " "+r);
        ok = false;
      } */

      if (r.getGateSize(datatype) != first.getGateSize(datatype)) {
        log.warn(raf.getLocation() + " different gate size (" + r.getGateSize(datatype) + ") in record " + name + " " + r);
        ok = false;
      }
      if (r.getGateStart(datatype) != first.getGateStart(datatype)) {
        log.warn(raf.getLocation() + " different gate start (" + r.getGateStart(datatype) + ") in record " + name + " " + r);
        ok = false;
      }
      if (r.resolution != first.resolution) {
        log.warn(raf.getLocation() + " different resolution (" + r.resolution + ") in record " + name + " " + r);
        ok = false;
      }

      if ((r.radial_num < 0) || (r.radial_num >= MAX_RADIAL)) {
        log.info(raf.getLocation() + " radial out of range= " + r.radial_num + " in record " + name + " " + r);
        continue;
      }
      if (radial[r.radial_num] > 0) {
        log.warn(raf.getLocation() + " duplicate radial = " + r.radial_num + " in record " + name + " " + r);
        ok = false;
      }
      radial[r.radial_num] = r.recno + 1;

      sum += r.getElevation();
      sum2 += r.getElevation() * r.getElevation();
      // System.out.println("  elev="+r.getElevation()+" azi="+r.getAzimuth());
    }

    for (int i = 1; i < radial.length; i++) {
      if (0 == radial[i]) {
        if (n != (i - 1)) {
          log.warn(" missing radial(s)");
          ok = false;
        }
        break;
      }
    }

    double avg = sum / n;
    double sd = Math.sqrt((n * sum2 - sum * sum) / (n * (n - 1)));
    // System.out.println(" avg elev="+avg+" std.dev="+sd);

    return ok;
  }

  // do we have same characteristics for all groups in a variable?
  private boolean testVariable(String name, List scans) {
    int datatype = name.equals("reflect") ? Cinrad2Record.REFLECTIVITY : Cinrad2Record.VELOCITY_HI;
    if (scans.size() == 0) {
      log.warn(" No data for = " + name);
      return false;
    }

    boolean ok = true;
    List firstScan = (List) scans.get(0);
    Cinrad2Record firstRecord = (Cinrad2Record) firstScan.get(0);
    dopplarResolution = firstRecord.resolution;

    if (debugGroups2)
      System.out.println("Group " + Cinrad2Record.getDatatypeName(datatype) + " ngates = " + firstRecord.getGateCount(datatype) +
              " start = " + firstRecord.getGateStart(datatype) + " size = " + firstRecord.getGateSize(datatype));

    for (int i = 1; i < scans.size(); i++) {
      List scan = (List) scans.get(i);
      Cinrad2Record record = (Cinrad2Record) scan.get(0);

      if ((datatype == Cinrad2Record.VELOCITY_HI) && (record.resolution != firstRecord.resolution))
      { // do all velocity resolutions match ??
        log.warn(name + " scan " + i + " diff resolutions = " + record.resolution + ", " + firstRecord.resolution +
                " elev= " + record.elevation_num + " " + record.getElevation());
        ok = false;
        hasDifferentDopplarResolutions = true;
      }

      if (record.getGateSize(datatype) != firstRecord.getGateSize(datatype)) {
        log.warn(name + " scan " + i + " diff gates size = " + record.getGateSize(datatype) + " " + firstRecord.getGateSize(datatype) +
                " elev= " + record.elevation_num + " " + record.getElevation());
        ok = false;

      } else if (debugGroups2)
        System.out.println(" ok gates size elev= " + record.elevation_num + " " + record.getElevation());

      if (record.getGateStart(datatype) != firstRecord.getGateStart(datatype)) {
        log.warn(name + " scan " + i + " diff gates start = " + record.getGateStart(datatype) + " " + firstRecord.getGateStart(datatype) +
                " elev= " + record.elevation_num + " " + record.getElevation());
        ok = false;

      } else if (debugGroups2)
        System.out.println(" ok gates start elev= " + record.elevation_num + " " + record.getElevation());
    }

    return ok;
  }

  /**
   * Get Reflectivity Groups
   * Groups are all the records for a variable and elevation_num;
   *
   * @return List of type List of type Cinrad2Record
   */
  public List getReflectivityGroups() {
    return reflectivityGroups;
  }

  /**
   * Get Velocity Groups
   * Groups are all the records for a variable and elevation_num;
   *
   * @return List of type List of type Cinrad2Record
   */
  public List getVelocityGroups() {
    return dopplerGroups;
  }

  private class GroupComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      List group1 = (List) o1;
      List group2 = (List) o2;
      Cinrad2Record record1 = (Cinrad2Record) group1.get(0);
      Cinrad2Record record2 = (Cinrad2Record) group2.get(0);

      //if (record1.elevation_num != record2.elevation_num)
      return record1.elevation_num - record2.elevation_num;
      //return record1.cut - record2.cut;
    }
  }

  /**
   * Get data format (ARCHIVE2, AR2V0001) for this file.
   */
  public String getDataFormat() {
    return dataFormat;
  }

  /**
   * Get the starting Julian day for this volume
   *
   * @return days since 1/1/70.
   */
  public int getTitleJulianDays() {
    return title_julianDay;
  }

  /**
   * Get the starting time in seconds since midnight.
   *
   * @return Generation time of data in milliseconds of day past  midnight (UTC).
   */
  public int getTitleMsecs() {
    return title_msecs;
  }

  /**
   * Get the Volume Coverage Pattern number for this data.
   *
   * @return VCP
   * @see Cinrad2Record#getVolumeCoveragePatternName
   */
  public int getVCP() {
    return vcp;
  }

  /**
   * Get the 4-char station ID for this data
   *
   * @return station ID (may be null)
   */

  public String getStationId() {
    return stationId;
  }

  public String getStationName() {
    return station == null ? "unknown" : station.name;
  }

  public double getStationLatitude() {
    return station == null ? 0.0 : station.lat;
  }

  public double getStationLongitude() {
    return station == null ? 0.0 : station.lon;
  }

  public double getStationElevation() {
    return station == null ? 0.0 : station.elev;
  }

  public Date getStartDate() {
    return first.getDate();
  }

  public Date getEndDate() {
    return last.getDate();
  }

  /**
   * Write equivilent uncompressed version of the file.
   *
   * @param raf2      file to uncompress
   * @param ufilename write to this file
   * @return raf of uncompressed file
   * @throws IOException
   */
  private RandomAccessFile uncompress(RandomAccessFile raf2, String ufilename, boolean debug) throws IOException {
    raf2.seek(0);
    byte[] header = new byte[Cinrad2Record.FILE_HEADER_SIZE];
    raf2.read(header);
    RandomAccessFile dout2 = new RandomAccessFile(ufilename, "rw");
    dout2.write(header);

    boolean eof = false;
    int numCompBytes;
    byte[] ubuff = new byte[40000];
    byte[] obuff = new byte[40000];
    try {
      CBZip2InputStream cbzip2 = new CBZip2InputStream();
      while (!eof) {

        try {
          numCompBytes = raf2.readInt();
          if (numCompBytes == -1) {
            if (debug) log.debug("  done: numCompBytes=-1 ");
            break;
          }
        } catch (EOFException ee) {
          if (debug) log.debug("  got EOFException ");
          break; // assume this is ok
        }

        if (debug) {
          log.debug("reading compressed bytes " + numCompBytes + " input starts at " + raf2.getFilePointer() + "; output starts at " + dout2.getFilePointer());
        }
        /*
        * For some stupid reason, the last block seems to
        * have the number of bytes negated.  So, we just
        * assume that any negative number (other than -1)
        * is the last block and go on our merry little way.
        */
        if (numCompBytes < 0) {
          if (debug) log.debug("last block?" + numCompBytes);
          numCompBytes = -numCompBytes;
          eof = true;
        }
        byte[] buf = new byte[numCompBytes];
        raf2.readFully(buf);
        ByteArrayInputStream bis = new ByteArrayInputStream(buf, 2, numCompBytes - 2);

        //CBZip2InputStream cbzip2 = new CBZip2InputStream(bis);
        cbzip2.setStream(bis);
        int total = 0;
        int nread;
        /*
        while ((nread = cbzip2.read(ubuff)) != -1) {
          dout2.write(ubuff, 0, nread);
          total += nread;
        }
        */
        try {
          while ((nread = cbzip2.read(ubuff)) != -1) {
            if (total + nread > obuff.length) {
              byte[] temp = obuff;
              obuff = new byte[temp.length * 2];
              System.arraycopy(temp, 0, obuff, 0, temp.length);
            }
            System.arraycopy(ubuff, 0, obuff, total, nread);
            total += nread;
          }
          if (obuff.length >= 0) dout2.write(obuff, 0, total);
        } catch (BZip2ReadException ioe) {
          log.debug("Cinrad2IOSP.uncompress ", ioe);
        }
        float nrecords = (float) (total / 2432.0);
        if (debug)
          log.debug("  unpacked " + total + " num bytes " + nrecords + " records; ouput ends at " + dout2.getFilePointer());
      }
    } catch (EOFException e) {
      e.printStackTrace();
    }

    dout2.flush();
    return dout2;
  }

// debugging

  static void bdiff(String filename) throws IOException {

    InputStream in1 = new FileInputStream(filename + ".tmp");
    InputStream in2 = new FileInputStream(filename + ".tmp2");

    int count = 0;
    int bad = 0;
    while (true) {
      int b1 = in1.read();
      int b2 = in2.read();
      if (b1 < 0) break;
      if (b2 < 0) break;

      if (b1 != b2) {
        System.out.println(count + " in1=" + b1 + " in2= " + b2);
        bad++;
        if (bad > 130) break;
      }
      count++;
    }
    System.out.println("total read = " + count);
  }

  // check if compressed file seems ok
  static public long testValid(String ufilename) throws IOException {
    boolean lookForHeader = false;

    // gotta make it
    RandomAccessFile raf = new RandomAccessFile(ufilename, "r");
    raf.order(RandomAccessFile.LITTLE_ENDIAN); //.BIG_ENDIAN);
    raf.seek(0);
    byte[] b = new byte[8];
    raf.read(b);
    String test = new String(b);
    if (test.equals(Cinrad2VolumeScan.ARCHIVE2) || test.equals(Cinrad2VolumeScan.AR2V0001)) {
      System.out.println("--Good header= " + test);
      raf.seek(24);
    } else {
      System.out.println("--No header ");
      lookForHeader = true;
      raf.seek(0);
    }

    boolean eof = false;
    int numCompBytes;
    try {

      while (!eof) {

        if (lookForHeader) {
          raf.read(b);
          test = new String(b);
          if (test.equals(Cinrad2VolumeScan.ARCHIVE2) || test.equals(Cinrad2VolumeScan.AR2V0001)) {
            System.out.println("  found header= " + test);
            raf.skipBytes(16);
            lookForHeader = false;
          } else {
            raf.skipBytes(-8);
          }
        }

        try {
          numCompBytes = raf.readInt();
          if (numCompBytes == -1) {
            System.out.println("\n--done: numCompBytes=-1 ");
            break;
          }
        } catch (EOFException ee) {
          System.out.println("\n--got EOFException ");
          break; // assume this is ok
        }

        System.out.print(" " + numCompBytes + ",");
        if (numCompBytes < 0) {
          System.out.println("\n--last block " + numCompBytes);
          numCompBytes = -numCompBytes;
          if (!lookForHeader) eof = true;
        }

        raf.skipBytes(numCompBytes);
      }
    } catch (EOFException e) {
      e.printStackTrace();
    }

    return raf.getFilePointer();
  }

  /**
   * test
   */
  public static void main2(String[] args) throws IOException {
    File testDir = new File("C:/data/bad/radar2/");

    File[] files = testDir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (!file.getPath().endsWith(".ar2v")) continue;
      System.out.println(file.getPath() + " " + file.length());
      long pos = testValid(file.getPath());
      if (pos == file.length()) {
        System.out.println("OK");
        try {
          NetcdfFile.open(file.getPath());
        } catch (Throwable t) {
          System.out.println("ERROR=  " + t);
        }
      } else
        System.out.println("NOT pos=" + pos);

      System.out.println();
    }
  }

  public static void main(String args[]) throws IOException {
    NexradStationDB.init();

    RandomAccessFile raf = new RandomAccessFile("R:/testdata/radar/nexrad/Cinrad2/problem/KCCX_20060627_1701", "r");
    new Cinrad2VolumeScan(raf, null);
  }

}
