package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.KMPMatch;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Scan raf for grib-2 messages
 *
 * @author caron
 * @since 3/28/11
 */
public class Grib2RecordScanner {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2RecordScanner.class);
  static private final KMPMatch matcher = new KMPMatch(new byte[] {'G','R','I','B'} );
  static private final boolean debug = false;
  static private final boolean debugRepeat = false;
  static private final int maxScan = 16000;

  static public boolean isValidFile(RandomAccessFile raf) {
    try {
      raf.seek(0);
      boolean found = raf.searchForward(matcher, maxScan); // look in first 16K
      if (!found) return false;
      raf.skipBytes(7); // will be positioned on byte 0 of indicator section
      int edition = raf.read(); // read at byte 8
      if (edition != 2) return false;

      // check ending = 7777
      long len = GribNumbers.int8(raf);
      if (len > raf.length()) return false;
      raf.skipBytes(len-20);
      for (int i = 0; i < 4; i++) {
        if (raf.read() != 55) return false;
      }
      return true;

    } catch (IOException e) {
      return false;
    }
  }

  /**
   * tricky bit of business. recapture the entire record based on drs position.
   * for validation.
   * @param raf             from this RandomAccessFile
   * @param drsPos          Grib2SectionDataRepresentation starts here
   */
  public static Grib2Record findRecordByDrspos(RandomAccessFile raf, long drsPos) throws IOException {
    long pos = Math.max(0, drsPos- (20*1000)); // go back 20K
    Grib2RecordScanner scan = new Grib2RecordScanner(raf, pos);
    while (scan.hasNext()) {
      ucar.nc2.grib.grib2.Grib2Record gr = scan.next();
      Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
      if (drsPos == drs.getStartingPosition()) return gr;
      if (raf.getFilePointer() > drsPos) break;   // missed it.
    }
    return null;
  }


  //////////////////////////////////////////////////////////////////////////////

  private Map<Long, Grib2SectionGridDefinition> gdsMap = new HashMap<>();
  private ucar.unidata.io.RandomAccessFile raf = null;

  private byte[] header;
  // private long startPos = 0;
  private long lastPos = 0;    // start scanning from here

  // deal with repeating sections - each becomes a Grib2Record
  private long repeatPos = -1;             // if > 0, we are in middle of repeating record
  private Grib2Record repeatRecord = null; // current repeating record
  private Grib2SectionBitMap repeatBms = null; // current repeating bms

  public Grib2RecordScanner(RandomAccessFile raf) throws IOException {
    this.raf = raf;
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    lastPos = 0;

    if (debugRepeat) System.out.printf(" Grib2RecordScanner %s%n", raf.getLocation());
  }

  private Grib2RecordScanner(RandomAccessFile raf, long startFrom) throws IOException {
    this.raf = raf;
    raf.seek(startFrom);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    lastPos = startFrom;
  }

  public boolean hasNext() throws IOException {
    if (lastPos >= raf.length()) return false;
    if (repeatPos > 0) {
      if (nextRepeating()) // this has created a new repeatRecord
        return true;
    } else {
      repeatRecord = null;
      repeatBms = null;
      // fall through to new record
    }

    boolean more;
    long stop = 0;

    while (true) { // scan until we get a GRIB-2 or more == false
      raf.seek(lastPos);
      more = raf.searchForward(matcher, -1); // will scan to end for a 'GRIB' string
      if (!more) break;

      stop = raf.getFilePointer();           // this is where the next 'GRIB' starts
      // see if its GRIB-2
      raf.skipBytes(7);
      int edition = raf.read();
      if (edition == 2) break;
      lastPos = raf.getFilePointer();   // not edition 2 ! just skip it !! start scanning from there
      log.warn("GRIB message at pos=" + stop + " not GRIB2; skip");
    }

    if (more) {
      int sizeHeader = (int) (stop - lastPos);  // wmo headers are embedded between records in some idd streams
      long startPos = stop-sizeHeader;
      if (sizeHeader > 100) sizeHeader = 100;   // maximum 100 bytes; more is likely to be garbage
      header = new byte[sizeHeader];
      raf.seek(startPos);
      raf.readFully(header);
      raf.seek(stop);
    }
    if (debug) System.out.println(" more "+more+" at "+stop+" header at "+ lastPos);
    return more;
  }

  public Grib2Record next() throws IOException {
    if (repeatRecord != null) { // serve current repeatRecord if it exists
      return new Grib2Record(repeatRecord);
    }

    Grib2SectionIndicator is = null;
    try {
      is = new Grib2SectionIndicator(raf);
      Grib2SectionIdentification ids = new Grib2SectionIdentification(raf);
      Grib2SectionLocalUse lus = new Grib2SectionLocalUse(raf);
      Grib2SectionGridDefinition gds = new Grib2SectionGridDefinition(raf);
      Grib2SectionProductDefinition pds = new Grib2SectionProductDefinition(raf);
      Grib2SectionDataRepresentation drs = new Grib2SectionDataRepresentation(raf);
      Grib2SectionBitMap bms = new Grib2SectionBitMap(raf);
      Grib2SectionData dataSection = new Grib2SectionData(raf);
      if (dataSection.getMsgLength() > is.getMessageLength()) { // presumably corrupt
        raf.seek(drs.getStartingPosition()); // go back to before the dataSection
        throw new IllegalStateException("Illegal Grib2SectionData Message Length");
      }

      // look for duplicate gds
      long crc = gds.calcCRC();
      Grib2SectionGridDefinition gdsCached = gdsMap.get(crc);
      if (gdsCached != null)
        gds = gdsCached;       // hmmmm why ??
      else
        gdsMap.put(crc, gds);

      // look for duplicate pds
      /* crc = pds.calcCRC();
      Grib2SectionProductDefinition pdsCached = pdsMap.get(crc);
      if (pdsCached != null)
        pds = pdsCached;
      else
        pdsMap.put(crc, pds); */

      // check to see if we have a repeating record
      long pos = raf.getFilePointer();
      long ending = is.getEndPos();
      if (pos+34 < ending) { // give it 30 bytes of slop
        if (debugRepeat) System.out.printf(" REPEAT AT %d != %d%n", pos+4, ending);
        repeatPos = pos;
        repeatRecord = new Grib2Record(header, is, ids, lus, gds, pds, drs, bms, dataSection, false, Grib2Index.ScanModeMissing); // this assumes immutable sections
        // track bms in case its a repeat
        if (bms.getBitMapIndicator() == 0)
          repeatBms = bms;
        return new Grib2Record(repeatRecord); // GribRecord isnt immutable; still, may not be necessary
      }

      if (debug) System.out.printf(" read until %d grib ending at %d header ='%s'%n", raf.getFilePointer(), ending, StringUtil2.cleanup(header));

      // check that end section is correct
      boolean foundEnding = true;
      raf.seek(ending-4);
      for (int i = 0; i < 4; i++) {
        if (raf.read() != 55) {
          foundEnding = false;
          String clean = StringUtil2.cleanup(header);
          if (clean.length() > 40) clean = clean.substring(0,40) + "...";
          if (debug) System.out.printf(" **missing End of GRIB message at pos=%d start= %d%n", ending, is.getStartPos());
          log.warn("Missing End of GRIB message at pos=" + ending + " start= " + is.getStartPos()+" header= "+clean+" for="+raf.getLocation());
          break;
        }
      }
      if (debug) System.out.printf(" read until %d grib ending at %d header ='%s' foundEnding=%s%n",
              raf.getFilePointer(), ending, StringUtil2.cleanup(header), foundEnding);

      if (foundEnding) {
        lastPos = raf.getFilePointer();
        return new Grib2Record(header, is, ids, lus, gds, pds, drs, bms, dataSection, false, Grib2Index.ScanModeMissing);

      } else { // skip this record, start scanning again at end of is + 20 bytes
        lastPos = is.getEndPos() + 20;
        if (hasNext()) // search forward for another one
         return next();
      }

    } catch (Throwable t) {
      long pos = (is == null) ? -1 : is.getStartPos();
      log.warn("Bad GRIB2 record in file {}, skipping pos={} cause={}", raf.getLocation(), pos, t.getMessage());
      lastPos = raf.getFilePointer();   // start scanning from wherever we are in the file
      if (hasNext()) // skip forward
        return next();
    }

    return null; // last record was incomplete
  }

  // return true if got another repeat out of this record
  // side effect is that the new record is in repeatRecord
  private boolean nextRepeating() throws IOException {
    raf.seek(repeatPos);

    GribNumbers.int4(raf); // octets 1-4 (Length of GDS)
    int section = raf.read();
    raf.seek(repeatPos);

    if (section == 2) {
      repeatRecord.setLus(new Grib2SectionLocalUse(raf));
      repeatRecord.setGdss(new Grib2SectionGridDefinition(raf));
      repeatRecord.setPdss(new Grib2SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib2SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib2SectionBitMap(raf), false);
      repeatRecord.setDataSection(new Grib2SectionData(raf));
      repeatRecord.repeat = section;

    } else if (section == 3) {
      repeatRecord.setGdss(new Grib2SectionGridDefinition(raf));
      repeatRecord.setPdss(new Grib2SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib2SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib2SectionBitMap(raf), false);
      repeatRecord.setDataSection(new Grib2SectionData(raf));
      repeatRecord.repeat = section;

    } else if (section == 4) {
      repeatRecord.setPdss(new Grib2SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib2SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib2SectionBitMap(raf), false);
      repeatRecord.setDataSection(new Grib2SectionData(raf));
      repeatRecord.repeat = section;

    } else {
      if (debugRepeat) System.out.printf(" REPEAT Terminate %d%n", section);
      repeatPos = -1;
      repeatRecord = null;
      repeatBms = null;
      return false;
    }

    // look for repeating bms
    Grib2SectionBitMap bms = repeatRecord.getBitmapSection();
    if (bms.getBitMapIndicator() == 254) {
      // replace BMS with last good one
      if (repeatBms == null)
        throw new IllegalStateException("No bms in repeating section");
      repeatRecord.setBms(repeatBms, true);
      //debug
      if (debugRepeat) System.out.printf("replaced bms %d%n", section);
      repeatRecord.repeat += 1000;

    } else if (bms.getBitMapIndicator() == 0) {
      // track last good bms
      repeatBms = repeatRecord.getBitmapSection();
    }

    // keep only unique gds
    if ((section == 2) || (section == 3)) {
      // look for duplicate gds
      Grib2SectionGridDefinition gds = repeatRecord.getGDSsection();
      long crc = gds.calcCRC();
      Grib2SectionGridDefinition gdsCached = gdsMap.get(crc);
      if (gdsCached != null)
        repeatRecord.setGdss(gdsCached);
      else
        gdsMap.put(crc, gds);
    }

   // check to see if we are at the end
    long pos = raf.getFilePointer();
    long ending = repeatRecord.getIs().getEndPos();
    if (pos+34 < ending) { // give it 30 bytes of slop
      if (debugRepeat) System.out.printf(" REPEAT AGAIN %d != %d%n", pos+4, ending);
      repeatPos = pos;
      return true;
    }

    if (debug) System.out.printf(" REPEAT read until %d grib ending at %d header ='%s'%n", raf.getFilePointer(), ending, StringUtil2.cleanup(header));

    // check that end section is correct
    raf.seek(ending-4);
    for (int i = 0; i < 4; i++) {
      if (raf.read() != 55) {
        String clean = StringUtil2.cleanup(header);
        if (clean.length() > 40) clean = clean.substring(0,40) + "...";
        log.warn("  REPEAT Missing End of GRIB message at pos=" + ending + " header= " + clean+" for="+raf.getLocation());
        break;
      }
    }
    lastPos = raf.getFilePointer();

    if (debugRepeat) System.out.printf(" REPEAT DONE%n");
    repeatPos = -1; // no more repeats in this record
    return true;
  }

  public static void main2(String[] args) throws IOException {
    String filename = (args.length > 0 && args[0] != null) ? args[0] : "Q:/cdmUnitTest/formats/grib2/LMPEF_CLM_050518_1200.grb";
    System.out.printf("Scan %s%n", filename);

    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      raf.seek(0);
      while (!raf.isAtEndOfFile()) {
        boolean found = raf.searchForward(matcher, -1);
        if (found) {
          raf.skipBytes(7); // will be positioned on byte 0 of indicator section
          int edition = raf.read(); // read at byte 8
          System.out.printf(" GRIB edition %d found at pos %d%n", edition, raf.getFilePointer());
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    int count = 0;
    RandomAccessFile raf = new RandomAccessFile("Q:/cdmUnitTest/formats/grib2/LMPEF_CLM_050518_1200.grb", "r");
    System.out.printf("Read %s%n", raf.getLocation());
    Grib2RecordScanner scan = new Grib2RecordScanner(raf);
    while (scan.hasNext()) {
      scan.next();
      count++;
    }
    raf.close();
    System.out.printf("count=%d%n",count);
  }

}
