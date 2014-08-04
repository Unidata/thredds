package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * The Indicator Section for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionIndicator {
  static final byte[] MAGIC = new byte[]{'G','R','I','B'};

  private final long messageLength;
  private final int discipline;
  private final long startPos;

  /**
   * Read Grib2SectionIndicator from raf.
   *
   * @param raf RandomAccessFile, with pointer at start (the "GRIB")
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib2SectionIndicator(RandomAccessFile raf) throws IOException {
    startPos = raf.getFilePointer();
    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != MAGIC[i])
        throw new IllegalArgumentException("Not a GRIB record");

    raf.skipBytes(2);
    discipline = raf.read();
    int edition = raf.read();
    if (edition != 2)
      throw new IllegalArgumentException("Not a GRIB-2 record");

    messageLength = GribNumbers.int8(raf);
  }

  public Grib2SectionIndicator(long startPos, long messageLength, int discipline) {
    this.startPos = startPos;
    this.messageLength = messageLength;
    this.discipline = discipline;
  }

  /**
   * Get the length of this GRIB record in bytes.
   *
   * @return length in bytes of GRIB record
   */
  public long getMessageLength() {
    return messageLength;
  }

  public long getStartPos() {
   return startPos;
 }

  public long getEndPos() {
   return startPos + messageLength;
 }

  /**
   * Discipline - GRIB Master Table Number.
   *
   * @return discipline number
   */
  public int getDiscipline() {
    return discipline;
  }
}
