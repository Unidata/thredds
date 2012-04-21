// $Id: UnknownUnit.java 64 2006-07-12 22:30:50Z edavis $
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

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides support for unknown base units. This can be used, for example, to
 * accomodate an unknown unit (e.g. "foo"). Values in such a unit will only be
 * convertible with units derived from "foo" (e.g. "20 foo").
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnknownUnit.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class UnknownUnit extends BaseUnit {
    private static final long                           serialVersionUID = 1L;
    /**
     * The name-to-unit map.
     * 
     * @serial
     */
    private static final SortedMap<String, UnknownUnit> map              = new TreeMap<String, UnknownUnit>();

    /**
     * Constructs from a name.
     * 
     * @param name
     *            The name of the unit.
     */
    private UnknownUnit(final String name) throws NameException {
        super(UnitName.newUnitName(name, null, name), BaseQuantity.UNKNOWN);
    }

    /**
     * Factory method for constructing an unknown unit from a name.
     * 
     * @param name
     *            The name of the unit.
     * @return The unknown unit.
     * @throws NameException
     *             <code>name == null</code>.
     */
    public static UnknownUnit create(String name) throws NameException {
        UnknownUnit unit;
        name = name.toLowerCase();
        synchronized (map) {
            unit = map.get(name);
            if (unit == null) {
                unit = new UnknownUnit(name);
                map.put(unit.getName(), unit);
                map.put(unit.getPlural(), unit);
            }
        }
        return unit;
    }

    /*
     * From Unit:
     */

    /**
     * Indicates if this unit is semantically identical to an object.
     * 
     * @param object
     *            The object.
     * @return <code>true</code> if and only if this instance is semantically
     *         identical to the object.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UnknownUnit)) {
            return false;
        }
        final UnknownUnit that = (UnknownUnit) object;
        return getName().equalsIgnoreCase(that.getName());
    }

    /**
     * Returns the hash code of this instance.
     * 
     * @return The hash code of this instance.
     */
    @Override
    public int hashCode() {
        return getName().toLowerCase().hashCode();
    }

    /**
     * Indicates if this unit is dimensionless. An unknown unit is never
     * dimensionless.
     * 
     * @return <code>false</code> always.
     */
    @Override
    public boolean isDimensionless() {
        return false;
    }

    /**
     * Tests this class.
     */
    public static void main(final String[] args) throws Exception {
        final UnknownUnit unit1 = UnknownUnit.create("a");
        System.out.println("unit_a.equals(unit_a)=" + unit1.equals(unit1));
        System.out.println("unit_a.isDimensionless()="
                + unit1.isDimensionless());
        UnknownUnit unit2 = UnknownUnit.create("b");
        System.out.println("unit_a.equals(unit_b)=" + unit1.equals(unit2));
        unit2 = UnknownUnit.create("A");
        System.out.println("unit_a.equals(unit_A)=" + unit1.equals(unit2));
    }
}
