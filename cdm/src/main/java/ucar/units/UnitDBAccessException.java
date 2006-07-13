// $Id: UnitDBAccessException.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for failure to access unit database (e.g. RMI
 * failure).
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitDBAccessException.java 64 2006-07-12 22:30:50Z edavis $
 */
public class
UnitDBAccessException
    extends	UnitDBException
{
    /**
     * Constructs from a string.
     * @param reason		The reason for the failure.
     */
    public
    UnitDBAccessException(String reason)
    {
	super("Couldn't access unit database: " + reason);
    }
}
