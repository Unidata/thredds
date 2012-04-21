// $Id: UnitSystemImpl.java 64 2006-07-12 22:30:50Z edavis $
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
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Steven R. Emmerson
 * @version $Id: UnitSystemImpl.java 64 2006-07-12 22:30:50Z edavis $
 */
public class UnitSystemImpl implements UnitSystem, Serializable {
	private static final long						serialVersionUID	= 1L;

	/**
	 * The quantity-to-base-unit map.
	 * 
	 * @serial
	 */
	private final HashMap<BaseQuantity, BaseUnit>	quantityMap;

	/**
	 * The base unit database;
	 * 
	 * @serial
	 */
	private final UnitDB							baseUnitDB;

	/**
	 * The complete database;
	 * 
	 * @serial
	 */
	private final UnitDBImpl						acceptableUnitDB;

	/**
	 * Constructs from a base unit database and a derived unit database.
	 * 
	 * @param baseUnitDB
	 *            The base unit database. Shall only contain base units.
	 * @param derivedUnitDB
	 *            The derived unit database. Shall not contain any base units.
	 * @throws UnitExistsException
	 *             A unit with the same identifier exists in both databases.
	 */
	protected UnitSystemImpl(final UnitDBImpl baseUnitDB,
			final UnitDBImpl derivedUnitDB) throws UnitExistsException {
		quantityMap = new HashMap<BaseQuantity, BaseUnit>(baseUnitDB
				.nameCount());
		for (final Iterator<?> iter = baseUnitDB.getIterator(); iter.hasNext();) {
			final Unit unit = (Unit) iter.next();
			final BaseUnit baseUnit = (BaseUnit) unit;
			quantityMap.put(baseUnit.getBaseQuantity(), baseUnit);
		}
		this.baseUnitDB = baseUnitDB;
		acceptableUnitDB = new UnitDBImpl(baseUnitDB.nameCount()
				+ derivedUnitDB.nameCount(), baseUnitDB.symbolCount()
				+ derivedUnitDB.symbolCount());
		acceptableUnitDB.add(baseUnitDB);
		acceptableUnitDB.add(derivedUnitDB);
	}

	/**
	 * Returns the base unit database.
	 * 
	 * @return The base unit database.
	 */
	public final UnitDB getBaseUnitDB() {
		return baseUnitDB;
	}

	/**
	 * Returns the complete unit database.
	 * 
	 * @return The complete unit database (both base units and derived units).
	 */
	public final UnitDB getUnitDB() {
		return acceptableUnitDB;
	}

	/**
	 * Returns the base unit corresponding to a base quantity.
	 * 
	 * @param quantity
	 *            The base quantity.
	 * @return The base unit corresponding to the base quantity in this system
	 *         of units or <code>
     *				null</code> if no such unit exists.
	 */
	public final BaseUnit getBaseUnit(final BaseQuantity quantity) {
		return quantityMap.get(quantity);
	}
}
