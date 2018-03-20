/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ldm;

import ucar.nc2.iosp.bufr.Message;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 22, 2008
 */
public interface Indexer {

  public boolean writeIndex(short fileno, Message m) throws IOException;

  public void close();

}
