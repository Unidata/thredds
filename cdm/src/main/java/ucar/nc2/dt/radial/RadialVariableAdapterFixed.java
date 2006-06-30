// $Id: RadialVariableAdapterFixed.java,v 1.4 2005/05/11 19:58:10 caron Exp $
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

/**
 * @author john
 */
public abstract class RadialVariableAdapterFixed extends RadialVariableAdapter
    implements RadialDatasetFixed.RadialVariableFixed {
  
  protected RadialCoordSys radialCoordsys;

  public RadialVariableAdapterFixed( VariableEnhanced ve, RadialCoordSys rcys) {
    super(ve, rcys);
    this.ve = ve;
    this.radialCoordsys = rcys;
  }

  public abstract class RadialSweep implements RadialDatasetFixed.Sweep {

    public float getMeanElevation() {
      return 0;
    }

  }
}

/* Change History:
   $Log: RadialVariableAdapterFixed.java,v $
   Revision 1.4  2005/05/11 19:58:10  caron
   add VariableSimpleIF, remove TypedDataVariable

   Revision 1.3  2005/04/21 01:34:04  caron
   clean up radar adapters

   Revision 1.2  2005/03/19 00:06:16  caron
   no message

   Revision 1.1  2005/03/15 23:28:49  caron
   Radial Datasets
   version 08

   Revision 1.2  2005/03/07 20:49:23  caron
   *** empty log message ***

   Revision 1.1  2005/03/07 20:48:31  caron
   no message

*/