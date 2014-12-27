package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.zip.CRC32;

/**
 * The Product Definition section 4 for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionProductDefinition {
  private final byte[] rawData;
  private final int templateNumber;

  /**
   * Read Product Definition section from raf.
   *
   * @param raf RandomAccessFile, with pointer at start of section
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
   public Grib2SectionProductDefinition( RandomAccessFile raf) throws IOException {

     long startingPosition = raf.getFilePointer();

     // octets 1-4 (Length of GDS)
     int length = GribNumbers.int4(raf);

    // octet 5
     int section = raf.read();
     if (section != 4)
       throw new IllegalArgumentException("Not a GRIB-2 PDS section");

     // octets 8-9
     raf.skipBytes(2);
     templateNumber = GribNumbers.int2(raf);

     // read in whole GDS as byte[]
     rawData = new byte[length];
     raf.seek(startingPosition);
     raf.readFully(rawData);
   }

  /**
   * Set PDS section from byte array.
   *
   * @param rawData the byte array
   */
  public Grib2SectionProductDefinition( byte[] rawData) {
    this.rawData = rawData;
    this.templateNumber = GribNumbers.int2( getInt(8), getInt(9) );
  }

  /**
   * get the raw bytes of the PDS
   *
   * @return PDS as byte[]
   */
  public byte[] getRawBytes() {
    return rawData;
  }

  /**
   * Calculate the CRC of the entire byte array
   * @return CRC
   */
  public long calcCRC() {
    CRC32 crc32 = new CRC32();
    crc32.update(rawData);
    return crc32.getValue();
  }

  public int getLength() {
    return rawData.length;
  }

  /**
   * Get PDS Template Number (code table 4.0)
   *
   * @return PDS Template Number
   */
  public int getPDSTemplateNumber() {
    return templateNumber;
  }

  private int getInt(int index) {
    return rawData[index-1] & 0xff;
  }

  /**
   * Parse the raw bytes into a Grib2Pds
   * @return Grib2Pds
   */
  public Grib2Pds getPDS() {
    return Grib2Pds.factory(templateNumber, rawData);
  }
}
