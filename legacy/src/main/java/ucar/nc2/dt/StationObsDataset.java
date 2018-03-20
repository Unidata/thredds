/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import java.io.Closeable;

/** A collection of Stations with StationObsDatatype data.
 * All getData() methods return List of StationObsDatatype.
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */
public interface StationObsDataset extends ucar.nc2.dt.PointObsDataset, ucar.nc2.dt.StationCollection, Closeable {

}
