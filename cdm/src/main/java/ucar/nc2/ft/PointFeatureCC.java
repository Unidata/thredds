/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.nc2.util.IOIterator;

/**
 * Single nested PointFeatureCollection
 *
 * @author caron
 * @since 9/23/2015.
 */
public interface PointFeatureCC extends DsgFeatureCollection {

  /**
   * General way to handle iterations on all classes that implement this interface.
   * Generally, one uses class specific foreach
   * @return Iterator over PointFeatureCollection which may throw an IOException
   * @throws java.io.IOException
   */
  IOIterator<PointFeatureCollection> getCollectionIterator() throws java.io.IOException;
}
