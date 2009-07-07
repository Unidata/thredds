/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
package thredds.filesystem;

import java.io.*;
import java.util.HashMap;

/**
 * Class Description
 *
 * @author caron
 * @since Jul 6, 2009
 */


public class CacheFileProto implements Externalizable {
  public static int countRead = 0;
  public static int countReadSize = 0;

  public static int countWrite = 0;
  public static int countWriteSize = 0;

  private static final long serialVersionUID = 7526472295622776147L; // disable brittle serialization
  ///////////////////

  protected String shortName;
  protected long lastModified;
  protected long length;
  protected boolean isDirectory;

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }

  public void setLength(long length) {
    this.length = length;
  }

  public void setDirectory(boolean directory) {
    isDirectory = directory;
  }

  private HashMap<String, Object> att; // does this need to be concurrent?

  public String getShortName() {
    return shortName;
  }

  public long getLastModified() {
    return lastModified;
  }

  public long getLength() {
    return length;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setAttribute(String key, Object value) {
    if (att == null) att = new HashMap<String, Object>(5);
    att.put(key,value);
  }

  public Object getAttribute(String key) {
    if (att == null) return null;
    return att.get(key);
  }

  @Override
  public String toString() {
    return "CacheFile{" +
        "shortName='" + shortName + '\'' +
        ", lastModified=" + lastModified +
        ", length=" + length +
        ", isDirectory=" + isDirectory +
        ", att=" + att +
        '}';
  }

  public CacheFileProto( File f) {
    this.shortName = f.getName();
    this.lastModified = f.lastModified();
    this.length = f.length();
    this.isDirectory = f.isDirectory();
  }

  public CacheFileProto() {
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    FileSystemProto.File.Builder fileBuilder = FileSystemProto.File.newBuilder();

    fileBuilder.setName( getShortName());
    fileBuilder.setIsDirectory( isDirectory());
    fileBuilder.setLastModified( getLastModified());
    fileBuilder.setLength( getLength());

    thredds.filesystem.FileSystemProto.File fileProto = fileBuilder.build();
    byte[] b = fileProto.toByteArray();

    out.writeInt(b.length);
    out.write(b);

    countWrite++;
    countWriteSize += b.length + 4;  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int len = in.readInt();
    byte[] b = new byte[len];

    // read fully
    int done = 0;
    while (done < len) {
      int got = in.read(b, done, len-done);
      if (got < 0) throw new IOException();
      done += got;
    }

    FileSystemProto.File proto = FileSystemProto.File.parseFrom(b);

    shortName = proto.getName();
    lastModified = proto.getLastModified();
    length = proto.getLength();
    isDirectory = proto.getIsDirectory();

    countRead++;
    countReadSize += len + 4;
  }
}
