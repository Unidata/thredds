/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.ce.parser;

import dap4.core.dmr.parser.ParseException;

abstract public class CEActions
{
    //////////////////////////////////////////////////
    // Constructors

    public CEActions()
    {
    }

    //////////////////////////////////////////////////
    // Abstract Parser actions

    abstract void enterconstraint() throws ParseException;
    abstract void leaveconstraint() throws ParseException;

    abstract void enterprojection() throws ParseException;
    abstract void leaveprojection() throws ParseException;

    abstract void segment(Object o1, Object o2) throws ParseException;

    abstract Object slice(Object o1, Object o2, Object o3) throws ParseException;

    abstract Object dimslice(Object o1) throws ParseException;

    abstract Object word(Object ow) throws ParseException;

    abstract Object index(Object oi) throws ParseException;
   
    abstract Object slicelist(Object o1, Object o2) throws ParseException;

}
