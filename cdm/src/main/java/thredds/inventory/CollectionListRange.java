/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection defined by a list of files, with a [start, end) date range
 *
 * @author caron
 * @since 12/23/2014
 */
public class CollectionListRange extends CollectionAbstract {
  private final List<MFile> mfiles = new ArrayList<>();
  private final CalendarDate startDate, endDate;

  public CollectionListRange(String collectionName, String root, CalendarDate startDate, CalendarDate endDate, Logger logger) {
    super(collectionName, logger);
    setRoot(root);
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public CalendarDate getStartDate() {
    return startDate;
  }

  public CalendarDate getEndDate() {
    return endDate;
  }

  public void addFile(MFile mfile) {
    mfiles.add(mfile);
  }

  @Override
  public Iterable<MFile> getFilesSorted() {
    return mfiles;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator(mfiles.iterator(), null);
  }

  @Override
  public void close() {  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("collectionName", collectionName)
            .add("startDate", startDate)
            .add("endDate", endDate)
            .toString();
  }
}
