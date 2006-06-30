// $Id: PrefixDBManager.java,v 1.5 2000/08/18 04:17:30 russ Exp $
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
 * Provides support for managing a database of unit prefixes.
 *
 * @author Steven R. Emmerson
 * @version $Id: PrefixDBManager.java,v 1.5 2000/08/18 04:17:30 russ Exp $
 */
public final class
PrefixDBManager
    implements	Serializable
{
    /**
     * @serial
     */
    private static PrefixDB	instance;

    /**
     * Gets the current prefix database.
     * @return			The current prefix database.
     * @throws PrefixDBException	The current prefix database couldn't 
     *					be created.
     */
    public static final PrefixDB
    instance()
	throws PrefixDBException
    {
	if (instance == null)
	{
	    synchronized(PrefixDBManager.class)
	    {
		if (instance == null)
		{
		    instance = StandardPrefixDB.instance();
		}
	    }
	}
	return instance;
    }

    /**
     * Sets the current prefix database.
     * @param instance		The prefix database to be made the current one.
     */
    public static final synchronized void
    setInstance(PrefixDB instance)
    {
	PrefixDBManager.instance = instance;
    }
}
