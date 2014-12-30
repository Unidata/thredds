/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.collection;

import thredds.filesystem.MFileOS;
import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * MFile stored in GC index
 *
 * @author caron
 * @since 2/19/14
 */
public class GcMFile implements thredds.inventory.MFile {

  public static List<GcMFile> makeFiles(File directory, List<MFile> files, Set<Integer> allFileSet) {
    List<GcMFile> result = new ArrayList<>(files.size());
    String dirPath = StringUtil2.replace(directory.getPath(), '\\', "/");

    for (int index : allFileSet) {
      MFile file = files.get(index);
      String filename;
      if (file.getPath().startsWith(dirPath)) {
        filename = file.getPath().substring(dirPath.length());
        if (filename.startsWith("/")) filename = filename.substring(1);
      } else
        filename = file.getPath();  // when does this happen ??
      result.add( new GcMFile(directory, filename, file.getLastModified(), file.getLength(), index));
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  public final File directory;
  public final String name;
  public final long lastModified, length;
  public final int index;

  public GcMFile(File directory, String name, long lastModified, long length, int index) {
    this.directory = directory;
    this.name = name;
    this.lastModified = lastModified;
    this.index = index;
    this.length = length;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public long getLength() {
    return length;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public String getPath() {
    String path =  new File(directory, name).getPath();
    return StringUtil2.replace(path, '\\', "/");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
   public MFile getParent() throws IOException {
     return new MFileOS(directory);
   }

  @Override
  public int compareTo(thredds.inventory.MFile o) {
    return name.compareTo(o.getName());
  }

  @Override
  public Object getAuxInfo() {
    return null;
  }

  @Override
  public void setAuxInfo(Object info) {

  }

  public File getDirectory() {
    return directory;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("GcMFile");
    sb.append("{directory=").append(directory);
    sb.append(", name='").append(name).append('\'');
    sb.append(", lastModified=").append( new Date(lastModified));
    sb.append(", size=").append( length);
    sb.append('}');
    return sb.toString();
  }
}