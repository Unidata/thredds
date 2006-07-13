// $Id: UnitFormatException.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for general failures with unit format classes.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitFormatException.java 64 2006-07-12 22:30:50Z edavis $
 */
public class
UnitFormatException
    extends	UnitException
{
    /**
     * Constructs from nothing.
     */
    public
    UnitFormatException()
    {}

    /**
     * Constructs from an error message.
     * @param message		The error message.
     */
    public
    UnitFormatException(String message)
    {
	super(message);
    }
}
