/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.ui.gis.shapefile;

import ucar.nc2.constants.CDM;

import java.io.DataInputStream;

/**
 * A dBase field descriptor object.  Nothing public here, this is all just for
 * use by DbaseFile and DbaseData.
 *
 * @author Kirk Water
 */
class DbaseFieldDesc {
  String Name;
  byte Type;
  int FieldLength;
  int DecimalCount;
  byte[] Header;

  DbaseFieldDesc(DataInputStream in, byte version) {
    if ((version & 0x03) == 3) {
      read_dbase3(in);
    } else {
      read_dbase4(in);
    }
  }

  DbaseFieldDesc(String Name, byte Type, int FieldLength, int DecimalCount,
                 int SetFlags, int WorkAreaID, byte MDXflag) {
    this.Name = Name;
    this.Type = Type;
    this.FieldLength = FieldLength;
    this.DecimalCount = DecimalCount;
    Header = new byte[32];
    for (int i = 0; i < 32; i++) {
      Header[i] = (byte) ' ';
    }
    // put the name into the header
    byte[] headerName = Name.getBytes(CDM.utf8Charset);
    System.arraycopy(headerName, 0, Header, 0, headerName.length);

    Header[11] = Type;
    Header[16] = (byte) FieldLength;
    Header[17] = (byte) DecimalCount;
    Header[23] = (byte) SetFlags;
    Header[20] = (byte) WorkAreaID;
    Header[31] = MDXflag;
  }

  private int read_dbase3(DataInputStream in) {

    Header = new byte[32];
    try {
      in.readFully(Header, 0, 32);
    } catch (java.io.IOException e) {
      return -1;
    }

    /* requires 1.1 compiler or higher */
    Name = new String(Header, 0, 11, CDM.utf8Charset);

    Name = Name.trim();
    Type = Header[11];
    FieldLength = (int) Header[16];
    if (FieldLength < 0) FieldLength += 256;
    DecimalCount = (int) Header[17];
    if (DecimalCount < 0) DecimalCount += 256;
    return 0;
  }

  /* works for dbase 5.0 DOS and Windows too */

  private int read_dbase4(DataInputStream in) {
    if (read_dbase3(in) != 0) return -1;
    return 0;
  }

  public String toString() {
    return (Name);
  }
}
