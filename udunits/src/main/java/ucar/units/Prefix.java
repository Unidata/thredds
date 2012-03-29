// $Id: Prefix.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.units;

/**
 * Provides support for unit prefixes (e.g. "centi", "c").
 * 
 * Instances of this class are immutable.
 * 
 * @author Steven R. Emmerson
 * @version $Id: Prefix.java 64 2006-07-12 22:30:50Z edavis $
 */
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
