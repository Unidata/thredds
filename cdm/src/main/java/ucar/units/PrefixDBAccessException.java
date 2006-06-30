// $Id: PrefixDBAccessException.java,v 1.5 2000/08/18 04:17:29 russ Exp $
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
 * Provides support for prefix database access failures.  This is not used
 * in the default implementation but could be used for remote database
 * implementations, for example.
 *
 * @author Steven R. Emmerson
 * @version $Id: PrefixDBAccessException.java,v 1.5 2000/08/18 04:17:29 russ Exp $
 */
public class
PrefixDBAccessException
    extends	PrefixDBException
{
    /**
     * Constructs from an error message.
     * @param reason		The reason the database couldn't be accessed.
     */
    public
    PrefixDBAccessException(String reason)
    {
	super("Couldn't access unit-prefix database: " + reason);
    }
}
