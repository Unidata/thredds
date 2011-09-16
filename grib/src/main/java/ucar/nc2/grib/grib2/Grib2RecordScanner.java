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
  static private final KMPMatch matcher = new KMPMatch(new byte[] {'G','R','I','B'});
  static private final boolean debug = false;
  static private final boolean debugRepeat = false;

  static public boolean isValidFile(RandomAccessFile raf) {
    try {
      raf.seek(0);
      boolean found = raf.searchForward(matcher, 16 * 1000); // look in first 16K
      if (!found) return false;
      raf.skipBytes(7); // will be positioned on byte 0 of indicator section
      int edition = raf.read(); // read at byte 8
      return (edition == 2);

    } catch (IOException e) {
     return false;
    }
  }

  private Map<Long, Grib2SectionGridDefinition> gdsMap = new HashMap<Long, Grib2SectionGridDefinition>();
  private ucar.unidata.io.RandomAccessFile raf = null;

  private byte[] header;
  private long startPos = 0;
  private long lastPos = 0;

  // deal with repeating sections - each becomes a Grib2Record
  private long repeatPos = -1;             // if > 0, we are in middle of repeating record
  private Grib2Record repeatRecord = null; // current repeating record

  public Grib2RecordScanner(RandomAccessFile raf) throws IOException {
    startPos = 0;
    this.raf = raf;
    raf.seek(startPos);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    lastPos = startPos;

    if (debugRepeat) System.out.printf(" Grib2RecordScanner %s%n", raf.getLocation());
  }

  public boolean hasNext() throws IOException {
    if (lastPos >= raf.length()) return false;
    if (repeatPos > 0) {
      if (nextRepeating()) // this has created a new repeatRecord
        return true;
    } else {
      repeatRecord = null;
      // fall through to new record
    }

    boolean more;
    long stop = 0;

    while (true) { // scan until we get a GRIB-1 or more is false
      raf.seek(lastPos);
      more = raf.searchForward(matcher, -1); // will scan to end for a 'GRIB' string
      if (!more) break;

      stop = raf.getFilePointer();
      // see if its GRIB-2
      raf.skipBytes(7);
      int edition = raf.read();
      if (edition == 2) break;
      lastPos = raf.getFilePointer(); // not edition 2 ! could terminate ??
    }

    if (more) {
      int sizeHeader = (int) (stop - lastPos);
      //if (sizeHeader > 30) sizeHeader = 30;
      header = new byte[sizeHeader];
      startPos = stop-sizeHeader;
      raf.seek(startPos);
      raf.read(header);
    }
    if (debug) System.out.println(" more "+more+" at "+startPos+" lastPos "+ lastPos);
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
        gds = gdsCached;
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
        repeatRecord = new Grib2Record(header, is, ids, lus, gds, pds, drs, bms, dataSection); // this assumes immutable sections
        return new Grib2Record(repeatRecord); // GribRecord isnt immutable; still may not be necessary
      }

      if (debug) System.out.printf(" read until %d grib ending at %d header ='%s'%n", raf.getFilePointer(), ending, StringUtil2.cleanup(header));

      // check that end section is correct
      raf.seek(ending-4);
      for (int i = 0; i < 4; i++) {
        if (raf.read() != 55) {
          log.warn("Missing End of GRIB message at pos=" + ending + " header= " + StringUtil2.cleanup(header)+" for="+raf.getLocation());
          break;
        }
      }
      lastPos = raf.getFilePointer();

      return new Grib2Record(header, is, ids, lus, gds, pds, drs, bms, dataSection);

    } catch (Throwable t) {
      long pos = (is == null) ? -1 : is.getStartPos();
      log.warn("Bad GRIB2 record in file {}, skipping pos={}", raf.getLocation(), pos);
      lastPos = raf.getFilePointer();
      if (hasNext()) // skip forward
        return next();
    }

    return null; // last record was incomplete
  }

  // return true if got another repeat out of this record
  // side effect is that the new record is in repeatRecord
  private boolean nextRepeating() throws IOException {
    raf.seek(repeatPos);

    // octets 1-4 (Length of GDS)
    int length = GribNumbers.int4(raf);
    int section = raf.read();
    raf.seek(repeatPos);

    if (section == 2) {
      repeatRecord.setLus(new Grib2SectionLocalUse(raf));
      repeatRecord.setGdss(new Grib2SectionGridDefinition(raf));
      repeatRecord.setPdss(new Grib2SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib2SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib2SectionBitMap(raf));
      repeatRecord.setDataSection(new Grib2SectionData(raf));

    } else if (section == 3) {
      repeatRecord.setGdss(new Grib2SectionGridDefinition(raf));
      repeatRecord.setPdss(new Grib2SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib2SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib2SectionBitMap(raf));
      repeatRecord.setDataSection(new Grib2SectionData(raf));

    } else if (section == 4) {
      repeatRecord.setPdss(new Grib2SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib2SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib2SectionBitMap(raf));
      repeatRecord.setDataSection(new Grib2SectionData(raf));

    } else {
      if (debugRepeat) System.out.printf(" REPEAT Terminate %d%n", section);
      repeatPos = -1;
      repeatRecord = null;
      return false;
    }

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
        log.warn("  REPEAT Missing End of GRIB message at pos=" + ending + " header= " + StringUtil2.cleanup(header)+" for="+raf.getLocation());
        break;
      }
    }
    lastPos = raf.getFilePointer();

    if (debugRepeat) System.out.printf(" REPEAT DONE%n");
    repeatPos = -1; // no more repeats in this record
    return true;
  }


  /**
   * tricky bit of business. recapture the entire record based on drs position.
   * for validation.
   * @param raf             from this RandomAccessFile
   * @param drsPos          Grib2SectionDataRepresentation starts here
   */
  static public Grib2Record findRecordByDrspos(RandomAccessFile raf, long drsPos) throws IOException {
    Grib2Record result = null;
    Grib2RecordScanner scanner = new Grib2RecordScanner(raf);
    long pos = Math.max(0, drsPos-10000); // go back 10000 bytes
    raf.seek(pos);
    while (scanner.hasNext()) {  // find GRIB header
      result = scanner.next();
      if (result.getDataRepresentationSection().getStartingPosition() == drsPos)
        return result;
      if (raf.getFilePointer() > drsPos)
        break;
    }
    return null;

  }

  public static void main(String[] args) throws IOException {
    RandomAccessFile raf = new RandomAccessFile("Q:/cdmUnitTest/formats/grib2/sfc_d01_20080430_1200_f00000.grb2", "r");
    Grib2RecordScanner scan = new Grib2RecordScanner(raf);
    while (scan.hasNext())
      scan.next();
    raf.close();
  }

}
