/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;

/**
 * Provides support for unit prefixes (e.g. "centi", "c").
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 */
@Immutable
public abstract class Prefix implements Comparable<Object> {
	/**
	 * The value of this prefix.
	 * 
	 * @serial
	 */
	private final double	value;

	/**
	 * The identifier of this prefix.
	 * 
	 * @serial
	 */
	private final String	id;

	/**
	 * Constructs from an identifier and a value.
	 * 
	 * @param id
	 *            The prefix identifier (e.g. "milli" or "m").
	 * @param value
	 *            The prefix value (e.g. 1e-3).
	 */
	protected Prefix(final String id, final double value) {
		this.id = id;
		this.value = value;
	}

	/**
	 * Gets the prefix identifier.
	 * 
	 * @return The prefix identifier.
	 */
	public final String getID() {
		return id;
	}

	/**
	 * Returns the string representation of this prefix.
	 * 
	 * @return The string representation of this prefix.
	 */
	@Override
	public final String toString() {
		return getID();
	}

	/**
	 * Gets the prefix value.
	 * 
	 * @return The prefix value.
	 */
	public final double getValue() {
		return value;
	}

	/**
	 * Compares this prefix to another.
	 * 
	 * @param obj
	 *            The other prefix.
	 * @return A negative value, zero, or a positive value depending on whether
	 *         this prefix is less than equal to, or greater than
	 *         <code>obj</code>.
	 */
	public abstract int compareTo(Object obj);

	/**
	 * Compares this prefix to a string.
	 * 
	 * @param string
	 *            The string.
	 * @return A negative value, zero, or a positive value depending on whether
	 *         this prefix is less than equal to, or greater than the string.
	 */
	public abstract int compareTo(String string);

	/**
	 * Return the length of the prefix identifier.
	 * 
	 * @return The length of the prefix identifier.
	 */
	public final int length() {
		return id.length();
	}
}
