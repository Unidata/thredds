/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import thredds.filesystem.MFileOS;
import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MFile stored in GC index
 *
 * @author caron
 * @since 2/19/14
 */
public class GcMFile implements thredds.inventory.MFile {

  static List<GcMFile> makeFiles(File directory, List<MFile> files, Set<Integer> allFileSet) {
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

  GcMFile(File directory, String name, long lastModified, long length, int index) {
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
   public MFile getParent() {
     return new MFileOS(directory);
   }

  @Override
  public int compareTo(thredds.inventory.MFile o) {
    return name.compareTo(o.getName());
  }

  @Override
  @Nullable
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
    return "GcMFile{" +
        "directory=" + directory +
        ", name='" + name + '\'' +
        ", lastModified=" + lastModified +
        ", length=" + length +
        ", index=" + index +
        '}';
  }
}