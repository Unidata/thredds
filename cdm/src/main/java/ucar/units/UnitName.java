// $Id: UnitName.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for unit names.
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnitName.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class UnitName extends UnitID implements Comparable<Object> {
	private static final long	serialVersionUID	= 1L;

	/**
	 * The name of the unit.
	 * 
	 * @serial
	 */
	private final String		name;

	/**
	 * The plural form of the unit name.
	 * 
	 * @serial
	 */
	private final String		plural;

	/**
	 * The symbol for the unit.
	 * 
	 * @serial
	 */
	private final String		symbol;

	/**
	 * Constructs from a name and a symbol. Regular rules are use to construct
	 * the plural form of the unit name.
	 * 
	 * @param name
	 *            The name of the unit. Shall not be <code>
     *				null</code>.
	 * @param symbol
	 *            The symbol for the unit. May be <code>null
     *				</code>.
	 * @throws NameException
	 *             <code>name == null</code>.
	 */
	protected UnitName(final String name, final String symbol)
			throws NameException {
		this(name, null, symbol);
	}

	/**
	 * Constructs from a name, a plural form of the unit name, and a symbol.
	 * 
	 * @param name
	 *            The name of the unit. Shall not be <code>
     *				null</code>.
	 * @param plural
	 *            The plural form of the name. May be <code>
     *				null</code>, in which
	 *            case regular plural- forming rules are used to construct the
	 *            plural form from the name.
	 * @param symbol
	 *            The symbol for the unit. May be <code>null
     *				</code>.
	 * @throws NameException
	 *             <code>name == null</code>.
	 */
	protected UnitName(final String name, final String plural,
			final String symbol) throws NameException {
		if (name == null) {
			throw new NameException("Unit name can't be null");
		}
		this.name = name;
		this.plural = plural == null
				? makePlural(name)
				: plural;
		this.symbol = symbol;
	}

	/**
	 * Factory method for constructing a UnitName from a name.
	 * 
	 * @param name
	 *            The name of the unit. Shall not be <code>
     *				null</code>.
	 * @throws NameException
	 *             <code>name == null</code>.
	 */
	public static UnitName newUnitName(final String name) throws NameException {
		return newUnitName(name, null);
	}

	/**
	 * Factory method for constructing a UnitName from a name and a plural form
	 * of the name.
	 * 
	 * @param name
	 *            The name of the unit. Shall not be <code>
     *				null</code>.
	 * @param plural
	 *            The plural form of the name. May be <code>
     *				null</code>, in which
	 *            case regular plural- forming rules are used to construct the
	 *            plural form from the name.
	 * @throws NameException
	 *             <code>name == null</code>.
	 */
	public static UnitName newUnitName(final String name, final String plural)
			throws NameException {
		return newUnitName(name, plural, null);
	}

	/**
	 * Factory method for constructing a UnitName from a name, a plural form of
	 * the name, and a symbol.
	 * 
	 * @param name
	 *            The name of the unit. Shall not be <code>
     *				null</code>.
	 * @param plural
	 *            The plural form of the name. May be <code>
     *				null</code>, in which
	 *            case regular plural- forming rules are used to construct the
	 *            plural form from the name.
	 * @param symbol
	 *            The symbol for the unit. May be <code>null
     *				</code>.
	 * @throws NameException
	 *             <code>name == null</code>.
	 */
	public static UnitName newUnitName(final String name, final String plural,
			final String symbol) throws NameException {
		return new UnitName(name, plural, symbol);
	}

	/**
	 * Returns the name.
	 * 
	 * @return The name. Won't be <code>null</code>.
	 */
	@Override
	public final String getName() {
		return name;
	}

	/**
	 * Returns the plural form of the unit name.
	 * 
	 * @return The plural form of the unit name.
	 */
	@Override
	public String getPlural() {
		return plural;
	}

	/**
	 * Returns the symbol.
	 * 
	 * @return The symbol. Might be <code>null</code>.
	 */
	@Override
	public final String getSymbol() {
		return symbol;
	}

	/**
	 * Returns the string representation of this identifier.
	 * 
	 * @return The string representation of this identifier.
	 */
	@Override
	public final String toString() {
		final String string = getSymbol();
		return string == null
				? getName()
				: string;
	}

	/**
	 * Compares this UnitName with another UnitName.
	 */
	public final int compareTo(final Object object) {
		return getName().compareToIgnoreCase(((UnitName) object).getName());
	}

	/**
	 * Indicates if this UnitName is semantically identical to an object.
	 */
	@Override
	public final boolean equals(final Object object) {
		return object instanceof UnitName && compareTo(object) == 0;
	}

	/**
	 * Returns the hash code of this instance.
	 * 
	 * @return The hash code of this instance.
	 */
	@Override
	public int hashCode() {
		return getName().toLowerCase().hashCode();
	}

	/**
	 * Returns the plural form of a name. Regular rules are used to generate the
	 * plural form.
	 * 
	 * @param name
	 *            The name.
	 * @return The plural form of the name.
	 */
	protected String makePlural(final String name) {
		String plural;
		final int length = name.length();
		final char lastChar = name.charAt(length - 1);
		if (lastChar != 'y') {
			plural = name
					+ (lastChar == 's' || lastChar == 'x' || lastChar == 'z'
							|| name.endsWith("ch")
							? "es"
							: "s");
		}
		else {
			if (length == 1) {
				plural = name + "s";
			}
			else {
				final char penultimateChar = name.charAt(length - 2);
				plural = (penultimateChar == 'a' || penultimateChar == 'e'
						|| penultimateChar == 'i' || penultimateChar == 'o' || penultimateChar == 'u')
						? name + "s"
						: name.substring(0, length - 1) + "ies";
			}
		}
		return plural;
	}
}
