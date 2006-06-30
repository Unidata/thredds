// $Id: UnitName.java,v 1.6 2000/08/18 04:17:38 russ Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
 * Provides support for unit names.
 *
 * @author Steven R. Emmerson
 * @version $Id: UnitName.java,v 1.6 2000/08/18 04:17:38 russ Exp $
 */
public final class
UnitName
    extends	UnitID
    implements	Comparable
{
    /**
     * The name of the unit.
     * @serial
     */
    private final String	name;

    /**
     * The plural form of the unit name.
     * @serial
     */
    private final String	plural;

    /**
     * The symbol for the unit.
     * @serial
     */
    private final String	symbol;

    /**
     * Constructs from a name and a symbol.  Regular rules are use to 
     * construct the plural form of the unit name.
     * @param name		The name of the unit.  Shall not be <code>
     *				null</code>.
     * @param symbol		The symbol for the unit.  May be <code>null
     *				</code>.
     * @throws NameException	<code>name == null</code>.
     */
    protected
    UnitName(String name, String symbol)
	throws NameException
    {
	this(name, null, symbol);
    }

    /**
     * Constructs from a name, a plural form of the unit name,  and a symbol.
     * @param name		The name of the unit.  Shall not be <code>
     *				null</code>.
     * @param plural		The plural form of the name.  May be <code>
     *				null</code>, in which case regular plural-
     *				forming rules are used to construct the plural
     *				form from the name.
     * @param symbol		The symbol for the unit.  May be <code>null
     *				</code>.
     * @throws NameException	<code>name == null</code>.
     */
    protected
    UnitName(String name, String plural, String symbol)
	throws NameException
    {
	if (name == null)
	    throw new NameException("Unit name can't be null");
	this.name = name;
	this.plural = plural == null ? makePlural(name) : plural;
	this.symbol = symbol;
    }

    /**
     * Factory method for constructing a UnitName from a name.
     * @param name		The name of the unit.  Shall not be <code>
     *				null</code>.
     * @throws NameException	<code>name == null</code>.
     */
    public static UnitName
    newUnitName(String name)
	throws NameException
    {
	return newUnitName(name, null);
    }

    /**
     * Factory method for constructing a UnitName from a name and a plural
     * form of the name.
     * @param name		The name of the unit.  Shall not be <code>
     *				null</code>.
     * @param plural		The plural form of the name.  May be <code>
     *				null</code>, in which case regular plural-
     *				forming rules are used to construct the plural
     *				form from the name.
     * @throws NameException	<code>name == null</code>.
     */
    public static UnitName
    newUnitName(String name, String plural)
	throws NameException
    {
	return newUnitName(name, plural, null);
    }

    /**
     * Factory method for constructing a UnitName from a name, a plural
     * form of the name, and a symbol.
     * @param name		The name of the unit.  Shall not be <code>
     *				null</code>.
     * @param plural		The plural form of the name.  May be <code>
     *				null</code>, in which case regular plural-
     *				forming rules are used to construct the plural
     *				form from the name.
     * @param symbol		The symbol for the unit.  May be <code>null
     *				</code>.
     * @throws NameException	<code>name == null</code>.
     */
    public static UnitName
    newUnitName(String name, String plural, String symbol)
	throws NameException
    {
	return new UnitName(name, plural, symbol);
    }

    /**
     * Returns the name.
     * @return			The name.  Won't be <code>null</code>.
     */
    public final String
    getName()
    {
	return name;
    }

    /**
     * Returns the plural form of the unit name.
     * @return			The plural form of the unit name.
     */
    public String
    getPlural()
    {
	return plural;
    }

    /**
     * Returns the symbol.
     * @return			The symbol.  Might be <code>null</code>.
     */
    public final String
    getSymbol()
    {
	return symbol;
    }

    /**
     * Returns the string representation of this identifier.
     * @return			The string representation of this identifier.
     */
    public final String
    toString()
    {
	String	string = getSymbol();
	return string == null
		? getName()
		: string;
    }

    /**
     * Compares this UnitName with another UnitName.
     */
    public final int
    compareTo(Object object)
    {
	return getName().compareToIgnoreCase(((UnitName)object).getName());
    }

    /**
     * Indicates if this UnitName is semantically identical to an object.
     */
    public final boolean
    equals(Object object)
    {
	return object instanceof UnitName && compareTo(object) == 0;
    }

    /**
     * Returns the plural form of a name.  Regular rules are used to generate
     * the plural form.
     * @param name		The name.
     * @return			The plural form of the name.
     */
    protected String
    makePlural(String name)
    {
	String	plural;
	int	length = name.length();
	char	lastChar = name.charAt(length-1);
	if (lastChar != 'y')
	{
	    plural = name +
		(lastChar == 's' || lastChar == 'x' ||
		 lastChar == 'z' || name.endsWith("ch")
		    ? "es"
		    : "s");
	}
	else
	{
	    if (length == 1)
	    {
		plural = name + "s";
	    }
	    else
	    {
		char	penultimateChar = name.charAt(length-2);
		plural =
		    (penultimateChar == 'a' || penultimateChar == 'e' ||
		     penultimateChar == 'i' || penultimateChar == 'o' ||
		     penultimateChar == 'u')
			? name + "s"
			: name.substring(0, length-1) + "ies";
	    }
	}
	return plural;
    }
}
