/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.nc2.util.IOIterator;

/**
 * Double nested PointFeatureCollection
 *
 * @author caron
 * @since 9/23/2015.
 */
public interface PointFeatureCCC extends DsgFeatureCollection {

  /**
   * General way to handle iterations on all classes that implement this interface.
   * Generally, use class specific foreach
   * @return Iterator over PointFeatureCC which may throw an IOException
   * @throws java.io.IOException
   */
  IOIterator<PointFeatureCC> getCollectionIterator() throws java.io.IOException;
}
