// $Id: DivideException.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for unit division failures.
 * @author Steven R. Emmerson
 * @version $Id: DivideException.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class
DivideException
    extends	OperationException
{
    /**
     * Constructs from a unit that can't be divided.
     * @param unit		The unit that can't be divided.
     */
    public
    DivideException(Unit unit)
    {
	super("Can't divide unit \"" + unit + "\"");
    }

    /**
     * Constructs from dividend and divisor units.
     * @param numerator		The unit attempting to be divided.
     * @param denominator	The unit attempting to divide.
     */
    public
    DivideException(Unit numerator, Unit denominator)
    {
	super("Can't divide unit \"" + numerator + "\" by unit \"" + 
	    denominator + "\"");
    }
}
