/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import ucar.nc2.constants.CDM;

import java.io.RandomAccessFile;

class DoradeRDAT extends DoradeDescriptor {

  private String paramName;
  private byte[] paramData;

  public DoradeRDAT(RandomAccessFile file, boolean littleEndianData) throws DescriptorException {
    byte[] data = readDescriptor(file, littleEndianData, "RDAT");

    //
    // unpack
    //
    paramName = new String(data, 8, 8, CDM.utf8Charset).trim();
    paramData = new byte[data.length - 16];
    System.arraycopy(data, 16, paramData, 0, data.length - 16);

    //
    // debugging output
    //
    if (verbose)
      System.out.println(this);
  }

  public String toString() {
    String s = "RDAT\n";
    s += "  param name: " + paramName + "\n";
    s += "  data length: " + paramData.length;
    return s;
  }

  public String getParamName() {
    return paramName;
  }

  public byte[] getRawData() {
    return paramData;
  }

  public static DoradeRDAT getNextOf(DoradePARM parm, RandomAccessFile file, boolean littleEndianData) throws DescriptorException, java.io.IOException {
    while (true) {
      long pos = findNextWithName("RDAT", file, littleEndianData);
      if (peekParamName(file).equals(parm.getName()))
        return new DoradeRDAT(file, littleEndianData);
      else
        skipDescriptor(file, littleEndianData);
    }
  }

  private static String peekParamName(RandomAccessFile file) throws DescriptorException {
    try {
      long filepos = file.getFilePointer();
      file.skipBytes(8);
      byte[] nameBytes = new byte[8];
      if (file.read(nameBytes) == -1)
        throw new DescriptorException("unexpected EOF");
      file.seek(filepos);
      return new String(nameBytes, CDM.utf8Charset).trim();

    } catch (Exception ex) {
      throw new DescriptorException(ex);
    }
  }

}