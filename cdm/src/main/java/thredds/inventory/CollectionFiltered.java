/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A decorator to filter an MCollection
 *
 * @author caron
 * @since 3/12/2015
 */
public class CollectionFiltered extends CollectionAbstract {
  private MCollection org;
  private MFileFilter filter;

  public CollectionFiltered(String name, MCollection org, MFileFilter filter) {
    super(name, null);
    this.org = org;
    this.filter = filter;
    setRoot( org.getRoot());
  }

  @Override
  public void close() {
    org.close();
  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    List<MFile> list = new ArrayList<>(100);
    try (CloseableIterator<MFile> iter = getFileIterator()) {
      while (iter.hasNext()) {
        list.add(iter.next());
      }
    }
    if (hasDateExtractor()) {
      Collections.sort(list, new DateSorter());  // sort by date
    } else {
      Collections.sort(list);                    // sort by name
    }
    return list;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator(org.getFileIterator(), filter);
  }
}
