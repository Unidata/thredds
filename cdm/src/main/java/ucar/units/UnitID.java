// $Id: UnitID.java 64 2006-07-12 22:30:50Z edavis $
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

import java.io.Serializable;

/**
 * Provides support for unit identifiers.
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnitID.java 64 2006-07-12 22:30:50Z edavis $
 */
public abstract class UnitID implements Serializable {
	private static final long	serialVersionUID	= 1L;

	/**
	 * Factory method for constructing an identifier from a name, plural, and
	 * symbol.
	 * 
	 * @param name
	 *            The name for the unit. May be <code>null
     *				</code>.
	 * @param plural
	 *            The plural form of the name. If <code>null
     *				</code> and
	 *            <code>name</code> is non-<code>
     *				null</code>, then regular
	 *            plural-forming rules are used on the name.
	 * @param symbol
	 *            The symbol for the unit. May be <code>null
     *				</code>.
	 */
	public static UnitID newUnitID(final String name, final String plural,
			final String symbol) {
		UnitID id;
		try {
			id = name == null
					? (UnitID) new UnitSymbol(symbol)
					: (UnitID) UnitName.newUnitName(name, plural, symbol);
		}
		catch (final NameException e) {
			id = null; // can't happen
		}
		return id;
	}

	/**
	 * Returns the name of the unit.
	 * 
	 * @return The name of the unit. May be <code>null</code>.
	 */
	public abstract String getName();

	/**
	 * Returns the plural form of the name of the unit.
	 * 
	 * @return The plural form of the name of the unit. May be <code>null</code>
	 *         .
	 */
	public abstract String getPlural();

	/**
	 * Returns the symbol for the unit.
	 * 
	 * @return The symbol for the unit. May be <code>null</code>.
	 */
	public abstract String getSymbol();

	/**
	 * Returns the string representation of this identifier.
	 * 
	 * @return The string representation of this identifier.
	 */
	@Override
	public abstract String toString();
}
