// $Id: UnitFormatManager.java 64 2006-07-12 22:30:50Z edavis $
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

import java.io.Serializable;

/**
 * Provides support for managing a default UnitFormat.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitFormatManager.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class
UnitFormatManager
    implements	Serializable
{
    /**
     * The singleton instance of the default unit format.
     * @serial
     */
    private static UnitFormat	instance;

    /**
     * Returns an instance of the default unit format.
     * @return			An instance of the default unit format.
     */
    public static final UnitFormat
    instance()
    {
	if (instance == null)
	{
	    synchronized(UnitFormatManager.class)
	    {
		if (instance == null)
		    instance = StandardUnitFormat.instance();
	    }
	}
	return instance;
    }

    /**
     * Sets the instance of the default unit format.  You'd better know what
     * you're doing if you call this method.
     * @param instance		An instance of the new, default unit format.
     */
    public static final synchronized void
    setInstance(UnitFormat instance)
    {
	UnitFormatManager.instance = instance;
    }
}
