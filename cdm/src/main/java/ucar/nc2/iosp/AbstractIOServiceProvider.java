/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Structure;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.Formatter;

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

  /**
   * Subclasses that use AbstractIOServiceProvider.open(...) or .close()
   * should use this (instead of their own private variable).
   */
  protected ucar.unidata.io.RandomAccessFile raf;
  protected String location;
  protected int rafOrder = RandomAccessFile.BIG_ENDIAN;
  protected NetcdfFile ncfile;

  @Override
  public void open( RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask ) throws IOException {
    this.raf = raf;
    this.location = (raf != null) ? raf.getLocation() : null;
    this.ncfile = ncfile;
  }

  @Override
  public void close() throws java.io.IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  // release any resources like file handles
  public void release() throws IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    raf = RandomAccessFile.acquire(location);
    this.raf.order(rafOrder);
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

  public long readToOutputStream(ucar.nc2.Variable v2, Section section, OutputStream out)
          throws java.io.IOException, ucar.ma2.InvalidRangeException {

    Array data = readData(v2, section);
    return IospHelper.copyToOutputStream(data,  out);
  }

  public long streamToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
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
    if (message == NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE) {
      return raf;
    }
    return null;
  }

  @Override
  public boolean syncExtend() throws IOException {
    return false;
  }

  /**
   * Returns the time that the underlying file(s) were last modified. If they've changed since they were stored in the
   * cache, they will be closed and reopened with {@link ucar.nc2.util.cache.FileFactory}.
   *
   * @return  a {@code long} value representing the time the file(s) were last modified or {@code 0L} if the
   *          last-modified time couldn't be determined for any reason.
   */
  public long getLastModified() {
    if (location != null) {
      File file = new File(location);
      return file.lastModified();
    } else {
      return 0;
    }
  }

  @Override
  public String toStringDebug(Object o) {
    return "";
  }

  @Override
  public String getDetailInfo() {
    if (raf == null) return "";
    try {
      Formatter fout = new Formatter();
      double size = raf.length() / (1000.0 * 1000.0);
      fout.format(" raf = %s%n", raf.getLocation());
      fout.format(" size= %d (%s Mb)%n%n", raf.length(), Format.dfrac(size, 3));
      return fout.toString();

    } catch (IOException e) {
      return e.getMessage();
    }
  }

  @Override
  public String getFileTypeVersion() {
    return "N/A";
  }

}
