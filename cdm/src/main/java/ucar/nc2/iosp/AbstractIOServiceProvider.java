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

package ucar.nc2.iosp;

import ucar.ma2.*;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Structure;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CancelTask;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.ByteBuffer;

/**
 * Abstract base class for IOSP implementations that provides default implementations
 * of readToByteChannel(...) and readSection(...).
 *
 * <p>Implementations should make sure to handle the RandomAccessFile properly by
 * doing one of the following:
 *
 * <ol>
 *   <li> Write your own open(...) and close() methods that keep track of the
 *     RandomAccessFile, be sure to close the RandomAccessFile in your close()
 *     method.</li>
 *   <li> Write your own open(...) and close() methods that call the open(...)
 *     and close() methods defined here, use the "raf" variable also defined
 *     here.</li>
 *   <li> Don't write an open(...) or close() method, so that those defined
 *     here are used.</li>
 * </ol>
 *
 */
public abstract class AbstractIOServiceProvider implements IOServiceProvider {

  // a no-op but leave it in in case we change our minds
  static public String createValidNetcdfObjectName(String name) {
    return name;
  }

  /**
   * Subclasses that use AbstractIOServiceProvider.open(...) or .close()
   * should use this (instead of their own private variable).
   */
  protected ucar.unidata.io.RandomAccessFile raf;

  @Override
  public void open( RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask )
          throws IOException
  {
    this.raf = raf;
  }

  @Override
  public void close() throws java.io.IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  // default implementation, reads into an Array, then writes to WritableByteChannel
  // subclasses should override if possible
  // LOOK DataOutputStream uses big-endian
  @Override
  public long readToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    Array data = readData(v2, section);
    return IospHelper.copyToByteChannel(data,  channel);
  }

  @Override
  public ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException {
    return IospHelper.readSection(cer);  //  IOSPs can optimize by overriding
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    return null;
  }

  @Override
  public Object sendIospMessage(Object message) {
    return null;
  }

  @Override
  public boolean syncExtend() throws IOException {
    return false;
  }

  @Override
  public boolean sync() throws IOException {
    return false;
  }

  @Override
  public String toStringDebug(Object o) {
    return "";
  }

  @Override
  public String getDetailInfo() {
    return "";
  }

  @Override
  public String getFileTypeVersion() {
    return "N/A";
  }

}
