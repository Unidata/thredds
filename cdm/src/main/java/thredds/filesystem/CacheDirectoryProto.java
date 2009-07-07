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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Lightweight, serializable representation of a java.io.File directory
 * @author caron
 * @since Mar 21, 2009
 */
public class CacheDirectoryProto implements Externalizable {
  private static final long serialVersionUID = 7526472295622776147L; // disable brittle serialization

  protected String path;
  protected long lastModified;
  private CacheFileProto[] children;

  private HashMap<String, Object> att; // does this need to be concurrent?

  public String getPath() {
    return path;
  }

  public long getLastModified() {
    return lastModified;
  }

  public CacheDirectoryProto() {
  }

  public CacheDirectoryProto(File dir) {
    this.path = dir.getPath();
    this.lastModified = dir.lastModified();

    File[] subs = dir.listFiles();
    if (subs == null) subs = new File[0];
    children = new CacheFileProto[subs.length];
    int count = 0;
    for (File f : subs) {
      children[count++] = new CacheFileProto(f);
    }
  }

  public boolean notModified() {
    File f = new File(getPath());
    return f.lastModified() <= lastModified;
  }

  public CacheFileProto[] getChildren() {
    return children;
  }

  @Override
  public String toString() {
    return "CacheDirectoryProto {" +
            "path='" + path + '\'' +
            ", num children=" + (children == null ? 0 : children.length) +
            '}';
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    FileSystemProto.Directory.Builder dirBuilder = FileSystemProto.Directory.newBuilder();

    dirBuilder.setPath( getPath());
    dirBuilder.setLastModified( getLastModified());

    FileSystemProto.File.Builder fileBuilder = FileSystemProto.File.newBuilder();
    for (CacheFileProto child : children) {
      fileBuilder.clear();
      fileBuilder.setName( child.getShortName());
      fileBuilder.setIsDirectory( child.isDirectory());
      fileBuilder.setLastModified( child.getLastModified());
      fileBuilder.setLength( child.getLength());

      dirBuilder.addFiles(fileBuilder);
    }

    thredds.filesystem.FileSystemProto.Directory dirProto = dirBuilder.build();
    byte[] b = dirProto.toByteArray();

    out.writeInt(b.length);
    out.write(b);
  }

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

    FileSystemProto.Directory proto = FileSystemProto.Directory.parseFrom(b);

    path = proto.getPath();
    lastModified = proto.getLastModified();
    List<FileSystemProto.File> files =proto.getFilesList();
    children = new CacheFileProto[files.size()];
    for (int i=0; i< files.size(); i++) {
      FileSystemProto.File fp = files.get(i);
      CacheFileProto cf = new CacheFileProto();
      cf.setShortName( fp.getName());
      cf.setDirectory( fp.getIsDirectory());
      cf.setLastModified( fp.getLastModified());
      cf.setLength( fp.getLength());

      children[i] = cf;
    }
  }

}
