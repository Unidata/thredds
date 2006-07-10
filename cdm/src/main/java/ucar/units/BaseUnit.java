// $Id: BaseUnit.java,v 1.5 2000/08/18 04:17:26 russ Exp $
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

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides support for base units.
 * @author Steven R. Emmerson
 * @version $Id: BaseUnit.java,v 1.5 2000/08/18 04:17:26 russ Exp $
 */
public class
BaseUnit
    extends	DerivedUnitImpl
    implements	Base
{
    /**
     * The identifier-to-unit map.
     * @serial
     */
    private static final SortedMap	nameMap =  new TreeMap();

    /**
     * The quantity-to-unit map.
     * @serial
     */
    private static final SortedMap	quantityMap =  new TreeMap();

    /**
     * The base quantity associated with this base unit.
     * @serial
     */
    private final BaseQuantity	baseQuantity;

    /**
     * Constructs from identifiers and a base quantity.
     * @param id		The identifiers for the base unit.  <code>
     *				id.getSymbol()</code> shall not return <code>
     *				null</code>.
     * @param baseQuantity	The base quantity of the base unit.
     * @throws NameException	<code>id.getSymbol()</code> returned <code>
     *				null</code>.
     */
    protected
    BaseUnit(UnitName id, BaseQuantity baseQuantity)
	throws NameException
    {
	super(id);
	if (id.getSymbol() == null)
	    throw new NameException("Base unit must have symbol");
	setDimension(new UnitDimension(this));
	this.baseQuantity = baseQuantity;
    }

    /**
     * Factory method for creating a new BaseUnit or obtaining a 
     * previously-created one.
     * @param id		The identifier for the base unit.  <code>
     *				id.getSymbol()</code> shall not return <code>
     *				null</code>.
     * @param baseQuantity	The base quantity of the base unit.
     * @throws NameException	<code>id.getSymbol()</code> returned <code>
     *				null</code>.
     * @throws UnitExistsException	Attempt to incompatibly redefine an
     *					existing base unit.
     */
    public static synchronized BaseUnit
    getOrCreate(UnitName id, BaseQuantity baseQuantity)
	throws NameException, UnitExistsException
    {
	BaseUnit	baseUnit;
	BaseUnit	nameUnit = (BaseUnit)nameMap.get(id);
	BaseUnit	quantityUnit = (BaseUnit)quantityMap.get(baseQuantity);
	if (nameUnit != null || quantityUnit != null)
	{
	    baseUnit = nameUnit != null
			? nameUnit
			: quantityUnit;
	    if ((nameUnit != null && 
		    !baseQuantity.equals(nameUnit.getBaseQuantity())) ||
		(quantityUnit != null && 
		    !id.equals(quantityUnit.getUnitName())))
	    {
		throw new UnitExistsException(
		    "Attempt to incompatibly redefine base unit \"" + baseUnit +
		    '"');
	    }
	}
	else
	{
	    baseUnit = new BaseUnit(id, baseQuantity);
	    quantityMap.put(baseQuantity, baseUnit);
	    nameMap.put(id, baseUnit);
	}
	return baseUnit;
    }

    /**
     * Returns the base quantity associated with this base unit.
     * @return			The base quantity associated with this base 
     *				unit.
     */
    public final BaseQuantity
    getBaseQuantity()
    {
	return baseQuantity;
    }

    /**
     * Returns the identifier for this base unit.  This is identical to
     * <code>getSymbol()</code>.
     * @return			The identifier for this base unit.
     */
    public final String
    getID()
    {
	return getSymbol();
    }

    /**
     * Returns the string representation of this base unit.  This is
     * identical to <code>getID()</code>.
     * @return			The string representation of this base unit.
     */
    public final String
    toString()
    {
	return getID();
    }

    /**
     * Indicates if this base unit is semantically identical to an
     * object.
     * @param object		The object. 
     * @return			<code>true</code> if and only if this base
     *				unit is semantically identical to <code>
     *				object</code>.
     */
    public boolean
    equals(Object object)
    {
	/*
	 * Because the BaseUnit class guarantees that a BaseQuantity has,
	 * at most, one corresponding BaseUnit, we only need to check for
	 * equality of the corresponding BaseQuantities.
	 */
	return
	    object instanceof BaseUnit &&
	    getBaseQuantity().equals(((BaseUnit)object).getBaseQuantity());
    }

    /**
     * Indicates if this base unit is dimensionless.
     * @return			<code>true</code> if and only if this base unit
     *				is dimensionless (e.g. "radian").
     */
    public boolean
    isDimensionless()
    {
	return baseQuantity.isDimensionless();
    }

    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws Exception
    {
	BaseUnit	meter =
	    new BaseUnit(
		UnitName.newUnitName("meter", null, "m"),
		BaseQuantity.LENGTH);
	System.out.println("meter.getBaseQuantity()=" + 
	    meter.getBaseQuantity());
	System.out.println("meter.toDerivedUnit(1.)=" + 
	    meter.toDerivedUnit(1.));
	System.out.println("meter.toDerivedUnit(new float[] {2})[0]=" + 
	   meter.toDerivedUnit(new float[] {2}, new float[1])[0]);
	System.out.println("meter.fromDerivedUnit(1.)=" + 
	    meter.fromDerivedUnit(1.));
	System.out.println("meter.fromDerivedUnit(new float[] {3})[0]=" + 
	    meter.fromDerivedUnit(new float[] {3}, new float[1])[0]);
	System.out.println("meter.isCompatible(meter)=" + 
	    meter.isCompatible(meter));
	BaseUnit	radian =
	    new BaseUnit(
		UnitName.newUnitName("radian", null, "rad"),
		BaseQuantity.PLANE_ANGLE);
	System.out.println("meter.isCompatible(radian)=" + 
	    meter.isCompatible(radian));
	System.out.println("meter.isDimensionless()=" + 
	    meter.isDimensionless());
	System.out.println("radian.isDimensionless()=" + 
	    radian.isDimensionless());
    }
}
