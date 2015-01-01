package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.zip.CRC32;

/**
 * The Grid Definition section 3 for GRIB-2 files
 * <pre>
  Octet Contents
  1–4   Length of section in octets (nn)
  5     Number of section (3)
  6     Source of grid definition (see Code table 3.0 and Note 1)
  7–10  Number of data points
  11    Number of octets for optional list of numbers (see Note 2)
  12    Interpretation of list of numbers (see Code table 3.11)
  13–14 Grid definition template number (= N) (see Code table 3.1)
  15–xx Grid definition template (see Template 3.N, where N is the grid definition template number given in octets 13–14)
  [xx+1]–nn Optional list of numbers defining number of points (see Notes 2, 3 and 4)
 </pre>
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionGridDefinition {
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
    templateNumber = GribNumbers.uint2(raf);

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
    this.templateNumber = GribNumbers.uint2( getOctet(13), getOctet(14) );
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
    CRC32 crc32 = new CRC32();
    crc32.update(rawData);
    return crc32.getValue();
  }

  public int getLength() {
    return rawData.length;
  }

  public long getOffset() {
    return startingPosition;
  }

  /**
   * octet 6
   * source of grid definition (Code Table 3.0)
   * "If octet 6 is not zero, octets 15–xx (15–nn if octet 11 is zero) may not be supplied. This should be documented with all
      bits set to 1 (missing value) in the grid definition template number."
   * @return source
   */
  public int getSource() {
    return getOctet(6);
  }

  /**
   * octets 7-10
   * number of data points .
   *
   * @return numberPoints
   */
  public int getNumberPoints() {
    return GribNumbers.int4(getOctet(7), getOctet(8), getOctet(9), getOctet(10));
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
  private int getOctet(int index) {
    return rawData[index-1] & 0xff;
  }

  public Grib2Gds getGDS() {
    return Grib2Gds.factory(templateNumber, rawData);
  }

}
