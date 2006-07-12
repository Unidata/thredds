// $Id$
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
 * Provides support for the general class of prefix database failures.
 *
 * @author Steven R. Emmerson
 * @version $Id$
 */
public class
PrefixDBException
    extends	UnitException
{
    /**
     * Constructs from nothing.
     */
    public
    PrefixDBException()
    {
	super("Prefix database exception");
    }

    /**
     * Constructs from an error message.
     * @param message		The error message.
     */
    public
    PrefixDBException(String message)
    {
	super("Prefix database exception: " + message);
    }

    /**
     * Constructs from the exception that caused this exception to be thrown.
     * @param e			The exception that caused this exception 
     *				to be thrown.
     */
    public
    PrefixDBException(Exception e)
    {
	this("Prefix database exception", e);
    }

    /**
     * Constructs from an error message and the exception that caused
     * this exception to be thrown.
     * @param message		The error message.
     * @param e			The exception that caused this exception 
     *				to be thrown.
     */
    public
    PrefixDBException(String message, Exception e)
    {
	super(message, e);
    }
}
