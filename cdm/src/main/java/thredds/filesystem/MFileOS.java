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

import net.jcip.annotations.ThreadSafe;
import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;

/**
 * Implements thredds.inventory.MFile using regular OS files.
 *
 * @author caron
 * @since Jun 30, 2009
 */
@ThreadSafe
public class MFileOS implements MFile {

  /**
   * Make MFileOS if file exists, otherwise return null
   * @param filename
   * @return MFileOS or null
   */
  static public MFileOS getExistingFile(String filename) {
    if (filename == null) return null;
    File file = new File(filename);
    if (file.exists()) return new MFileOS(file);
    return null;
  }

  private final File file;
  private final long lastModified;
  private Object auxInfo;

  public MFileOS(java.io.File file) {
    this.file = file;
    this.lastModified = file.lastModified();
  }

  public MFileOS(String filename) {
    this.file = new File(filename);
    this.lastModified = file.lastModified();
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public long getLength() {
    return file.length();
  }

  @Override
  public boolean isDirectory() {
    return file.isDirectory();
  }

  @Override
  public String getPath() {
    // no microsnot
    return StringUtil2.replace(file.getPath(), '\\', "/");
  }

  @Override
  public String getName() {
    return file.getName();
  }

  @Override
  public MFile getParent() {
    return new MFileOS(file.getParentFile());
  }

  @Override
  public int compareTo(MFile o) {
    return getPath().compareTo( o.getPath());
  }

    @Override
  public Object getAuxInfo() {
    return auxInfo;
  }

  @Override
  public void setAuxInfo(Object auxInfo) {
    this.auxInfo = auxInfo;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MFileOS{");
    sb.append("file=").append(file.getPath());
    sb.append(", lastModified=").append(lastModified);
    sb.append('}');
    return sb.toString();
  }

  public File getFile() {
    return file;
  }
}
