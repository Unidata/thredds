/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.nexrad2;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;
import ucar.nc2.NetcdfFile;
import static ucar.nc2.iosp.nexrad2.Level2Record.REFLECTIVITY_HIGH;
import static ucar.nc2.iosp.nexrad2.Level2Record.VELOCITY_HIGH ;
import java.io.*;
import java.util.*;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.unidata.io.bzip2.BZip2ReadException;

/**
 * This class reads a NEXRAD level II data file.
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
 */
public class Level2VolumeScan {

  // data formats
  static public final String ARCHIVE2 = "ARCHIVE2";
  static public final String AR2V0001 = "AR2V0001";
  static public final String AR2V0002 = "AR2V0002";
  static public final String AR2V0003 = "AR2V0003";
  static public final String AR2V0004 = "AR2V0004";
  static public final String AR2V0006 = "AR2V0006";
  static public final String AR2V0007 = "AR2V0007";

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Level2VolumeScan.class);
  ////////////////////////////////////////////////////////////////////////////////////

  // Data file
  RandomAccessFile raf;

  private String dataFormat = null; // ARCHIVE2 or AR2V0001
  private int title_julianDay; // days since 1/1/70
  private int title_msecs; // milliseconds since midnight
  private String stationId; // 4 letter station assigned by ICAO
  private NexradStationDB.Station station; // from lookup table, may be null
  private Level2Record first, last;

  private int vcp = 0; // Volume coverage pattern
  private int max_radials = 0;
  private int min_radials = Integer.MAX_VALUE;
  private int max_radials_hr = 0;
  private int min_radials_hr = Integer.MAX_VALUE;
  private int dopplarResolution;
  private boolean hasDifferentDopplarResolutions;
  private boolean hasHighResolutionData;

  private boolean hasHighResolutionREF;
  private boolean hasHighResolutionVEL;
  private boolean hasHighResolutionSW;
  private boolean hasHighResolutionZDR;
  private boolean hasHighResolutionPHI;
  private boolean hasHighResolutionRHO;

  private List<List<Level2Record>> reflectivityGroups, dopplerGroups;
  private List<List<Level2Record>> reflectivityHighResGroups;
  private List<List<Level2Record>> velocityHighResGroups;
  private List<List<Level2Record>> spectrumHighResGroups;

  private List<List<Level2Record>> diffReflectHighResGroups;
  private List<List<Level2Record>> diffPhaseHighResGroups;
  private List<List<Level2Record>> coefficientHighResGroups;

  private boolean showMessages = false, showData = false, debugScans = false, debugGroups2 = false, debugRadials = false;
  private boolean runCheck = false;

  Level2VolumeScan(RandomAccessFile orgRaf, CancelTask cancelTask) throws IOException {
    this.raf = orgRaf;

    if (log.isDebugEnabled())
      log.debug("Level2VolumeScan on " + raf.getLocation());

    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);

    // volume scan header
    dataFormat = raf.readString(8);
    raf.skipBytes(4);
    title_julianDay = raf.readInt(); // since 1/1/70
    title_msecs = raf.readInt();
    stationId = raf.readString(4).trim(); // only in AR2V0001
    if (log.isDebugEnabled()) log.debug(" dataFormat= " + dataFormat + " stationId= " + stationId);

    if (stationId.length() == 0) {
      // try to get it from the filename LOOK

      stationId = null;
    }

    // try to find the station
    if (stationId != null) {
      if (!stationId.startsWith("K") && stationId.length() == 4) {
        String _stationId = "K" + stationId;
        station = NexradStationDB.get(_stationId);
      } else
        station = NexradStationDB.get(stationId);
    }

    //see if we have to uncompress
     if (dataFormat.equals(AR2V0001) || dataFormat.equals(AR2V0003)
            || dataFormat.equals(AR2V0004) || dataFormat.equals(AR2V0006)  || dataFormat.equals(AR2V0007) ) {
      raf.skipBytes(4);
      String BZ = raf.readString(2);
      if (BZ.equals("BZ")) {
        RandomAccessFile uraf = null;
        File uncompressedFile = DiskCache.getFileStandardPolicy(raf.getLocation() + ".uncompress");

        if (uncompressedFile.exists() && uncompressedFile.length() > 0) {
          // see if its locked - another thread is writing it
          try (FileInputStream fstream = new FileInputStream(uncompressedFile)) {
            //lock = fstream.getChannel().lock(0, 1, true); // wait till its unlocked

            while (true) { // loop waiting for the lock
              try {
                fstream.getChannel().lock(0, 1, true); // wait till its unlocked
                break;

              } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
                try {
                  Thread.sleep(100); // msecs
                } catch (InterruptedException e1) {
                  break;
                }
              }
            }

          } // Lock is released when the corresponding channel is closed on exit of try()
          uraf = ucar.unidata.io.RandomAccessFile.acquire(uncompressedFile.getPath());

        } else {
          try {
            // nope, gotta uncompress it
            uraf = uncompress(raf, uncompressedFile.getPath());
            if (log.isDebugEnabled()) log.debug("made uncompressed file= " + uncompressedFile.getPath());
          } catch (Throwable t) {
            if (uraf != null) uraf.close();
            throw t;
          }
        }

        // switch to uncompressed file
        raf.close();
        raf = uraf;
        raf.order(RandomAccessFile.BIG_ENDIAN);
      }

      raf.seek(Level2Record.FILE_HEADER_SIZE);
    }

    List<Level2Record> reflectivity = new ArrayList<>();
    List<Level2Record> doppler = new ArrayList<>();
    List<Level2Record> highReflectivity = new ArrayList<>();
    List<Level2Record> highVelocity = new ArrayList<>();
    List<Level2Record> highSpectrum = new ArrayList<>();
    List<Level2Record> highDiffReflectivity = new ArrayList<>();
    List<Level2Record> highDiffPhase = new ArrayList<>();
    List<Level2Record> highCorreCoefficient = new ArrayList<>();

    long message_offset31 = 0;
    int recno = 0;
    while (true) {

      Level2Record r = Level2Record.factory(raf, recno++, message_offset31);
      if (r == null) break;
      if (showData) r.dump2(System.out);
      // skip non-data messages
      if (r.message_type == 31) {
        message_offset31 = message_offset31 + (r.message_size * 2 + 12 - 2432);
      }

      if (r.message_type != 1 && r.message_type != 31) {
        if (showMessages) r.dumpMessage(System.out);
        continue;
      }

      //  if (showData) r.dump2(System.out);

      /* skip bad
      if (!r.checkOk()) {
        r.dump(System.out);
        continue;
      }   */

      // some global params
      if (vcp == 0) vcp = r.vcp;
      if (first == null) first = r;
      last = r;

      if (runCheck && !r.checkOk()) {
        continue;
      }

      if (r.hasReflectData)
        reflectivity.add(r);
      if (r.hasDopplerData)
        doppler.add(r);


      if (r.message_type == 31) {
        if (r.hasHighResREFData)
          highReflectivity.add(r);
        if (r.hasHighResVELData)
          highVelocity.add(r);
        if (r.hasHighResSWData)
          highSpectrum.add(r);
        if (r.hasHighResZDRData)
          highDiffReflectivity.add(r);
        if (r.hasHighResPHIData)
          highDiffPhase.add(r);
        if (r.hasHighResRHOData)
          highCorreCoefficient.add(r);
      }

      if ((cancelTask != null) && cancelTask.isCancel()) return;
    }
    if (debugRadials) System.out.println(" reflect ok= " + reflectivity.size() + " doppler ok= " + doppler.size());
    if (highReflectivity.size() == 0) {
      reflectivityGroups = sortScans("reflect", reflectivity, 600);
      dopplerGroups = sortScans("doppler", doppler, 600);
    }
    if (highReflectivity.size() > 0)
      reflectivityHighResGroups = sortScans("reflect_HR", highReflectivity, 720);
    if (highVelocity.size() > 0)
      velocityHighResGroups = sortScans("velocity_HR", highVelocity, 720);
    if (highSpectrum.size() > 0)
      spectrumHighResGroups = sortScans("spectrum_HR", highSpectrum, 720);
    if (highDiffReflectivity.size() > 0)
      diffReflectHighResGroups = sortScans("diffReflect_HR", highDiffReflectivity, 720);
    if (highDiffPhase.size() > 0)
      diffPhaseHighResGroups = sortScans("diffPhase_HR", highDiffPhase, 720);
    if (highCorreCoefficient.size() > 0)
      coefficientHighResGroups = sortScans("coefficient_HR", highCorreCoefficient, 720);

  }

  private List<List<Level2Record>> sortScans(String name, List<Level2Record> scans, int siz) {

    // now group by elevation_num
    Map<Short, List<Level2Record>> groupHash = new HashMap<>(siz);
    for (Level2Record record : scans) {
      List<Level2Record> group = groupHash.get(record.elevation_num);
      if (null == group) {
        group = new ArrayList<>();
        groupHash.put(record.elevation_num, group);
      }
      group.add(record);
    }

    // sort the groups by elevation_num
    List<List<Level2Record>> groups = new ArrayList<>(groupHash.values());
    Collections.sort(groups, new GroupComparator());

    // use the maximum radials
    for (List<Level2Record> group: groups) {
      Level2Record r = group.get(0);
      if(runCheck) testScan(name, group);
      if(r.getGateCount(REFLECTIVITY_HIGH) > 500 || r.getGateCount(VELOCITY_HIGH) > 1000) {
          if(group.size() <= 360) {
              max_radials = Math.max(max_radials, group.size());
              min_radials = Math.min(min_radials, group.size());
          } else {
              max_radials_hr = Math.max(max_radials_hr, group.size());
              min_radials_hr = Math.min(min_radials_hr, group.size());
          }
      }
      else {
        max_radials = Math.max(max_radials, group.size());
        min_radials = Math.min(min_radials, group.size());
      }
    }

    if (debugRadials) {
      System.out.println(name + " min_radials= " + min_radials + " max_radials= " + max_radials);
      for (List<Level2Record> group: groups) {
        Level2Record lastr = group.get(0);
        for (int j = 1; j < group.size(); j++) {
          Level2Record r = group.get(j);
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

  public int getMaxRadials(int r) {
    if (r == 0)
      return max_radials;
    else if (r == 1)
      return max_radials_hr;
    else
      return 0;
  }

  public int getMinRadials(int r) {
    if (r == 0)
      return min_radials;
    else if (r == 1)
      return min_radials_hr;
    else
      return 0;
  }

  public int getDopplarResolution() {
    return dopplarResolution;
  }

  public boolean hasDifferentDopplarResolutions() {
    return hasDifferentDopplarResolutions;
  }

  public boolean hasHighResolutions(int dt) {
    if (dt == 0)
      return hasHighResolutionData;
    else if (dt == 1)
      return hasHighResolutionREF;
    else if (dt == 2)
      return hasHighResolutionVEL;
    else if (dt == 3)
      return hasHighResolutionSW;
    else if (dt == 4)
      return hasHighResolutionZDR;
    else if (dt == 5)
      return hasHighResolutionPHI;
    else if (dt == 6)
      return hasHighResolutionRHO;
    else
      return false;
  }

  // do we have same characteristics for all records in a scan?
  private int MAX_RADIAL = 721;
  private int[] radial = new int[MAX_RADIAL];

  private boolean testScan(String name, List<Level2Record> group) {
    int datatype = name.equals("reflect") ? Level2Record.REFLECTIVITY : Level2Record.VELOCITY_HI;
    Level2Record first = group.get(0);

    int n = group.size();
    if (debugScans) {
      boolean hasBoth = first.hasDopplerData && first.hasReflectData;
      System.out.println(name + " " + first + " has " + n + " radials resolution= " + first.resolution + " has both = " + hasBoth);
    }

    boolean ok = true;

    for (int i = 0; i < MAX_RADIAL; i++)
      radial[i] = 0;

    for (Level2Record r: group) {
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

    return ok;
  }

  // do we have same characteristics for all groups in a variable?
  private boolean testVariable(String name, List scans) {
    int datatype = name.equals("reflect") ? Level2Record.REFLECTIVITY : Level2Record.VELOCITY_HI;
    if (scans.size() == 0) {
      log.warn(" No data for = " + name);
      return false;
    }

    boolean ok = true;
    List firstScan = (List) scans.get(0);
    Level2Record firstRecord = (Level2Record) firstScan.get(0);
    dopplarResolution = firstRecord.resolution;

    if (debugGroups2)
      System.out.println("Group " + Level2Record.getDatatypeName(datatype) + " ngates = " + firstRecord.getGateCount(datatype) +
              " start = " + firstRecord.getGateStart(datatype) + " size = " + firstRecord.getGateSize(datatype));

    for (int i = 1; i < scans.size(); i++) {
      List scan = (List) scans.get(i);
      Level2Record record = (Level2Record) scan.get(0);

      if ((datatype == Level2Record.VELOCITY_HI) && (record.resolution != firstRecord.resolution)) { // do all velocity resolutions match ??
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

      if (record.message_type == 31) {
        hasHighResolutionData = true;
        //each data type
        if (record.hasHighResREFData)
          hasHighResolutionREF = true;
        if (record.hasHighResVELData)
          hasHighResolutionVEL = true;
        if (record.hasHighResSWData)
          hasHighResolutionSW = true;
        if (record.hasHighResZDRData)
          hasHighResolutionZDR = true;
        if (record.hasHighResPHIData)
          hasHighResolutionPHI = true;
        if (record.hasHighResRHOData)
          hasHighResolutionRHO = true;

      }
    }

    return ok;
  }

  /**
   * Get Reflectivity Groups
   * Groups are all the records for a variable and elevation_num;
   *
   * @return List of type List of type Level2Record
   */
  public List<List<Level2Record>> getReflectivityGroups() {
    return reflectivityGroups;
  }

  /**
   * Get Velocity Groups
   * Groups are all the records for a variable and elevation_num;
   *
   * @return List of type List of type Level2Record
   */
  public List<List<Level2Record>> getVelocityGroups() {
    return dopplerGroups;
  }

  public List<List<Level2Record>> getHighResVelocityGroups() {
    return velocityHighResGroups;
  }

  public List<List<Level2Record>> getHighResReflectivityGroups() {
    return reflectivityHighResGroups;
  }

  public List<List<Level2Record>> getHighResSpectrumGroups() {
    return spectrumHighResGroups;
  }

  public List<List<Level2Record>> getHighResDiffReflectGroups() {
    return diffReflectHighResGroups;
  }

  public List<List<Level2Record>> getHighResDiffPhaseGroups() {
    return diffPhaseHighResGroups;
  }

  public List<List<Level2Record>> getHighResCoeffocientGroups() {
    return coefficientHighResGroups;
  }

  private static class GroupComparator implements
    Comparator<List<Level2Record>> {

    public int compare(List<Level2Record> group1, List<Level2Record> group2) {
      Level2Record record1 = group1.get(0);
      Level2Record record2 = group2.get(0);

      //if (record1.elevation_num != record2.elevation_num)
      return record1.elevation_num - record2.elevation_num;
      //return record1.cut - record2.cut;
    }
  }

  /**
   * Get data format (ARCHIVE2, AR2V0001) for this file.
   *
   * @return data format (ARCHIVE2, AR2V0001) for this file.
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
   * @see Level2Record#getVolumeCoveragePatternName
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
   * @param inputRaf  file to uncompress
   * @param ufilename write to this file
   * @return raf of uncompressed file
   * @throws IOException on read error
   */
  private RandomAccessFile uncompress(RandomAccessFile inputRaf, String ufilename) throws IOException {
    RandomAccessFile outputRaf = new RandomAccessFile(ufilename, "rw");
    FileLock lock;

    while (true) { // loop waiting for the lock
      try {
        lock = outputRaf.getRandomAccessFile().getChannel().lock(0, 1, false);
        break;

      } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
        try {
          Thread.sleep(100); // msecs
        } catch (InterruptedException e1) {
        }
      } catch (IOException e) {
          outputRaf.close();
          throw e;
      }
    }

    try {
        inputRaf.seek(0);
        byte[] header = new byte[Level2Record.FILE_HEADER_SIZE];
        int bytesRead = inputRaf.read(header);
        if (bytesRead != header.length)
        {
            throw new IOException("Error reading NEXRAD2 header -- got " +
                    bytesRead + " rather than" + header.length);
        }
        outputRaf.write(header);

        boolean eof = false;
        int numCompBytes;
        byte[] ubuff = new byte[40000];
        byte[] obuff = new byte[40000];

        CBZip2InputStream cbzip2 = new CBZip2InputStream();
        while (!eof) {
            try {
                numCompBytes = inputRaf.readInt();
                if (numCompBytes == -1) {
                    if (log.isDebugEnabled())
                        log.debug("  done: numCompBytes=-1 ");
                    break;
                }
            } catch (EOFException ee) {
                log.debug("got EOFException");
                break; // assume this is ok
            }

            if (log.isDebugEnabled()) {
                log.debug("reading compressed bytes " + numCompBytes + " input starts at " + inputRaf.getFilePointer() + "; output starts at " + outputRaf.getFilePointer());

            }
          /*
          * For some stupid reason, the last block seems to
          * have the number of bytes negated.  So, we just
          * assume that any negative number (other than -1)
          * is the last block and go on our merry little way.
          */
            if (numCompBytes < 0) {
                if (log.isDebugEnabled())
                    log.debug("last block?" + numCompBytes);
                numCompBytes = -numCompBytes;
                eof = true;
            }
            byte[] buf = new byte[numCompBytes];
            inputRaf.readFully(buf);
            ByteArrayInputStream bis = new ByteArrayInputStream(buf, 2,
                    numCompBytes - 2);

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
                if (obuff.length >= 0) outputRaf.write(obuff, 0, total);
            } catch (BZip2ReadException ioe) {
                log.warn("Nexrad2IOSP.uncompress ", ioe);
            }
            float nrecords = (float) (total / 2432.0);
            if (log.isDebugEnabled())
                log.debug("  unpacked " + total + " num bytes " + nrecords + " records; ouput ends at " + outputRaf.getFilePointer());
        }

        outputRaf.flush();
    } catch (IOException e) {
        if (outputRaf != null) outputRaf.close();

        // dont leave bad files around
        File ufile = new File(ufilename);
        if (ufile.exists()) {
            if (!ufile.delete())
                log.warn("failed to delete uncompressed file (IOException)" + ufilename);
        }

        throw e;
    } finally {
      try {
          if (lock != null) lock.release();
      } catch (IOException e) {
          if (outputRaf != null) outputRaf.close();
          throw e;
      }
    }

    return outputRaf;
  }

  // check if compressed file seems ok
  static public long testValid(String ufilename) throws IOException {
    boolean lookForHeader = false;

    // gotta make it
    try (RandomAccessFile raf = RandomAccessFile.acquire(ufilename)) {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);
      String test = raf.readString(8);
      if (test.equals(Level2VolumeScan.ARCHIVE2) ||
                test.startsWith("AR2V000")) {
        System.out.println("--Good header= " + test);
        raf.seek(24);
      } else {
        System.out.println("--No header ");
        lookForHeader = true;
        raf.seek(0);
      }

      boolean eof = false;
      int numCompBytes;

      while (!eof) {

        if (lookForHeader) {
          test = raf.readString(8);
          if (test.equals(Level2VolumeScan.ARCHIVE2) ||
                  test.startsWith("AR2V000")) {
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
      return raf.getFilePointer();
    } catch (EOFException e) {
      e.printStackTrace();
    }
    return 0;
  }

}