// $Id: BaseQuantity.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for abstract base quantities (ex: length, time).
 * 
 * @author Steven R. Emmerson
 * @version $Id: BaseQuantity.java 64 2006-07-12 22:30:50Z edavis $
 */
public abstract class BaseQuantity implements Base, Comparable<BaseQuantity>,
		Serializable {
	private static final long						serialVersionUID	= 1L;

	/**
	 * The base quantity of amount of substance.
	 */
	public static final RegularBaseQuantity			AMOUNT_OF_SUBSTANCE;

	/**
	 * The base quantity of electric current.
	 */
	public static final RegularBaseQuantity			ELECTRIC_CURRENT;

	/**
	 * The base quantity of length.
	 */
	public static final RegularBaseQuantity			LENGTH;

	/**
	 * The base quantity of luminous intensity.
	 */
	public static final RegularBaseQuantity			LUMINOUS_INTENSITY;

	/**
	 * The base quantity of mass.
	 */
	public static final RegularBaseQuantity			MASS;

	/**
	 * The base quantity of plane angle.
	 */
	public static final SupplementaryBaseQuantity	PLANE_ANGLE;

	/**
	 * The base quantity of solid angle.
	 */
	public static final SupplementaryBaseQuantity	SOLID_ANGLE;

	/**
	 * The base quantity of themodynamic temperature.
	 */
	public static final RegularBaseQuantity			THERMODYNAMIC_TEMPERATURE;

	/**
	 * The base quantity of time.
	 */
	public static final RegularBaseQuantity			TIME;

	/**
	 * The unknown base quantity.
	 */
	public static final UnknownBaseQuantity			UNKNOWN;

	/**
	 * @serial
	 */
	private final String							name;						// never
	// null
	// or
	// empty

	/**
	 * @serial
	 */
	private final String							symbol;					// may
	// be
	// null
	// ;
	// never
	// empty

	static {
		AMOUNT_OF_SUBSTANCE = new RegularBaseQuantity("Amount of Substance",
				"N", true);
		ELECTRIC_CURRENT = new RegularBaseQuantity("Electric Current", "I",
				true);
		LENGTH = new RegularBaseQuantity("Length", "L", true);
		LUMINOUS_INTENSITY = new RegularBaseQuantity("Luminous Intensity", "J",
				true);
		MASS = new RegularBaseQuantity("Mass", "M", true);
		PLANE_ANGLE = new SupplementaryBaseQuantity("Plane Angle", null, true);
		SOLID_ANGLE = new SupplementaryBaseQuantity("Solid Angle", null, true);
		THERMODYNAMIC_TEMPERATURE = new RegularBaseQuantity(
				"Thermodynamic Temperature", "T", true);
		TIME = new RegularBaseQuantity("Time", "t", true);
		UNKNOWN = new UnknownBaseQuantity();
	}

	/**
	 * Constructs from a name and a symbol.
	 * 
	 * @param name
	 *            Name of the base quantity. Shall be neither <code>null</code>
	 *            nor empty.
	 * @param symbol
	 *            Symbol for the base quantity. May be <code>null</code> but
	 *            shall not be empty.
	 * @throws NameException
	 *             <code>name</code> is <code>null</code> or empty.
	 */
	public BaseQuantity(final String name, final String symbol)
			throws NameException {
		this(name, symbol, true);
		if (name == null || name.length() == 0
				|| (symbol != null && symbol.length() == 0)) {
			throw new NameException("Invalid name or symbol");
		}
	}

	/**
	 * Constructs from a name and a symbol. This is the trusted form of the the
	 * constructor for use by subclasses only.
	 * 
	 * @param name
	 *            Name of the base quantity. Shall be neither <code>null</code>
	 *            nor empty.
	 * @param symbol
	 *            Symbol for the base quantity. May be <code>null</code> but
	 *            shall not be empty.
	 */
	protected BaseQuantity(final String name, final String symbol,
			final boolean trusted) {
		this.name = name;
		this.symbol = symbol;
	}

	/**
	 * Returns the name of the base quantity.
	 * 
	 * @return The name of the base quantity. Shall not be <code>null</code> or
	 *         empty.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the symbol of the base quantity.
	 * 
	 * @return The symbol of the base quantity. May be <code>null</code>. If
	 *         non-<code>null</code>, then shall not be empty.
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * Returns the identifier for the base quantity.
	 * 
	 * @return The base quantity's identifier (i.e. symbol or name).
	 */
	public final String getID() {
		final String id = getSymbol();
		return id == null
				? getName()
				: id;
	}

	/**
	 * Returns the string representation of the base quantity.
	 * 
	 * @return The string representation of the base quantity.
	 */
	@Override
	public final String toString() {
		return getID();
	}

	/**
	 * Indicates if this base quantity is semantically identical to an object.
	 * 
	 * @param object
	 *            The object to examine.
	 * @return <code>true</code> if an only if this base quantity is
	 *         semantically identical to <code>object</code>.
	 */
	@Override
	public boolean equals(final Object object) {
		return this == object
				? true
				: !(object instanceof BaseQuantity)
						? false
						: getName().equalsIgnoreCase(
								((BaseQuantity) object).getName())
								&& (getSymbol() == null || getSymbol().equals(
										((BaseQuantity) object).getSymbol()));
	}

	/**
	 * Returns the hash code of this instance.
	 * 
	 * @return The hash code of this instance.
	 */
	@Override
	public int hashCode() {
		return getName().toLowerCase().hashCode() ^ (getSymbol() == null
				? 0
				: getSymbol().hashCode());
	}

	/**
	 * Compares this base quantity to another base quantity.
	 * 
	 * @param that
	 *            The BaseQuantity to compare against.
	 * @return An integer that is negative, zero, or positive depending on
	 *         whether this BaseQuantity is less than, equal to, or greater than
	 *         <code>that</code>.
	 */
	public int compareTo(final BaseQuantity that) {
		int comp;
		if (this == that) {
			comp = 0;
		}
		else {
			comp = getName().compareToIgnoreCase(that.getName());
			if (comp == 0 && getSymbol() != null) {
				comp = getSymbol().compareTo(that.getSymbol());
			}
		}
		return comp;
	}

	/**
	 * Indicates if this base quantity is dimensionless.
	 * 
	 * @return <code>true</code> if an only if this BaseQuantity is
	 *         dimensionless (e.g. <code>BaseQuantity.SOLID_ANGLE</code>).
	 */
	public abstract boolean isDimensionless();

	/**
	 * Tests this class.
	 */
	public static void main(final String[] args) {
		System.out.println("AMOUNT_OF_SUBSTANCE.getName() = "
				+ AMOUNT_OF_SUBSTANCE.getName());
		System.out.println("LUMINOUS_INTENSITY.getSymbol() = "
				+ LUMINOUS_INTENSITY.getSymbol());
		System.out.println("PLANE_ANGLE.getSymbol() = "
				+ PLANE_ANGLE.getSymbol());

		System.out.println("LENGTH.equals(LENGTH) = " + LENGTH.equals(LENGTH));
		System.out.println("LENGTH.equals(MASS) = " + LENGTH.equals(MASS));
		System.out.println("LENGTH.equals(PLANE_ANGLE) = "
				+ LENGTH.equals(PLANE_ANGLE));
		System.out.println("PLANE_ANGLE.equals(PLANE_ANGLE) = "
				+ PLANE_ANGLE.equals(PLANE_ANGLE));
		System.out.println("PLANE_ANGLE.equals(SOLID_ANGLE) = "
				+ PLANE_ANGLE.equals(SOLID_ANGLE));

		System.out.println("LENGTH.compareTo(LENGTH) = "
				+ LENGTH.compareTo(LENGTH));
		System.out
				.println("LENGTH.compareTo(MASS) = " + LENGTH.compareTo(MASS));
		System.out.println("LENGTH.compareTo(PLANE_ANGLE) = "
				+ LENGTH.compareTo(PLANE_ANGLE));
		System.out.println("PLANE_ANGLE.compareTo(PLANE_ANGLE) = "
				+ PLANE_ANGLE.compareTo(PLANE_ANGLE));
		System.out.println("PLANE_ANGLE.compareTo(SOLID_ANGLE) = "
				+ PLANE_ANGLE.compareTo(SOLID_ANGLE));
	}
}
