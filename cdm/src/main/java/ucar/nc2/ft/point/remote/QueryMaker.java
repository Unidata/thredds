/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.remote;

/**
 * Abstraction for making the query to send to the cdmRemote point dataset
 *
 * @author caron
 * @since Jun 15, 2009
 */
public interface QueryMaker {
  String makeQuery();
}
