// $Id: Factor.java,v 1.5 2000/08/18 04:17:27 russ Exp $
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
 * Provides support for a Base/exponent pair.  Instances of this class are
 * immutable.
 * @author Steven R. Emmerson
 * @version $Id: Factor.java,v 1.5 2000/08/18 04:17:27 russ Exp $
 */
public final class
Factor
    implements	Serializable
{
    /**
     * The Base entity.
     * @serial
     */
    private final Base	_base;

    /**
     * The exponent.
     * @serial
     */
    private final int	_exponent;

    /**
     * Constructs from a Base.  The exponent is set to unity.
     * @param base		The base entity.
     */
    public
    Factor(Base base)
    {
	this(base, 1);
    }

    /**
     * Constructs from a Factor and an exponent.
     * @param factor		The factor.
     * @param exponent		The exponent.
     */
    public
    Factor(Factor factor, int exponent)
    {
	this(factor.getBase(), exponent);
    }

    /**
     * Constructs from a Base and an exponent.
     * @param base		The base entity.
     * @param exponent		The exponent.
     */
    public
    Factor(Base base, int exponent)
    {
	_base = base;
	_exponent = exponent;
    }

    /**
     * Returns the Base entity.
     * @return			The Base entity.
     */
    public Base
    getBase()
    {
	return _base;
    }

    /**
     * Returns the identifier of the Base entity.
     * @return			The identifier of the Base entity (symbol or
     *				name).
     */
    public String
    getID()
    {
	return getBase().getID();
    }

    /**
     * Returns the exponent of the Base entity.
     * @return			The exponent of the Base entity.
     */
    public int
    getExponent()
    {
	return _exponent;
    }

    /**
     * Raises this Factor to a power.
     * @param power		The power by which to raise this Factor.
     * @return			The Factor resulting from raising this Factor
     *				to the given power.
     */
    public Factor
    pow(int power)
    {
	return new Factor(getBase(), getExponent()*power);
    }

    /**
     * Returns the string representation of this Factor.
     * @return			The string representation of this Factor.
     */
    public final String
    toString()
    {
	return getExponent() == 0
		? ""
		: getExponent() == 1
		    ? getBase().toString()
		    : getBase().toString() + getExponent();
    }

    /**
     * Indicates if this Factor is semantically identical to another object.
     * @param object		The object.
     * @return			<code>true</code> if and only if this Factor
     *				is semantically identical to <code>object<
     *				/code>.
     */
    public boolean
    equals(Object object)
    {
	boolean	equals;
	if (this == object)
	    equals = true;
	else
	if (!(object instanceof Factor))
	    equals = false;
	else
	{
	    Factor	that = (Factor)object;
	    equals = getExponent() != that.getExponent()
			? false
			: getExponent() == 0 ||
			    getBase().equals(that.getBase());
	}
	return equals;
    }

    /**
     * Indicates if this Factor is the reciprocal of another Factor.
     * @param that		The other factor.
     * @return			<code>true</code> if and only if this Factor
     *				is the reciprocal of <code>that</code>.
     */
    public boolean
    isReciprocalOf(Factor that)
    {
	return
	    getBase().equals(that.getBase()) &&
	    getExponent() == -that.getExponent();
    }

    /**
     * Indicates if this factor is dimensionless.  A Factor is
     * dimensionless if and only if the exponent is zero or the Base
     * entity is dimensionless.
     * @return			<code>true</code> if and only if this Factor is
     *				dimensionless.
     */
    public boolean
    isDimensionless()
    {
	return getExponent() == 0 || getBase().isDimensionless();
    }
}
