/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1;

import ucar.grib.GribNumbers;
import ucar.unidata.io.KMPMatch;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author John
 * @since 9/3/11
 */
public class Grib1RecordScanner {
    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1RecordScanner.class);
  static private final KMPMatch matcher = new KMPMatch("GRIB".getBytes());
  static private final boolean debug = false;
  static private final boolean debugRepeat = false;

  private Map<Long, Grib1SectionGridDefinition> gdsMap = new HashMap<Long, Grib1SectionGridDefinition>();
  private ucar.unidata.io.RandomAccessFile raf = null;

  private byte[] header;
  private long startPos = 0;
  private long lastPos = 0;

  // deal with repeating sections - each becomes a Grib1Record
  private long repeatPos = -1;             // if > 0, we are in middle of repeating record
  private Grib1Record repeatRecord = null; // current repeating record

  public Grib1RecordScanner(RandomAccessFile raf) throws IOException {
    startPos = 0;
    this.raf = raf;
    raf.seek(startPos);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    lastPos = startPos;

    if (debugRepeat) System.out.printf(" Grib1RecordScanner %s%n", raf.getLocation());
  }

  public boolean hasNext() throws IOException {
    if (lastPos >= raf.length()) return false;
    /* if (repeatPos > 0) {
      if (nextRepeating()) // this has created a new repeatRecord
        return true;
    } else {
      repeatRecord = null;
      // fall through to new record
    }  */

    raf.seek(lastPos);
    boolean more = raf.searchForward(matcher, -1); // will scan to end for another GRIB header
    if (more) {
      long stop = raf.getFilePointer();
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

  public Grib1Record next() throws IOException {
    if (repeatRecord != null) { // serve current repeatRecord if it exists
      return new Grib1Record(repeatRecord);
    }

    Grib1SectionIndicator is = null;
    try {
      is = new Grib1SectionIndicator(raf);
      Grib1SectionGridDefinition gds = new Grib1SectionGridDefinition(raf);
      Grib1SectionProductDefinition pds = new Grib1SectionProductDefinition(raf);
      Grib1SectionBinaryData dataSection = new Grib1SectionBinaryData(raf);
      //if (dataSection.getDataLength() > is.getMessageLength()) { // presumably corrupt
      //  raf.seek(drs.getStartingPosition()); // go back to before the dataSection
      //  throw new IllegalStateException("Illegal Grib1SectionData Message Length");
      //}

      // look for duplicate gds
      long crc = gds.calcCRC();
      Grib1SectionGridDefinition gdsCached = gdsMap.get(crc);
      if (gdsCached != null)
        gds = gdsCached;
      else
        gdsMap.put(crc, gds);

      // look for duplicate pds
      /* crc = pds.calcCRC();
      Grib1SectionProductDefinition pdsCached = pdsMap.get(crc);
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
        repeatRecord = new Grib1Record(header, is, gds, pds, dataSection); // this assumes immutable sections
        return new Grib1Record(repeatRecord); // GribRecord isnt immutable; still may not be necessary
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

      return new Grib1Record(header, is, gds, pds, dataSection);

    } catch (Throwable t) {
      long pos = (is == null) ? -1 : is.getStartPos();
      log.warn("Bad Grib1 record in file {}, skipping pos={}", raf.getLocation(), pos);
      lastPos = raf.getFilePointer();
      if (hasNext()) // skip forward
        return next();
    }

    return null; // last record was incomplete
  }

  // return true if got another repeat out of this record
  /* side effect is that the new record is in repeatRecord
  private boolean nextRepeating() throws IOException {
    raf.seek(repeatPos);

    // octets 1-4 (Length of GDS)
    int length = GribNumbers.int4(raf);
    int section = raf.read();
    raf.seek(repeatPos);

    if (section == 2) {
      repeatRecord.setLus(new Grib1SectionLocalUse(raf));
      repeatRecord.setGdss(new Grib1SectionGridDefinition(raf));
      repeatRecord.setPdss(new Grib1SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib1SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib1SectionBitMap(raf));
      repeatRecord.setDataSection(new Grib1SectionData(raf));

    } else if (section == 3) {
      repeatRecord.setGdss(new Grib1SectionGridDefinition(raf));
      repeatRecord.setPdss(new Grib1SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib1SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib1SectionBitMap(raf));
      repeatRecord.setDataSection(new Grib1SectionData(raf));

    } else if (section == 4) {
      repeatRecord.setPdss(new Grib1SectionProductDefinition(raf));
      repeatRecord.setDrs(new Grib1SectionDataRepresentation(raf));
      repeatRecord.setBms(new Grib1SectionBitMap(raf));
      repeatRecord.setDataSection(new Grib1SectionData(raf));

    } else {
      if (debugRepeat) System.out.printf(" REPEAT Terminate %d%n", section);
      repeatPos = -1;
      repeatRecord = null;
      return false;
    }

    if ((section == 2) || (section == 3)) {
      // look for duplicate gds
      Grib1SectionGridDefinition gds = repeatRecord.getGDSsection();
      long crc = gds.calcCRC();
      Grib1SectionGridDefinition gdsCached = gdsMap.get(crc);
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
   * @param drsPos          Grib1SectionDataRepresentation starts here
   *
  static public Grib1Record findRecordByDrspos(RandomAccessFile raf, long drsPos) throws IOException {
    Grib1Record result = null;
    Grib1RecordScanner scanner = new Grib1RecordScanner(raf);
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

  }  */

  public static void main(String[] args) throws IOException {
    RandomAccessFile raf = new RandomAccessFile("Q:/cdmUnitTest/formats/Grib1/sfc_d01_20080430_1200_f00000.grb2", "r");
    Grib1RecordScanner scan = new Grib1RecordScanner(raf);
    while (scan.hasNext())
      scan.next();
    raf.close();
  }
}
