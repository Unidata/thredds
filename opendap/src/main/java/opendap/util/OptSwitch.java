/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////



package opendap.util;

/**
 * OptSwitch	- class definition for Switches
 * <p/>
 * Description:  this class defines a switch element.
 * a switch is considered to be used (having been set or assigned
 * a value if the 'set' field is false AND the 'val' field is null.
 * <p/>
 * Constants:
 * <p/>
 * Permitted values for 'type' field:
 * <p/>
 * protected static final int NONE 	uninitialized
 * protected static final int BOOL 	boolean type switch
 * protected static final int VAL  	value type switch
 * <p/>
 * Fields:	sw	the switch name
 * type	boolean/value
 * set	value is set/clear
 * val	switch value (not applicable to boolean type switch)
 *
 * @author Arieh Markel
 * @version 1.0
 */
public class OptSwitch extends Object {
    protected static final int NONE = 0;
    protected static final int BOOL = 1;
    protected static final int VAL = 2;

    int sw;        // switch name
    int type;        // boolean/value
    public boolean set;    // switch is set/unset
    public String val;        // value of switch
    boolean debug = false;

    public OptSwitch() {
        set = false;
        type = NONE;
        val = null;
        sw = -1;
    }

    /**
     * Invocation with explicit Character switchname and type
     *
     * @param c
     * @param type
     */
    public OptSwitch(Character c, int type) {
        sw = Character.digit(c.charValue(), 10);
        this.type = type;
        set = false;
        val = null;
        if (debug) {
            System.out.println("sw = " + (char) sw + "; type = " + type +
                    "; set = " + set + "; val = " + val);
        }
    }

    /**
     * Invocation with explicit integer switchname and type
     *
     * @param c
     * @param type
     */
    public OptSwitch(int c, int type) {
        sw = c;
        this.type = type;
        set = false;
        val = null;
        if (debug) {
            System.out.println("sw = " + (char) sw + "; type = " + type +
                    "; set = " + set + "; val = " + val);
        }
    }

    /**
     * Set the value type of the option switch to the type passed
     *
     * @param    type    type of value that switch may accept or be
     */
    public void SetHasValue(int type) {
        this.type = type;
        if (debug) {
            System.out.println("sw = " + (char) sw + "; type = " + type +
                    "; set = " + set + "; val = " + val);
        }
    }

    /**
     * Return whether the option switch accepts values or no
     */
    public boolean acceptVal() {
        return type == VAL;
    }

    /**
     * Set the 'set' field of the option switch to 'b'.
     *
     * @param b set the 'set' boolean field to 'b'.
     */
    public void SetVal(boolean b) {
        set = b;
    }

    /**
     * Set the 'val' field of the option switch to 's'.
     *
     * @param s string to assign to 'val' field.
     */
    public void SetVal(String s) {
        val = s;
    }
}


