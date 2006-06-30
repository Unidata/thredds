// $Id: RadialVariableAdapter.java,v 1.8 2005/10/20 18:39:41 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt.radial;

import ucar.nc2.dt.*;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.VariableSimpleAdapter;
import ucar.nc2.VariableSimpleIF;

import java.util.List;

/**
 * @author john
 */
public abstract class RadialVariableAdapter extends VariableSimpleAdapter implements RadialDataset.RadialVariable {
  protected RadialCoordSys radialCoordsys;
  protected VariableEnhanced ve;

  public RadialVariableAdapter( VariableEnhanced ve, RadialCoordSys rcys) {
    super(ve);
    this.ve = ve;
    this.radialCoordsys = rcys;
  }

  public RadialCoordSys getRadialCoordSys() {
    return radialCoordsys;
  }

}