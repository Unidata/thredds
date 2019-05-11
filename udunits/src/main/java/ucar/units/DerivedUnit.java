/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Interface for derived units.
 * @author Steven R. Emmerson
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
    boolean
    isReciprocalOf(DerivedUnit that);

    /**
     * Returns the unit dimension of this derived unit.  For example, the unit
     * "newton" has the unit dimension "kg.m.s-2".
     * @return			The unit dimension of this derived unit.
     */
    UnitDimension
    getDimension();

    /**
     * Return the quantity dimension of this derived unit.  For example, the
     * unit "newton" has the quantity dimension "M.L.t-2").
     * @return			The quantity dimension of this derived unit.
     */
    QuantityDimension
    getQuantityDimension();
}
