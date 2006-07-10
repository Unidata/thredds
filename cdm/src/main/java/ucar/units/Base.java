// $Id: Base.java,v 1.5 2000/08/18 04:17:25 russ Exp $
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
 * Interface for "base" entities like base units or base quantities.
 * @author Steven R. Emmerson
 * @version $Id: Base.java,v 1.5 2000/08/18 04:17:25 russ Exp $
 */
public interface
Base
{
    /**
     * Indicates if this base entity is dimensionless.
     * @return			<code>true</code> if and only if the base
     *				entity is dimensionless (e.g. 
     *				(BaseQuantity.SOLID_ANGLE</code>).
     */
    public boolean
    isDimensionless();

    /**
     * Returns the identifier for the base entity.
     * @return			The base entity's identifier (i.e. symbol or
     *				name).
     */
    public String
    getID();

    /**
     * Indicates if this base entity is semantically the same as another object.
     * @param object		The other object.
     * @return			<code>true</code> if and only if this base
     *				entity is semantically the same as
     *				<code>object</code>.
     */
    public boolean
    equals(Object object);
}
