// $Id: PrefixSymbol.java 64 2006-07-12 22:30:50Z edavis $
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
 * Provides support for prefix symbols.
 *
 * Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: PrefixSymbol.java 64 2006-07-12 22:30:50Z edavis $
 */
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
