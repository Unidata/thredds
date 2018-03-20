/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.cache;

import ucar.nc2.dataset.DatasetUrl;

/**
 * Interface for factories of FileCacheable objects.
 *
 * @author caron
 * @since Jun 2, 2008
 */
public interface FileFactory {
  FileCacheable open(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws java.io.IOException;
}
