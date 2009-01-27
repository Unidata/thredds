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
package ucar.unidata.io;

import java.io.*;

/**
 * Similar to a DataInputStream that keeps track of position.
 * position must always increase, no going backwards.
 * cant handle byte order yet - assume big endian(?).
 * @author caron
 * @since Jan 3, 2008
 */
public class PositioningDataInputStream {
  private DataInputStream delegate;
  private long cpos = 0;

  public PositioningDataInputStream(InputStream is) {
    if (is instanceof DataInputStream)
      delegate = (DataInputStream) is;
    else
      delegate = new DataInputStream(is);
  }

  private void seek(long pos) throws IOException {
    if (pos < cpos) throw new IllegalArgumentException("Cannot go backwards; current="+cpos+" request="+pos);
    long want = pos-cpos;
    while (want > 0)
      want -= delegate.skip(want);
    cpos=pos;
    //System.out.println("now at "+pos);
  }

  public void read(long pos, byte dest[], int off, int len) throws IOException {
    seek(pos);
    delegate.readFully(dest, off, len);
    cpos += len;
  }

  public void readShort(long pos, short dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readShort();
    cpos += len * 2;
  }

  public void readInt(long pos, int dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readInt();
    cpos += len * 4;
  }

  public void readLong(long pos, long dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readLong();
    cpos += len * 8;
  }

  public void readFloat(long pos, float dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readFloat();
    cpos += len * 4;
  }

  public void readDouble(long pos, double dest[], int off, int len) throws IOException {
    seek(pos);
    for (int i=0; i<len; i++)
      dest[off+i] = delegate.readDouble();
    cpos += len * 8;
  }
}
