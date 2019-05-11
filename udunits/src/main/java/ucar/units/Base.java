/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Interface for "base" entities like base units or base quantities.
 * 
 * @author Steven R. Emmerson
 */
public interface Base {
    /**
     * Indicates if this base entity is dimensionless.
     * 
     * @return <code>true</code> if and only if the base entity is dimensionless
     *         (e.g. (BaseQuantity.SOLID_ANGLE</code>).
     */
    boolean isDimensionless();

    /**
     * Returns the identifier for the base entity.
     * 
     * @return The base entity's identifier (i.e. symbol or name).
     */
    String getID();

    /**
     * Indicates if this base entity is semantically the same as another object.
     * 
     * @param object
     *            The other object.
     * @return <code>true</code> if and only if this base entity is semantically
     *         the same as <code>object</code>.
     */
    boolean equals(Object object);
}
