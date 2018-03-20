/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

/**
 * Provides support for an unknown base quantity.
 * 
 * @author Steven R. Emmerson
 */
public final class UnknownBaseQuantity extends BaseQuantity {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructs from nothing.
	 */
	protected UnknownBaseQuantity() {
		super("Unknown", "x", true);
	}

	/**
	 * Indicates if this quantity is semantically the same as an object. Unknown
	 * quantities are never equal by definition -- not even to itself.
	 * 
	 * @param object
	 *            The object.
	 * @return <code>false</code> always.
	 */
	@Override
	public boolean equals(final Object object) {
		return false;
	}

	/**
	 * Returns the hash code of this instance.
	 * 
	 * @return The hash code of this instance.
	 */
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	/**
	 * Indicates if this quantity is dimensionless. Unknown quantities are never
	 * dimensionless by definition.
	 * 
	 * @return <code>false</code> always.
	 */
	@Override
	public boolean isDimensionless() {
		return false;
	}
}
