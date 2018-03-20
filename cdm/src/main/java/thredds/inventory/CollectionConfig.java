/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import javax.annotation.concurrent.Immutable;

/**
 * Configuration object for a collection of managed files.
 *
 * @author caron
 */
@Immutable
public class CollectionConfig {
  private final String name;
  private final String dirName;
  private final boolean wantSubdirs;
  private final MFileFilter ff;
  private final Object auxInfo;

  /**
   * Constructor
   * @param name name of collection
   * @param dirName top directory name
   * @param wantSubdirs if want subdirectories
   * @param filters optional list of MFileFilter (may be null) - applies only to non-directories, assumed include OR include ....
   * @param auxInfo optional info added to each MFile
   *
  public CollectionConfig(String name, String dirName, boolean wantSubdirs, List<MFileFilter> filters, Object auxInfo) {
    this.name = name;
    this.dirName = dirName;
    this.wantSubdirs = wantSubdirs;
    ff = (filters == null || filters.size() == 0) ? null : ((filters.size() == 1) ? filters.get(0) : new CompositeMFileFilter(filters));
    this.auxInfo = auxInfo;
  } */

  /**
   * Constructor
   * @param name name of collection
   * @param dirName top directory name
   * @param wantSubdirs if want subdirectories
   * @param ff optional FilenameFilter (may be null) - applies only to non-directories
   * @param auxInfo optional info added to each MFile
   */
  public CollectionConfig(String name, String dirName, boolean wantSubdirs, MFileFilter ff, Object auxInfo) {
    this.name = name;
    this.dirName = dirName;
    this.wantSubdirs = wantSubdirs;
    this.ff = ff;
    this.auxInfo = auxInfo;
  }

  public CollectionConfig subdir(MFile child) {
    return new CollectionConfig( name+"/"+child.getName(), dirName+"/"+child.getName(), wantSubdirs, ff, child.getAuxInfo());
  }

  public String getName() {
    return name;
  }

  public String getDirectoryName() {
    return dirName;
  }

  public boolean wantSubdirs() {
    return wantSubdirs;
  }

  public MFileFilter getFileFilter() {
    return ff;
  }

  public boolean accept(MFile file) {
    return ((ff == null) || ff.accept(file));
  }


  @Override
  public String toString() {
    return "MCollection{" +
        "name='" + name + '\'' +
        ", dirName='" + dirName + '\'' +
        ", wantSubdirs=" + wantSubdirs +
        ", filter=" + ff +
        '}';
  }


  public Object getAuxInfo() {
    return auxInfo;
  }

}
