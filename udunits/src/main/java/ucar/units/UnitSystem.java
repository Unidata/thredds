/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Interface for a system of units.
 *
 * @author Steven R. Emmerson
 */
public interface
UnitSystem
{
    /**
     * Returns the database of base units.
     * @return			The database of base units.
     */
    UnitDB
    getBaseUnitDB();

    /**
     * Returns the complete database of units (base units and 
     * derived units acceptable for use in the system of units.
     * @return			The complete database of units.
     */
    UnitDB
    getUnitDB();

    /**
     * Returns the base unit corresponding to a base quantity.
     * @param quantity		A base quantity.
     * @return			The base unit corresponding to the base
     *				quantity or <code>null</code> if no such
     *				unit exists.
     */
    BaseUnit
    getBaseUnit(BaseQuantity quantity);
}
