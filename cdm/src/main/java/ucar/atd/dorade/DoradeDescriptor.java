/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDateFormatter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Date;
import java.util.TimeZone;
import java.util.HashMap;

abstract class DoradeDescriptor {

  protected String descName;
  protected String expectedName;
  protected RandomAccessFile file;
  protected boolean littleEndianData;
  protected boolean verbose;

  protected static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
  private static boolean defaultVerboseState = false;
  // map from descriptor names to per-class default verbose states
  private static HashMap<String, Boolean> classVerboseStates = new HashMap<>();

  static class DescriptorException extends Exception {
    protected DescriptorException(String message) {
      super(message);
    }

    protected DescriptorException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Read and set the descriptor name, size, and endianness, and return the
   * entire contents of the descriptor (including the name and size) as a
   * byte array.  The file position will be left at the beginning of the next
   * descriptor (or at the end of file).
   *
   * @param file             the DORADE sweepfile, positioned at the beginning of a
   *                         descriptor
   * @param littleEndianData set to true iff the file contains little-endian
   *                         data
   * @param expectedName     the expected name for the descriptor being read
   * @throws DescriptorException for file read errors, descriptor name
   *                             mismatch, etc.
   */
  protected byte[] readDescriptor(RandomAccessFile file,
                                  boolean littleEndianData,
                                  String expectedName)
          throws DescriptorException {

    this.file = file;
    this.littleEndianData = littleEndianData;
    this.expectedName = expectedName;
    verbose = getDefaultVerboseState(expectedName);

    byte[] data;

    try {
      //
      // find the next descriptor with our expected name
      //
      findNext(file);

      //
      // keep track of the start of this descriptor
      //
      long startpos = file.getFilePointer();

      //
      // get the name and descriptor size
      //
      byte[] header = new byte[8];
      file.readFully(header);
      descName = new String(header, 0, 4, CDM.utf8Charset);
      int size = grabInt(header, 4);

      //
      // now back up to the start of the descriptor and read the entire
      // thing into a byte array
      //
      file.seek(startpos);

      data = new byte[size];
      file.readFully(data);
    } catch (java.io.IOException ex) {
      throw new DescriptorException(ex);
    }

    //
    // now check the name we got against the expected name
    //
    if (!descName.equals(expectedName))
      throw new DescriptorException("Got descriptor name '" + descName +
              "' when expecting name '" +
              expectedName + "'");

    return data;
  }

  /**
   * Skip the current DORADE descriptor in the file, leaving the file position
   * at the beginning of the next descriptor (or at the end of file).
   *
   * @param file the DORADE sweepfile, positioned at the beginning of a
   *             descriptor
   * @throws java.io.IOException
   */
  protected static void skipDescriptor(RandomAccessFile file,
                                       boolean littleEndianData)
          throws DescriptorException, java.io.IOException {
    try {
      file.readFully(new byte[4]); // skip name
      byte[] lenBytes = new byte[4];
      file.readFully(lenBytes);
      int descLen = grabInt(lenBytes, 0, littleEndianData);
      file.readFully(new byte[descLen - 8]);
    } catch (java.io.EOFException eofex) {
      return; // just leave the file at EOF
    } catch (Exception ex) {
      throw new DescriptorException(ex);
    }
  }

  /**
   * Return the name of the DORADE descriptor at the current location
   * in the file.  The current location will not be changed.
   *
   * @param file the DORADE sweep file, positioned at the beginning of a
   *             descriptor
   * @return the name of the DORADE descriptor starting at the current
   * file position, or null if no descriptor name is available
   * @throws DescriptorException
   */
  protected static String peekName(RandomAccessFile file)
          throws DescriptorException {
    try {
      long filepos = file.getFilePointer();
      byte[] nameBytes = new byte[4];
      if (file.read(nameBytes) == -1)
        return null;  // EOF
      file.seek(filepos);
      return new String(nameBytes, CDM.utf8Charset);

    } catch (IOException ex) {
      throw new DescriptorException(ex);
    }
  }

  /**
   * Determine if the given DORADE sweepfile contains little-endian data
   * (in violation of the DORADE definition...).
   *
   * @param file the DORADE sweepfile,
   * @return <code>true</code> iff the file contains little-endian data
   * @throws DescriptorException
   */
  public static boolean sweepfileIsLittleEndian(RandomAccessFile file)
          throws DescriptorException {
    int descLen;
    try {
      file.seek(0);
      //
      // skip the 4-byte descriptor name
      //
      byte[] bytes = new byte[4];
      file.readFully(bytes);
      //
      // get the descriptor length
      //
      descLen = file.readInt();
      file.seek(0);
    } catch (Exception ex) {
      throw new DescriptorException(ex);
    }
    return (descLen < 0 || descLen > 0xffffff);
  }

  /**
   * Unpack a two-byte integer from the given byte array.
   *
   * @param bytes  byte array to be read
   * @param offset number of bytes to skip in the byte array before reading
   * @return the unpacked short value
   */
  protected short grabShort(byte[] bytes, int offset) {
    int ndx0 = offset + (littleEndianData ? 1 : 0);
    int ndx1 = offset + (littleEndianData ? 0 : 1);
    // careful that we only allow sign extension on the highest order byte
    return (short) (bytes[ndx0] << 8 | (bytes[ndx1] & 0xff));
  }

