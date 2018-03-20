/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

/**
 * Provides support for a Base/exponent pair. Instances of this class are
 * immutable.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public final class Factor implements Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * The Base entity.
	 * 
	 * @serial
	 */
	private final Base			_base;

	/**
	 * The exponent.
	 * 
	 * @serial
	 */
	private final int			_exponent;

	/**
	 * Constructs from a Base. The exponent is set to unity.
	 * 
	 * @param base
	 *            The base entity.
	 */
	public Factor(final Base base) {
		this(base, 1);
	}

	/**
	 * Constructs from a Factor and an exponent.
	 * 
	 * @param factor
	 *            The factor.
	 * @param exponent
	 *            The exponent.
	 */
	public Factor(final Factor factor, final int exponent) {
		this(factor.getBase(), exponent);
	}

	/**
	 * Constructs from a Base and an exponent.
	 * 
	 * @param base
	 *            The base entity.
	 * @param exponent
	 *            The exponent.
	 */
	public Factor(final Base base, final int exponent) {
		_base = base;
		_exponent = exponent;
	}

	/**
	 * Returns the Base entity.
	 * 
	 * @return The Base entity.
	 */
	public Base getBase() {
		return _base;
	}

	/**
	 * Returns the identifier of the Base entity.
	 * 
	 * @return The identifier of the Base entity (symbol or name).
	 */
	public String getID() {
		return getBase().getID();
	}

	/**
	 * Returns the exponent of the Base entity.
	 * 
	 * @return The exponent of the Base entity.
	 */
	public int getExponent() {
		return _exponent;
	}

	/**
	 * Raises this Factor to a power.
	 * 
	 * @param power
	 *            The power by which to raise this Factor.
	 * @return The Factor resulting from raising this Factor to the given power.
	 */
	public Factor pow(final int power) {
		return new Factor(getBase(), getExponent() * power);
	}

	/**
	 * Returns the string representation of this Factor.
	 * 
	 * @return The string representation of this Factor.
	 */
	@Override
	public final String toString() {
		return getExponent() == 0
				? ""
				: getExponent() == 1
						? getBase().toString()
						: getBase().toString() + getExponent();
	}

	/**
	 * Indicates if this Factor is semantically identical to another object.
	 * 
	 * @param object
	 *            The object.
	 * @return <code>true</code> if and only if this Factor is semantically
	 *         identical to <code>object<
     *				/code>.
	 */
	@Override
	public boolean equals(final Object object) {
		boolean equals;
		if (this == object) {
			equals = true;
		}
		else if (!(object instanceof Factor)) {
			equals = false;
		}
		else {
			final Factor that = (Factor) object;
			equals = getExponent() != that.getExponent()
					? false
					: getExponent() == 0 || getBase().equals(that.getBase());
		}
		return equals;
	}

	/**
	 * Returns the hash code of this instance.
	 * 
	 * @return The hash code of this instance.
	 */
	@Override
	public int hashCode() {
		return getExponent() == 0
				? getClass().hashCode()
				: getExponent() ^ getBase().hashCode();
	}

	/**
	 * Indicates if this Factor is the reciprocal of another Factor.
	 * 
	 * @param that
	 *            The other factor.
	 * @return <code>true</code> if and only if this Factor is the reciprocal of
	 *         <code>that</code>.
	 */
	public boolean isReciprocalOf(final Factor that) {
		return getBase().equals(that.getBase())
				&& getExponent() == -that.getExponent();
	}

	/**
	 * Indicates if this factor is dimensionless. A Factor is dimensionless if
	 * and only if the exponent is zero or the Base entity is dimensionless.
	 * 
	 * @return <code>true</code> if and only if this Factor is dimensionless.
	 */
	public boolean isDimensionless() {
		return getExponent() == 0 || getBase().isDimensionless();
	}
}
