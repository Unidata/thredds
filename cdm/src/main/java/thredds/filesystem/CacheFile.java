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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Formatter;

/**
 * Lightweight, serializable representation of a java.io.File with arbitrary attributes
 * @author caron
 * @since Mar 21, 2009
 */
public class CacheFile implements Serializable {
  protected String shortName;
  protected long lastModified;
  protected long length;
  protected boolean isDirectory;
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

  public CacheFile() {
  }

  public CacheFile( File f) {
    this.shortName = f.getName();
    this.lastModified = f.lastModified();
    this.length = f.length();
    this.isDirectory = f.isDirectory();
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
    Formatter f = new Formatter();
    f.format("CacheFile{ shortName='%s' lastModified=%d length=%d isDirectory=%s%n", shortName, lastModified, length, isDirectory);
    if (att != null ) {
      f.format(" attributes:%n");
      for (String key : att.keySet()) {
        f.format("  %s = %s %n", key, att.get(key));
      }
    }

    return f.toString();
  }
}
