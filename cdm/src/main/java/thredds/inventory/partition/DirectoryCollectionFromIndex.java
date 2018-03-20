/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.partition;

import thredds.inventory.CollectionAbstract;
import thredds.inventory.DateExtractor;
import thredds.inventory.MFile;
import thredds.inventory.MFileIterator;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;

/**
* A Directory Collection of MFiles, the MFiles are found from an existing ncx2 index
* Scans files only when asked
* @author John
* @since 12/9/13
*/
class DirectoryCollectionFromIndex extends CollectionAbstract {
  final DirectoryBuilder builder;
  final IndexReader indexReader;

  DirectoryCollectionFromIndex(DirectoryBuilder builder, DateExtractor dateExtractor, IndexReader indexReader, org.slf4j.Logger logger) {
    super(builder.getPartitionName(), logger);
    setDateExtractor(dateExtractor);
    setRoot(builder.getDir().toString());
    this.builder = builder;
    this.indexReader = indexReader;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator( getFilesSorted().iterator(), null);
  }

  @Override
  public String getRoot() {
    return builder.getDir().toString();
  }

  @Override
  public void close() { }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return builder.readFilesFromIndex(indexReader);  // LOOK guarenteed sorted ??
  }

}