  /**
   * Unpack a four-byte integer from the given byte array.
   *
   * @param bytes            byte array to be read
   * @param offset           number of bytes to skip in the byte array before reading
   * @param littleEndianData true iff the byte array contains little-endian
   *                         data
   * @return the unpacked integer value
   */
  protected static int grabInt(byte[] bytes, int offset,
                               boolean littleEndianData) {
    int ndx0 = offset + (littleEndianData ? 3 : 0);
    int ndx1 = offset + (littleEndianData ? 2 : 1);
    int ndx2 = offset + (littleEndianData ? 1 : 2);
    int ndx3 = offset + (littleEndianData ? 0 : 3);

    // careful that we only allow sign extension on the highest order byte
    return (bytes[ndx0] << 24 |
            (bytes[ndx1] & 0xff) << 16 |
            (bytes[ndx2] & 0xff) << 8 |
            (bytes[ndx3] & 0xff));
  }

  /**
   * Unpack a four-byte integer from the given byte array.
   *
   * @param bytes  byte array to be read
   * @param offset number of bytes to skip in the byte array before reading
   * @return the unpacked integer value
   */
  protected int grabInt(byte[] bytes, int offset) {
    return grabInt(bytes, offset, littleEndianData);
  }

  /**
   * Unpack a four-byte IEEE float from the given byte array.
   *
   * @param bytes  byte array to be read
   * @param offset number of bytes to skip in the byte array before reading
   * @return the unpacked float value
   */
  protected float grabFloat(byte[] bytes, int offset)
          throws DescriptorException {
    try {
      byte[] src;
      if (littleEndianData) {
        src = new byte[4];
        src[0] = bytes[offset + 3];
        src[1] = bytes[offset + 2];
        src[2] = bytes[offset + 1];
        src[3] = bytes[offset];
        offset = 0;
      } else {
        src = bytes;
      }
      DataInputStream stream =
              new DataInputStream(new ByteArrayInputStream(src, offset, 4));
      return stream.readFloat();
    } catch (Exception ex) {
      throw new DescriptorException(ex);
    }
  }

  /**
   * Unpack an eight-byte IEEE float from the given byte array.
   *
   * @param bytes  byte array to be read
   * @param offset number of bytes to skip in the byte array before reading
   * @return the unpacked double value
   */
  protected double grabDouble(byte[] bytes, int offset)
          throws DescriptorException {
    try {
      byte[] src;
      if (littleEndianData) {
        src = new byte[8];
        src[0] = bytes[offset + 7];
        src[1] = bytes[offset + 6];
        src[2] = bytes[offset + 5];
        src[3] = bytes[offset + 4];
        src[4] = bytes[offset + 3];
        src[5] = bytes[offset + 2];
        src[6] = bytes[offset + 1];
        src[7] = bytes[offset];
        offset = 0;
      } else {
        src = bytes;
      }
      DataInputStream stream =
              new DataInputStream(new ByteArrayInputStream(src, offset, 8));
      return stream.readDouble();
    } catch (Exception ex) {
      throw new DescriptorException(ex);
    }
  }


  protected static long findNextWithName(String expectedName,
                                         RandomAccessFile file,
                                         boolean littleEndianData)
          throws DescriptorException, java.io.IOException {

    //
    // Skip forward through the file until we find a descriptor with
    // the expected name
    //
    String descName;
    while ((descName = peekName(file)) != null) {
      if (descName.equals(expectedName)) {
        try {
          return file.getFilePointer();
        } catch (java.io.IOException ex) {
          throw new DescriptorException(ex);
        }
      }
      skipDescriptor(file, littleEndianData);
    }
    throw new DescriptorException("Expected " + expectedName +
            " descriptor not found!");
  }

  protected long findNext(RandomAccessFile file)
          throws DescriptorException, java.io.IOException {
    return findNextWithName(expectedName, file, littleEndianData);
  }


  /**
   * Return a string with a reasonable and complete representation of the
   * given <code>Date</code>, shown in UTC.
   *
   * @param date <code>Date</code> to be represented
   * @return a string containing the representation of the date
   */
  public static String formatDate(Date date) {
    return CalendarDateFormatter.toDateTimeString(date);
  }

  /**
   * Get the default verbose state for new <code>DoradeDescriptor</code>-s.
   */
  public static boolean getDefaultVerboseState() {
    return defaultVerboseState;
  }

  /**
   * Set the default verbose state for new <code>DoradeDescriptor</code>-s.
   *
   * @param verbose the new default verbose state
   */
  public static void setDefaultVerboseState(boolean verbose) {
    defaultVerboseState = verbose;
    classVerboseStates.clear();
  }

  /**
   * Get the default verbose state for new <code>DoradeDescriptor</code>-s
   * of the given name.
   *
   * @param descriptorName the descriptor name for which the new default
   *                       verbose state will apply
   */
  public static boolean getDefaultVerboseState(String descriptorName) {
    Boolean classVerboseState = classVerboseStates.get(descriptorName.toUpperCase());
    if (classVerboseState != null)
      return classVerboseState;
    else
      return defaultVerboseState;
  }

  /**
   * Set the default verbose state for new <code>DoradeDescriptor</code>-s
   * of the given name.
   *
   * @param descriptorName the descriptor name for which the new default
   *                       verbose state will apply
   * @param verbose        the new default verbose state
   */
  public static void setDefaultVerboseState(String descriptorName,
                                            boolean verbose) {
    classVerboseStates.put(descriptorName.toUpperCase(), verbose);
  }
}