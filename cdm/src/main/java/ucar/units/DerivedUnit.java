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
 * Interface for derived units.
 * @author Steven R. Emmerson
 * @version $Id$
 */
public interface
DerivedUnit
    extends	Unit
{
    /**
     * Indicates if this derived unit is the reciprocal of another derived
     * unit (e.g. "second" and "hertz").
     * @param that		The other, derived unit.
     */
    public boolean
    isReciprocalOf(DerivedUnit that);

    /**
     * Returns the unit dimension of this derived unit.  For example, the unit
     * "newton" has the unit dimension "kg.m.s-2".
     * @return			The unit dimension of this derived unit.
     */
    public UnitDimension
    getDimension();

    /**
     * Return the quantity dimension of this derived unit.  For example, the
     * unit "newton" has the quantity dimension "M.L.t-2").
     * @return			The quantity dimension of this derived unit.
     */
    public QuantityDimension
    getQuantityDimension();
}
