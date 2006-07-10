// $Id: UnitClassException.java,v 1.5 2000/08/18 04:17:34 russ Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * Provides support for a Unit that is an instance of an unknown class.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitClassException.java,v 1.5 2000/08/18 04:17:34 russ Exp $
 */
public final class
UnitClassException
    extends	UnitFormatException
{
    /**
     * Constructs from an error message.
     * @param msg		The error message.
     */
    private
    UnitClassException(String msg)
    {
	super(msg);
    }

    /**
     * Constructs from the unit that's an instance of an unknown class.
     * @param unit		The unknown unit.
     */
    public
    UnitClassException(Unit unit)
    {
	this("\"" + unit.getClass().getName() + "\" is an unknown unit class");
    }
}
