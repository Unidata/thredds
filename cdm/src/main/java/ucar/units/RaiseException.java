// $Id: RaiseException.java,v 1.5 2000/08/18 04:17:31 russ Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.units;

/**
 * Provides support for failure to raise a unit to a power.
 *
 * @author Steven R. Emmerson
 * @version $Id: RaiseException.java,v 1.5 2000/08/18 04:17:31 russ Exp $
 */
public final class
RaiseException
    extends	OperationException
{
    /**
     * Constructs from the unit that couldn't be raised to a power.
     * @param unit		The unit that couldn't be raised to a power.
     */
    public
    RaiseException(Unit unit)
    {
	super("Can't exponentiate unit \"" + unit + "\"");
    }
}
