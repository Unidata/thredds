/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.dmr.DapType;
import dap4.core.util.DapException;

import java.math.BigInteger;
import java.nio.charset.Charset;

abstract public class Value
{
    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    // default max values
    static public final int MAXSTRINGSIZE = 10;
    static public final int MAXOPAQUESIZE = 8;

    // For URL generation
    static public final int MAXSEGSIZE = 8;
    static public final int HOSTNSEG = 3;
    static public final int PATHNSEG = 4;

    static public enum ValueSource {RANDOM, FIXED;}

    //////////////////////////////////////////////////
    // Instance variables

    protected boolean asciionly = true;

    //////////////////////////////////////////////////
    // Constructor

    public Value() {}

    //////////////////////////////////////////////////
    // Accessors

    public void setASCII(boolean tf) {this.asciionly = tf;}

    // Abstract Methods

    abstract public ValueSource source();

    abstract public Object nextValue(DapType basetype) throws DapException;

    abstract public int nextCount(int max) throws DapException;


}

