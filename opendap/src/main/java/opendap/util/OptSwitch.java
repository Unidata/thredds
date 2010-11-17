/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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


