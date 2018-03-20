/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.Variable;

/**
 * public by accident
 */
public interface ReplaceVariableCheck {
  boolean replace( Variable v);
}
