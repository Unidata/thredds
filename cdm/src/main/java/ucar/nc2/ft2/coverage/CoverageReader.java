/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import java.io.Closeable;
import java.io.IOException;

import ucar.ma2.InvalidRangeException;

/**
 * Abstraction to read the data in a coverage.
 * Makes it simpler to implement multiple versions of CoverageDataset + related classes.
 *
 * @author caron
 * @since 7/13/2015
 */
public interface CoverageReader extends Closeable {

  String getLocation();

  GeoReferencedArray readData(Coverage coverage, SubsetParams subset, boolean canonicalOrder) throws IOException, InvalidRangeException;

  // List<ArrayWithCoordinates> readData(List<Coverage> coverage, SubsetParams subset) throws IOException;

}
