// $Id: HasNetcdfVariable.java,v 1.2 2005/07/27 23:25:37 caron Exp $
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

package dods.servers.netcdf;

import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Tag that Object has a netcdf variable, and the data can be set externally by an Array.
 *
 * @version $Revision: 1.2 $
 * @author jcaron
 */
public interface HasNetcdfVariable {

    /** reset the underlying proxy */
  public void setData(Array data);

  /** get the underlying proxy */
  public Variable getVariable();

  // for structure members
  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException;
}
