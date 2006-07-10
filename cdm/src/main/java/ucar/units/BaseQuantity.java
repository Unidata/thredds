// $Id: BaseQuantity.java,v 1.6 2004/02/27 21:25:02 jeffmc Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for abstract base quantities (ex: length, time).
 *
 * @author Steven R. Emmerson
 * @version $Id: BaseQuantity.java,v 1.6 2004/02/27 21:25:02 jeffmc Exp $
 */
public abstract class
BaseQuantity
    implements	Base, Comparable, Serializable
{
    /**
     * The base quantity of amount of substance.
     */
    public static final RegularBaseQuantity		AMOUNT_OF_SUBSTANCE;

    /**
     * The base quantity of electric current.
     */
    public static final RegularBaseQuantity		ELECTRIC_CURRENT;

    /**
     * The base quantity of length.
     */
    public static final RegularBaseQuantity		LENGTH;

    /**
     * The base quantity of luminous intensity.
     */
    public static final RegularBaseQuantity		LUMINOUS_INTENSITY;

    /**
     * The base quantity of mass.
     */
    public static final RegularBaseQuantity		MASS;

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
    public static final RegularBaseQuantity		THERMODYNAMIC_TEMPERATURE;

    /**
     * The base quantity of time.
     */
    public static final RegularBaseQuantity		TIME;

    /**
     * The unknown base quantity.
     */
    public static final UnknownBaseQuantity		UNKNOWN;

    /**
     * @serial
     */
    private final String		name;	// never null or empty

    /**
     * @serial
     */
    private final String		symbol;	// may be null; never empty

    static
    {
	AMOUNT_OF_SUBSTANCE = 
	    new RegularBaseQuantity("Amount of Substance", "N", true);
	ELECTRIC_CURRENT = 
	    new RegularBaseQuantity("Electric Current", "I", true);
	LENGTH =
	    new RegularBaseQuantity("Length", "L", true);
	LUMINOUS_INTENSITY =
	    new RegularBaseQuantity("Luminous Intensity", "J", true);
	MASS =
	    new RegularBaseQuantity("Mass", "M", true);
	PLANE_ANGLE =
	    new SupplementaryBaseQuantity("Plane Angle", null, true);
	SOLID_ANGLE =
	    new SupplementaryBaseQuantity("Solid Angle", null, true);
	THERMODYNAMIC_TEMPERATURE =
	    new RegularBaseQuantity("Thermodynamic Temperature", "T", true);
	TIME =
	    new RegularBaseQuantity("Time", "t", true);
	UNKNOWN =
	    new UnknownBaseQuantity();
    }

    /**
     * Constructs from a name and a symbol.
     * @param name		Name of the base quantity.  Shall be neither
     *				<code>null</code> nor empty.
     * @param symbol		Symbol for the base quantity.  May be 
     *				<code>null</code> but shall not be empty.
     * @throws NameException	<code>name</code> is <code>null</code> or
     *				empty.
     */
    public
    BaseQuantity(String name, String symbol)
	throws NameException
    {
	this(name, symbol, true);
	if (name == null || name.length() == 0 || 
	    (symbol != null && symbol.length() == 0))
	{
	    throw new NameException("Invalid name or symbol");
	}
    }

    /**
     * Constructs from a name and a symbol.  This is the trusted form of the
     * the constructor for use by subclasses only.
     * @param name		Name of the base quantity.  Shall be neither
     *				<code>null</code> nor empty.
     * @param symbol		Symbol for the base quantity.  May be 
     *				<code>null</code> but shall not be empty.
     */
    protected
    BaseQuantity(String name, String symbol, boolean trusted)
    {
	this.name = name;
	this.symbol = symbol;
    }

    /**
     * Returns the name of the base quantity.
     * @return			The name of the base quantity.  Shall not be
     *				<code>null</code> or empty.
     */
    public String
    getName()
    {
	return name;
    }

    /**
     * Returns the symbol of the base quantity.
     * @return			The symbol of the base quantity.  May be
     *				<code>null</code>.  If non-<code>null</code>,
     *				then shall not be empty.
     */
    public String
    getSymbol()
    {
	return symbol;
    }

    /**
     * Returns the identifier for the base quantity.
     * @return                  The base quantity's identifier (i.e. symbol or
     *                          name).
     */
    public final String
    getID()
    {
	String	id = getSymbol();
	return id == null
		? getName()
		: id;
    }

    /**
     * Returns the string representation of the base quantity.
     * @return                  The string representation of the base quantity.
     */
    public final String
    toString()
    {
	return getID();
    }

    /**
     * Indicates if this base quantity is semantically identical to an object.
     * @param object		The object to examine.
     * @return			<code>true</code> if an only if this base
     *				quantity is semantically identical to
     *				<code>object</code>.
     */
    public boolean
    equals(Object object)
    {
	return this == object
		? true
		: !(object instanceof BaseQuantity)
		    ? false
		    : getName().equalsIgnoreCase(
			((BaseQuantity)object).getName()) &&
		      (getSymbol() == null ||
		       getSymbol().equals(((BaseQuantity)object).getSymbol()));
    }

    /**
     * Compares this base quantity to another base quantity.
     * @param object		To BaseQuantity to compare agains.
     * @return			An integer that is negative, zero, or positive
     *				depending on whether this BaseQuantity is less
     *				than, equal to, or greater than <code>object
     *				</code>.
     */
    public int
    compareTo(Object object)
    {
	int	comp;
	if (this == object)
	    comp = 0;
	else
	{
	    BaseQuantity	that = (BaseQuantity)object;
	    comp = getName().compareToIgnoreCase(that.getName());
	    if (comp == 0 && getSymbol() != null)
		comp = getSymbol().compareTo(that.getSymbol());
	}
	return comp;
    }

    /**
     * Indicates if this base quantity is dimensionless.
     * @return			<code>true</code> if an only if this 
     *				BaseQuantity is dimensionless (e.g.
     *				<code>BaseQuantity.SOLID_ANGLE</code>).
     */
    public abstract boolean
    isDimensionless();

    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
    {
	System.out.println("AMOUNT_OF_SUBSTANCE.getName() = " +
	    AMOUNT_OF_SUBSTANCE.getName());
	System.out.println("LUMINOUS_INTENSITY.getSymbol() = " +
	    LUMINOUS_INTENSITY.getSymbol());
	System.out.println("PLANE_ANGLE.getSymbol() = " +
	    PLANE_ANGLE.getSymbol());

	System.out.println("LENGTH.equals(LENGTH) = " +
	    LENGTH.equals(LENGTH));
	System.out.println("LENGTH.equals(MASS) = " +
	    LENGTH.equals(MASS));
	System.out.println("LENGTH.equals(PLANE_ANGLE) = " +
	    LENGTH.equals(PLANE_ANGLE));
	System.out.println("PLANE_ANGLE.equals(PLANE_ANGLE) = " +
	    PLANE_ANGLE.equals(PLANE_ANGLE));
	System.out.println("PLANE_ANGLE.equals(SOLID_ANGLE) = " +
	    PLANE_ANGLE.equals(SOLID_ANGLE));

	System.out.println("LENGTH.compareTo(LENGTH) = " +
	    LENGTH.compareTo(LENGTH));
	System.out.println("LENGTH.compareTo(MASS) = " +
	    LENGTH.compareTo(MASS));
	System.out.println("LENGTH.compareTo(PLANE_ANGLE) = " +
	    LENGTH.compareTo(PLANE_ANGLE));
	System.out.println("PLANE_ANGLE.compareTo(PLANE_ANGLE) = " +
	    PLANE_ANGLE.compareTo(PLANE_ANGLE));
	System.out.println("PLANE_ANGLE.compareTo(SOLID_ANGLE) = " +
	    PLANE_ANGLE.compareTo(SOLID_ANGLE));
    }
}
