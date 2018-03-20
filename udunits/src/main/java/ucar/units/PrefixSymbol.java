/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.units;

import javax.annotation.concurrent.Immutable;

/**
 * Provides support for prefix symbols.
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 */
@Immutable
public final class
PrefixSymbol
    extends	Prefix
{
    /**
     * Constructs from a name and a numeric value.
     * @param name		The name for the prefix.
     * @param value		The numeric value for the prefix.
     */
    public
    PrefixSymbol(String name, double value)
    {
	super(name, value);
    }

    /**
     * Compares this prefix against another PrefixSymbol.  The sort keys are
     * decreasing length (major) and increasing lexicality (minor).
     * @param obj		The other PrefixSymbol.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this PrefixSymbol is less
     *				than, equal to, or greater than <code>
     *				obj</code>, respectively.
     */
    public final int
    compareTo(Object obj)
    {
	String	thatID = ((PrefixSymbol)obj).getID();
	int	comp = thatID.length() - getID().length();
	if (comp == 0)
	    comp = getID().compareTo(thatID);
	return comp;
    }

    /**
     * Compares this prefix against a String.  The sort keys are
     * decreasing length (major) and increasing lexicality (minor).
     * @param string		The string.
     * @return			A negative value, zero, or a positive value
     *				depending on whether this PrefixSymbol is less
     *				than, equal to, or greater than <code>
     *				string</code>, respectively.
     */
    public final int
    compareTo(String string)
    {
	int	comp = string.length() - getID().length();
	return comp < 0
		? comp
		: comp == 0
		    ? (getID().compareTo(string) == 0 ? 0 : -1)
		    : (getID().compareTo(string.substring(0, getID().length()))
			== 0
			    ? 0
			    : -1);
    }
}
