/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ldm;

import ucar.nc2.iosp.bufr.Message;

import java.util.List;
import java.io.IOException;

/**
 * encapsolates writing an index
 *
 * @author caron
 * @since Aug 21, 2008
 */
public class IndexerTask {
  List<Message> mlist;
  Indexer indexer;
  short fileno;

  public void process() throws IOException {
    if (indexer == null) return;
    for (Message m : mlist)
      indexer.writeIndex(fileno, m);
  }
}
