// $Id: UnitSystemManager.java,v 1.5 2000/08/18 04:17:39 russ Exp $
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

import java.io.Serializable;

/**
 * Provides support for managing a UnitSystem.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitSystemManager.java,v 1.5 2000/08/18 04:17:39 russ Exp $
 */
public final class
UnitSystemManager
    implements	Serializable
{
    /**
     * The singleton instance of the system of units.
     * @serial
     */
    private static UnitSystem	instance;

    /**
     * Returns an instance of the system of units.
     * @return			An instance of the system of units.
     */
    public static final UnitSystem
    instance()
	throws UnitSystemException
    {
	if (instance == null)
	{
	    synchronized(UnitSystemManager.class)
	    {
		if (instance == null)
		    instance = SI.instance();
	    }
	}
	return instance;
    }

    /**
     * Sets the system of units.  This must be called before any
     * call to <code>instance()</code>.
     * @param instance		The system of units.
     * @throws UnitSystemException	<code>instance()</code> was called
     *					earlier.
     */
    public static final synchronized void
    setInstance(UnitSystem instance)
	throws UnitSystemException
    {
	if (instance != null)
	    throw new UnitSystemException("Unit system already used");
	UnitSystemManager.instance = instance;
    }
}
