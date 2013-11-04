/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A CollectionManager consisting of a single file
 *
 * @author caron
 * @since 12/23/11
 */
public class CollectionManagerSingleFile extends CollectionManagerAbstract {
  private MFile mfile;

  public CollectionManagerSingleFile(MFile file, org.slf4j.Logger logger) {
    super(file.getPath(), logger);
    this.mfile = file;
  }

  @Override
  public String getRoot() {
    String path = mfile.getPath();
    int pos = path.lastIndexOf("/");
    if (pos >=0)
      return path.substring(0,pos);

    // otherwise it must be a path reletive to working directory
    return System.getProperty("user.dir");
  }

  @Override
  public long getLastScanned() {
    return System.currentTimeMillis();
  }

  @Override
  public long getLastChanged() {
    return mfile.getLastModified();
  }

  @Override
  public boolean isScanNeeded() {
    return false;
  }

  @Override
  public boolean scanIfNeeded() throws IOException {
    return false;
  }

  @Override
  public boolean scan(boolean sendEvent) throws IOException {
    return false;
  }

  @Override
  public Iterable<MFile> getFiles() {
    List<MFile> list = new ArrayList<MFile>(1);
    list.add(mfile);
    return list;
  }

  @Override
  public void setFiles(Iterable<MFile> files) {
    int count = 0;
    for (MFile f : files) {
      this.mfile = f;
      count++;
    }
    assert count == 1;
  }

  @Override
  public CalendarDate extractRunDate(MFile mfile) {
    return null;
  }

  @Override
  public boolean hasDateExtractor() {
    return false;
  }

  @Override
  public CalendarDate getStartCollection() {
    return null;
  }
}
