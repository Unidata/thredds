/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.VariableSimpleIF;
import java.util.List;

/**
 * Abstract superclass for implementations of ObsFeature.
 *
 * @author caron
 */


public abstract class ObsFeatureImpl implements ObsFeature {
  protected FeatureDataset fd;
  protected DateUnit timeUnit;

  public ObsFeatureImpl( FeatureDataset fd, DateUnit timeUnit) {
    this.fd = fd;
    this.timeUnit = timeUnit;
  }

  public String getDescription() {
    return null;
  }

  public ucar.nc2.units.DateUnit getTimeUnits() { return timeUnit; }

  public List<VariableSimpleIF> getDataVariables() {
    return fd.getDataVariables();
  }

  public VariableSimpleIF getDataVariable(String name) {
    return fd.getDataVariable(name);
  }

}