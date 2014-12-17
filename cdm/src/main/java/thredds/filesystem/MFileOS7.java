/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.filesystem;

import net.jcip.annotations.ThreadSafe;
import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Use Java 7 nio Paths
 *
 * @author caron
 * @since 11/16/13
 */

@ThreadSafe
public class MFileOS7 implements MFile {

  /**
   * Make MFileOS7 if file exists, otherwise return null
   * @param filename full path name
   * @return MFileOS or null
   */
  static public MFileOS7 getExistingFile(String filename) throws IOException {
    if (filename == null) return null;
    Path path = Paths.get(filename);
    if (Files.exists(path)) return new MFileOS7(path);
    return null;
  }

  private final Path path;
  private final BasicFileAttributes attr;
  private Object auxInfo;

  public MFileOS7(Path path) throws IOException {
    this.path = path;
    this.attr =  Files.readAttributes(path, BasicFileAttributes.class);
  }

  public MFileOS7(Path path, BasicFileAttributes attr) {
    this.path = path;
    this.attr =  attr;
  }

  public MFileOS7(String filename) throws IOException {
    this.path = Paths.get(filename);
    this.attr =  Files.readAttributes(path, BasicFileAttributes.class);
  }

  @Override
  public long getLastModified() {
    return attr.lastModifiedTime().toMillis();
  }

  @Override
  public long getLength() {
    return attr.size();
  }

  @Override
  public boolean isDirectory() {
    return attr.isDirectory();
  }

  @Override
  public String getPath() {
    // no microsnot
    return StringUtil2.replace(path.toString(), '\\', "/");
  }

  @Override
  public String getName() {
    return path.getFileName().toString();
  }

  @Override
   public MFile getParent() throws IOException {
     return new MFileOS7(path.getParent());
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
    return getPath();
  }

  public Path getNioPath() {
    return path;
  }
}
