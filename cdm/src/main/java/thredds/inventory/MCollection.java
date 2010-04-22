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

package thredds.inventory;

import net.jcip.annotations.Immutable;

/**
 * Configuration object for a collection of managed files.
 *
 * @author caron
 */
@Immutable
public class MCollection {
  private final String name;
  private final String dirName;
  private final boolean wantSubdirs;
  private final MFileFilter ff;
  //private final DateExtractor dateExtractor;
  private final Object auxInfo;

  /**
   * Constructor
   * @param name name of collection
   * @param dirName top directory name
   * @param wantSubdirs if want subdirectories
   * @param ff optional FilenameFilter (may be null) - applies only to non-directories
   // * @param dateExtractor optional DateExtractor (may be null) - applies only to non-directories
   * @param auxInfo optional info added to each MFile
   */
  public MCollection(String name, String dirName, boolean wantSubdirs, MFileFilter ff, Object auxInfo) {
    this.name = name;
    this.dirName = dirName;
    this.wantSubdirs = wantSubdirs;
    this.ff = ff;
    //this.dateExtractor = dateExtractor;
    this.auxInfo = auxInfo;
  }

  public thredds.inventory.MCollection subdir(MFile child) {
    return new MCollection( name+"/"+child.getName(), dirName+"/"+child.getName(), wantSubdirs, ff, child.getAuxInfo());
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

  /* public DateExtractor getDateExtractor() {
    return dateExtractor;
  } */

  public boolean accept(MFile file) {
    return ((ff == null) || ff.accept(file));
  }


  @Override
  public String toString() {
    return "MCollection{" +
        "name='" + name + '\'' +
        ", dirName='" + dirName + '\'' +
        ", wantSubdirs=" + wantSubdirs +
        ", ff=" + ff +
        // ", dateExtractor=" + dateExtractor +
        '}';
  }


  public Object getAuxInfo() {
    return auxInfo;
  }

}