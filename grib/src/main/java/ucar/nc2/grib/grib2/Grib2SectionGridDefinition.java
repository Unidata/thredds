package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.zip.CRC32;

/**
 * The Grid Definition section 3 for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class   Grib2SectionGridDefinition {
  private final byte[] rawData;
  private final long startingPosition;
  private final int templateNumber;

  /**
   * Read Grib Definition section from raf.
   *
   * @param raf RandomAccessFile, with pointer at start of section
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib2SectionGridDefinition(RandomAccessFile raf) throws IOException {

    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of GDS)
    int length = GribNumbers.int4(raf);

   // octet 5
    int section = raf.read();  // This is section 3
    if (section != 3)
      throw new IllegalArgumentException("Not a GRIB-2 GDS section");

    // octets 13-14
    raf.skipBytes(7);
    templateNumber = GribNumbers.int2(raf);

    // read in whole GDS as byte[]
    rawData = new byte[length];
    raf.seek(startingPosition);
    raf.readFully(rawData);
  }

  /**
   * Set Grib Definition section from byte array.
   *
   * @param rawData the byte array
   */
  public Grib2SectionGridDefinition(byte[] rawData) {
    this.rawData = rawData;
    this.templateNumber = GribNumbers.int2( getInt(13), getInt(14) );
    this.startingPosition = -1;
  }

  /**
   * get the raw bytes of the GDS
   *
   * @return GDS as byte[]
   */
  public byte[] getRawBytes() {
    return rawData;
  }

  /**
   * Calculate the CRC of the entire byte array
   * @return CRC  of the entire byte array
   */
  public long calcCRC() {
    if (crc == 0) {
      CRC32 crc32 = new CRC32();
      crc32.update(rawData);
      crc = crc32.getValue();
    }
    return crc;
  }
  private long crc = 0;

  public int getLength() {
    return rawData.length;
  }

  public long getOffset() {
    return startingPosition;
  }

  /**
   * octet 6
   * source of grid definition (Code Table 3.0)
   *
   * @return source
   */
  public int getSource() {
    return getInt(6);
  }

  /**
   * octets 7-10
   * number of data points .
   *
   * @return numberPoints
   */
  public int getNumberPoints() {
    return GribNumbers.int4(getInt(7), getInt(8), getInt(9), getInt(10));
  }

  /**
   * Get GDS Template number (code table 3.1)
   *
   * @return GDS Template number.
   */
  public int getGDSTemplateNumber() {
    return templateNumber;
  }

  /**
   * Get the the byte in the raw Data
   *
   * @param index in the raw array
   * @return byte[index-1] as int
   */
  private int getInt(int index) {
    return rawData[index-1] & 0xff;
  }

  private Grib2Gds gds = null;
  public Grib2Gds getGDS() {
    if (gds == null)
      gds = Grib2Gds.factory(templateNumber, rawData);
    return gds;
  }

}
