/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.filesystem;

import thredds.inventory.MFile;
import ucar.unidata.util.StringUtil2;

import javax.annotation.concurrent.ThreadSafe;
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
