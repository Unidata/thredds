// $Id$
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

/**
 * Provides support for units that are offset from reference units (ex: as
 * the unit "degree Celsius" is offset from the reference unit "kelvin").
 * Instances of this class are immutable.
 * @author Steven R. Emmerson
 * @version $Id$
 */
public final class
OffsetUnit
    extends	UnitImpl
    implements	DerivableUnit
{
    /**
     * The origin of this unit in terms of the reference unit.
     * @serial
     */
    private final double	_offset;

    /**
     * The reference unit.
     * @serial
     */
    private final Unit		_unit;

    /**
     * The derived unit that is convertible with this unit.
     * @serial
     */
    private DerivedUnit		_derivedUnit;

    /**
     * Constructs from a reference unit and an offset.
     * @param unit		The reference unit.
     * @param offset		The origin of this unit in terms of the 
     *				reference unit.  For example, a degree
     *				Celsius unit would be created as "<code>new
     *				OffsetUnit(kelvin, 273.15)</code>.
     */
    public
    OffsetUnit(Unit unit, double offset)
    {
	this(unit, offset, null);
    }

    /**
     * Constructs from a reference unit, and offset, and a unit identifier.
     * @param unit		The reference unit.
     * @param offset		The origin of this unit in terms of the 
     *				reference unit.  For example, a degree
     *				Celsius unit would be created as "<code>new
     *				OffsetUnit(kelvin, 273.15)</code>.
     * @param id		The identifier for the new unit.
     */
    public
    OffsetUnit(Unit unit, double offset, UnitName id)
    {
	super(id);
	if (!(unit instanceof OffsetUnit))
	{
	    _unit = unit;
	    _offset = offset;
	}
	else
	{
	    _unit = ((OffsetUnit)unit)._unit;
	    _offset = ((OffsetUnit)unit)._offset + offset;
	}
    }

    /**
     * Returns the reference unit.
     * @return			The reference unit.
     */
    public Unit
    getUnit()
    {
	return _unit;
    }

    /**
     * Returns the offset.  The offset is the location of the origin of
     * this unit in terms of the reference unit.
     * @return			The origin of this unit in terms of the
     *				reference unit.
     */
    public double
    getOffset()
    {
	return _offset;
    }

    /*
     * From UnitImpl:
     */

    /**
     * Clones this unit, changing the identifier.
     * @param id		The identifier for the new unit.
     * @return			This unit with its identifier changed.
     */
    public Unit
    clone(UnitName id)
    {
	return new OffsetUnit(getUnit(), getOffset(), id);
    }

    /** 
     * Multiply this unit by another unit.
     * @param that		The unit to multiply this unit by.
     * @return			The product of this unit and <code>that</code>.
     *				The offset of this unit will be ignored; thus,
     *				for example "celsius.myMultiplyBy(day)" is
     *				equivalent to "kelvin.myMultiplyBy(day)".
     * @throws MultiplyException	Can't multiply these units together.
     */
    protected Unit
    myMultiplyBy(Unit that)
	throws MultiplyException
    {
	return that instanceof OffsetUnit
	    ? getUnit().multiplyBy(((OffsetUnit)that).getUnit())
	    : getUnit().multiplyBy(that);
    }

    /** 
     * Divide this unit by another unit.
     * @param that		The unit to divide this unit by.
     * @return			The quotient of this unit and <code>that</code>.
     *				The offset of this unit will be ignored; thus,
     *				for example "celsius.myDivideBy(day)" is
     *				equivalent to "kelvin.myDivideBy(day)".
     * @throws OperationException	Can't divide these units.
     */
    protected Unit
    myDivideBy(Unit that)
	throws OperationException
    {
	return that instanceof OffsetUnit
	    ? getUnit().divideBy(((OffsetUnit)that).getUnit())
	    : getUnit().divideBy(that);
    }

    /** 
     * Divide this unit into another unit.
     * @param that		The unit to divide this unit into.
     * @return			The quotient of <code>that</code> unit and 
     *				this unit.  The offset of this unit will be
     *				ignored; thus, for example
     *				"celsius.myDivideInto(day)" is equivalent to
     *				"kelvin.myDivideInto(day)".
     * @throws OperationException	Can't divide these units.
     */
    protected Unit
    myDivideInto(Unit that)
	throws OperationException
    {
	return that instanceof OffsetUnit
	    ? getUnit().divideInto(((OffsetUnit)that).getUnit())
	    : getUnit().divideInto(that);
    }

    /** 
     * Raise this unit to a power.
     * @param power		The power to raise this unit by.
     * @return			The result of raising this unit by the power
     *				<code>power</code>.
     *				The offset of this unit will be
     *				ignored; thus, for example
     *				"celsius.myRaiseTo(2)" is equivalent to
     *				"kelvin.myRaiseTo(2)".
     * @throws RaiseException	Can't raise this unit to a power.
     */
    // Ignore offset (e.g. "Cel2" == "K2")
    protected Unit
    myRaiseTo(int power)
	throws RaiseException
    {
	return getUnit().raiseTo(power);
    }

    /**
     * Returns the derived unit that is convertible with this unit.
     * @return			The derived unit that is convertible with 
     *				this unit.
     */
    public DerivedUnit
    getDerivedUnit()
    {
	if (_derivedUnit == null)
	    _derivedUnit = getUnit().getDerivedUnit();
	return _derivedUnit;
    }

    /**
     * Converts a value in this unit to the equivalent value in the 
     * convertible derived unit.
     * @param amount		The value in this unit.
     * @return			The equivalent value in the convertible
     *				derived unit.
     * @throws ConversionException	Can't convert between units.
     */
    public float
    toDerivedUnit(float amount)
	throws ConversionException
    {
	return (float)toDerivedUnit((double)amount);
    }

    /**
     * Converts a value in this unit to the equivalent value in the 
     * convertible derived unit.
     * @param amount		The value in this unit.
     * @return			The equivalent value in the convertible
     *				derived unit.
     * @throws ConversionException	Can't convert between units.
     */
    public double
    toDerivedUnit(double amount)
	throws ConversionException
    {
	if (!(_unit instanceof DerivableUnit))
	    throw new ConversionException(this, getDerivedUnit());
	return ((DerivableUnit)getUnit()).toDerivedUnit(amount + getOffset());
    }

    /**
     * Converts values in this unit to the equivalent values in the 
     * convertible derived unit.
     * @param input		The values in this unit.
     * @param output		The equivalent values in the convertible
     *				derived unit.  May be the same array as
     *				<code>input</code>.
     * @return			<code>output</code>.
     * @throws ConversionException	Can't convert between units.
     */
    public float[]
    toDerivedUnit(float[] input, float[] output)
	throws ConversionException
    {
	if (!(_unit instanceof DerivableUnit))
	    throw new ConversionException(this, getDerivedUnit());
	float	origin = (float)getOffset();
	for (int i = input.length; --i >= 0; )
	    output[i] = input[i] + origin;
	return ((DerivableUnit)getUnit()).toDerivedUnit(output, output);
    }

    /**
     * Converts values in this unit to the equivalent values in the 
     * convertible derived unit.
     * @param input		The values in this unit.
     * @param output		The equivalent values in the convertible
     *				derived unit.  May be the same array as
     *				<code>input</code>.
     * @return			<code>output</code>.
     * @throws ConversionException	Can't convert between units.
     */
    public double[]
    toDerivedUnit(double[] input, double[] output)
	throws ConversionException
    {
	if (!(_unit instanceof DerivableUnit))
	    throw new ConversionException(this, getDerivedUnit());
	double	origin = getOffset();
	for (int i = input.length; --i >= 0; )
	    output[i] = input[i] + origin;
	return ((DerivableUnit)getUnit()).toDerivedUnit(output, output);
    }

    /**
     * Converts a value in the convertible derived unit to the equivalent
     * value in this unit.
     * @param amount		The value in the convertible derived unit.
     * @return			The equivalent value in this unit.
     * @throws ConversionException	Can't convert between units.
     */
    public float
    fromDerivedUnit(float amount)
	throws ConversionException
    {
	return (float)fromDerivedUnit((double)amount);
    }

    /**
     * Converts a value in the convertible derived unit to the equivalent
     * value in this unit.
     * @param amount		The value in the convertible derived unit.
     * @return			The equivalent value in this unit.
     * @throws ConversionException	Can't convert between units.
     */
    public double
    fromDerivedUnit(double amount)
	throws ConversionException
    {
	if (!(_unit instanceof DerivableUnit))
	    throw new ConversionException(getDerivedUnit(), this);
	return ((DerivableUnit)getUnit()).fromDerivedUnit(amount) - getOffset();
    }

    /**
     * Converts values in the convertible derived unit to the equivalent
     * values in this unit.
     * @param input		The values in the convertible derived unit.
     * @param output		The equivalent values in this unit.
     *				May be the same array as <code>input</code>.
     * @return			<code>output</code>.
     * @throws ConversionException	Can't convert between units.
     */
    public float[]
    fromDerivedUnit(float[] input, float[] output)
	throws ConversionException
    {
	if (!(_unit instanceof DerivableUnit))
	    throw new ConversionException(getDerivedUnit(), this);
	((DerivableUnit)getUnit()).fromDerivedUnit(input, output);
	float	origin = (float)getOffset();
	for (int i = input.length; --i >= 0; )
	    output[i] -= origin;
	return output;
    }

    /**
     * Converts values in the convertible derived unit to the equivalent
     * values in this unit.
     * @param input		The values in the convertible derived unit.
     * @param output		The equivalent values in this unit.
     *				May be the same array as <code>input</code>.
     * @return			<code>output</code>.
     * @throws ConversionException	Can't convert between units.
     */
    public double[]
    fromDerivedUnit(double[] input, double[] output)
	throws ConversionException
    {
	if (!(_unit instanceof DerivableUnit))
	    throw new ConversionException(getDerivedUnit(), this);
	((DerivableUnit)getUnit()).fromDerivedUnit(input, output);
	double	origin = getOffset();
	for (int i = input.length; --i >= 0; )
	    output[i] -= origin;
	return output;
    }

    /**
     * Indicates if this unit is semantically identical to an object.
     * @param object		The object.
     * @return			<code>true</code> if and only if this unit
     *				is semantically identical to <code>object
     *				</code>.
     */
    public boolean
    equals(Object object)
    {
	return
	    object instanceof OffsetUnit
		? super.equals(object) &&
		  getOffset() == ((OffsetUnit)object).getOffset() &&
		  getUnit().equals(((OffsetUnit)object).getUnit())
		: getOffset() == 0 && getUnit().equals(object);
    }

    /**
     * Indicates if this unit is dimensionless.
     * @return			<code>true</code> if and only if this unit
     *				is dimensionless.
     */
    public boolean
    isDimensionless()
    {
	return getUnit().isDimensionless();
    }

    /**
     * Returns the string representation of this unit.
     * @return			The string representation of this unit.
     */
    public String
    toString()
    {
	String	string = super.toString();	// get symbol or name
	return string != null
		? string
		: getCanonicalString();
    }
    
    /**
     * Returns the canonical string representation of the unit.
     * @return                  The canonical string representation.
     */
    public String
    getCanonicalString() {
        return "(" + getUnit().toString() + ") @ " + getOffset();
    }


    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws	Exception
    {
	BaseUnit	kelvin =
	    BaseUnit.getOrCreate(
		UnitName.newUnitName("kelvin", null, "K"),
		BaseQuantity.THERMODYNAMIC_TEMPERATURE);
	OffsetUnit	celsius = new OffsetUnit(kelvin, 273.15);
	System.out.println("celsius.equals(kelvin)=" + celsius.equals(kelvin));
	System.out.println("celsius.getUnit().equals(kelvin)=" +
	    celsius.getUnit().equals(kelvin));
	Unit		celsiusKelvin = celsius.multiplyBy(kelvin);
	System.out.println("celsiusKelvin.divideBy(celsius)=" + 
	    celsiusKelvin.divideBy(celsius));
	System.out.println("celsius.divideBy(kelvin)=" + 
	    celsius.divideBy(kelvin));
	System.out.println("kelvin.divideBy(celsius)=" + 
	    kelvin.divideBy(celsius));
	System.out.println("celsius.raiseTo(2)=" + 
	    celsius.raiseTo(2));
	System.out.println("celsius.toDerivedUnit(1.)=" + 
	    celsius.toDerivedUnit(1.));
	System.out.println(
	    "celsius.toDerivedUnit(new float[]{1,2,3}, new float[3])[1]="+ 
	    celsius.toDerivedUnit(new float[]{1,2,3}, new float[3])[1]);
	System.out.println("celsius.fromDerivedUnit(274.15)=" + 
	    celsius.fromDerivedUnit(274.15));
	System.out.println(
	    "celsius.fromDerivedUnit(new float[]{274.15f},new float[1])[0]="+ 
	    celsius.fromDerivedUnit(new float[]{274.15f}, new float[1])[0]);
	System.out.println(
	    "celsius.equals(celsius)=" + celsius.equals(celsius));
	OffsetUnit	celsius100 = new OffsetUnit(celsius, 100.);
	System.out.println(
	    "celsius.equals(celsius100)=" + celsius.equals(celsius100));
	System.out.println(
	    "celsius.isDimensionless()=" + celsius.isDimensionless());
	BaseUnit	radian =
	    BaseUnit.getOrCreate(
		UnitName.newUnitName("radian", null, "rad"),
		BaseQuantity.PLANE_ANGLE);
	OffsetUnit	offRadian = new OffsetUnit(radian, 3.14159/2);
	System.out.println("offRadian.isDimensionless()=" + 
	    offRadian.isDimensionless());;
    }
}
