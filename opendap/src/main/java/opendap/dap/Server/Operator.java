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

package opendap.dap.Server;

import opendap.dap.*;
//import gnu.regexp.*;
import java.util.regex.*;
import opendap.dap.parser.ExprParserConstants;

/**
 * This class contains the code for performing relative
 * operations (RelOps) on BaseTypes. It contains one
 * heavily overloaded method that is smart enough to figure
 * out which actual BaseType were passed in and perform (if
 * appropriate) the RelOp comparison on the 2 types.
 *
 * @author ndp
 * @version $Revision: 22540 $
 */


public class Operator implements ExprParserConstants {

    private static boolean _Debug = false;

    /**
     * Performs the Relatove Operation (RelOp) indicated by the
     * parameter <code>oprtr</code> on the 2 passed BaseTypes if
     * appropriate.
     * <p/>
     * Obviously some type don't compare logically, such as asking if
     * String is less than a Float. For these non sensical operations
     * and <code>InvalidOperatorException</code> is thrown.
     *
     * @param oprtr The operatoration to perform as defined in <code>
     *              opendap.dap.parser.ExprParserConstants</code>
     * @param lop   A BaseType to be used as the left operand.
     * @param rop   A BaseType to be used as the right operand.
     * @return True is the operation evaluates as true, flase otherwise.
     * @throws InvalidOperatorException
     * @throws RegExpException
     * @throws SBHException
     * @see opendap.dap.parser.ExprParserConstants
     */
    public static boolean op(int oprtr, BaseType lop, BaseType rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        if (lop instanceof DByte)
            return (op(oprtr, (DByte) lop, rop));
        else if (lop instanceof DFloat32)
            return (op(oprtr, (DFloat32) lop, rop));
        else if (lop instanceof DFloat64)
            return (op(oprtr, (DFloat64) lop, rop));
        else if (lop instanceof DInt16)
            return (op(oprtr, (DInt16) lop, rop));
        else if (lop instanceof DInt32)
            return (op(oprtr, (DInt32) lop, rop));
        else if (lop instanceof DString)
            return (op(oprtr, (DString) lop, rop));
        else if (lop instanceof DUInt16)
            return (op(oprtr, (DUInt16) lop, rop));
        else if (lop instanceof DUInt32)
            return (op(oprtr, (DUInt32) lop, rop));
        else if (lop instanceof DURL)
            return (op(oprtr, (DURL) lop, rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }


    private static boolean op(int oprtr, DByte lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DFloat32 lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DFloat64 lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DInt16 lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DInt32 lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DString lop, BaseType rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DUInt16 lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DUInt32 lop, BaseType rop) throws InvalidOperatorException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

    private static boolean op(int oprtr, DURL lop, BaseType rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        if (rop instanceof DByte)
            return (op(oprtr, lop, (DByte) rop));
        else if (rop instanceof DFloat32)
            return (op(oprtr, lop, (DFloat32) rop));
        else if (rop instanceof DFloat64)
            return (op(oprtr, lop, (DFloat64) rop));
        else if (rop instanceof DInt16)
            return (op(oprtr, lop, (DInt16) rop));
        else if (rop instanceof DInt32)
            return (op(oprtr, lop, (DInt32) rop));
        else if (rop instanceof DString)
            return (op(oprtr, lop, (DString) rop));
        else if (rop instanceof DUInt16)
            return (op(oprtr, lop, (DUInt16) rop));
        else if (rop instanceof DUInt32)
            return (op(oprtr, lop, (DUInt32) rop));
        else if (rop instanceof DURL)
            return (op(oprtr, lop, (DURL) rop));
        else
            throw new InvalidOperatorException("Binary operations not supported for the type:" + lop.getClass());
    }

//******************************************************************************************
//******************************************************************************************
//******************************************************************************************
//******************************************************************************************


    private static String opErrorMsg(int oprtr, String lop, String rop) {
        switch (oprtr) {
            case LESS:
                return ("Less Than (<) Operator not valid between types" + lop + "and" + rop + ".");
            case LESS_EQL:
                return ("Less Than Equal To (<=) Operator not valid between types" + lop + "and" + rop + ".");
            case GREATER:
                return ("Greater Than (>) Operator not valid between types" + lop + "and" + rop + ".");
            case GREATER_EQL:
                return ("Greater Than Equal To (>=) Operator not valid between types" + lop + "and" + rop + ".");
            case EQUAL:
                return ("Equal To (==) Operator not valid between types" + lop + "and" + rop + ".");
            case NOT_EQUAL:
                return ("Not Equal To (!=) Operator not valid between types" + lop + "and" + rop + ".");
            case REGEXP:
                return ("Regular Expression cannot beevaluated between types" + lop + "and" + rop + ".");
            default:
                return ("Unknown Operator Requested! RTFM!");
        }
    }


//******************************************************************************************
//***                              Byte Vs The World                                 ****
//------------------------------------------------------------------------------------------

    private static boolean op(int oprtr, DByte lop, DByte rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        int rval = ((int) rop.getValue()) & 0xFF;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DFloat32 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DFloat64 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DInt16 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DInt32 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DUInt16 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DUInt32 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFF;
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DByte lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              Float32 Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DFloat32 lop, DByte rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DFloat32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DFloat64 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DUInt16 rop) throws InvalidOperatorException {
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DUInt32 rop) throws InvalidOperatorException {
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat32 lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              Float64 Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DFloat64 lop, DByte rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DFloat32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DFloat64 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DUInt16 rop) throws InvalidOperatorException {
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DUInt32 rop) throws InvalidOperatorException {
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DFloat64 lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              Int16 Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DInt16 lop, DByte rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DFloat32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DFloat64 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DUInt16 rop) throws InvalidOperatorException {
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DUInt32 rop) throws InvalidOperatorException {
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt16 lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              Int32 Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DInt32 lop, DByte rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DFloat32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DFloat64 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rop.getValue());
            case LESS_EQL:
                return (lop.getValue() <= rop.getValue());
            case GREATER:
                return (lop.getValue() > rop.getValue());
            case GREATER_EQL:
                return (lop.getValue() >= rop.getValue());
            case EQUAL:
                return (lop.getValue() == rop.getValue());
            case NOT_EQUAL:
                return (lop.getValue() != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DUInt16 rop) throws InvalidOperatorException {
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DUInt32 rop) throws InvalidOperatorException {
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lop.getValue() < rval);
            case LESS_EQL:
                return (lop.getValue() <= rval);
            case GREATER:
                return (lop.getValue() > rval);
            case GREATER_EQL:
                return (lop.getValue() >= rval);
            case EQUAL:
                return (lop.getValue() == rval);
            case NOT_EQUAL:
                return (lop.getValue() != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DInt32 lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              String Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DString lop, DByte rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DFloat32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DFloat64 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DString rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        int cmp;
        switch (oprtr) {
            case LESS:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp < 0)
                    return (true);
                else
                    return (false);
            case LESS_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp <= 0)
                    return (true);
                else
                    return (false);
            case GREATER:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp > 0)
                    return (true);
                else
                    return (false);
            case GREATER_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp >= 0)
                    return (true);
                else
                    return (false);
            case EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp == 0)
                    return (true);
                else
                    return (false);
            case NOT_EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp != 0)
                    return (true);
                else
                    return (false);
            case REGEXP:
                if (_Debug)
                    System.out.println("Left Operand: '" + lop.getValue() + "'  Right Operand: '" + rop.getValue() + "'");
                try {
//                    RE regexp = new RE(lop.getValue());
//                    return (regexp.isMatch(rop.getValue()));
		      return rop.getValue().matches(lop.getValue());
                }
//		catch (REException e1) {
                catch (PatternSyntaxException e1) {
                    throw new RegExpException(e1.getMessage());
                }
                catch (Exception e2) {
                    throw new SBHException(e2.getMessage());
                }

            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DUInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DUInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DString lop, DURL rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        int cmp;
        switch (oprtr) {
            case LESS:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp < 0)
                    return (true);
                else
                    return (false);
            case LESS_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp <= 0)
                    return (true);
                else
                    return (false);
            case GREATER:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp > 0)
                    return (true);
                else
                    return (false);
            case GREATER_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp >= 0)
                    return (true);
                else
                    return (false);
            case EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp == 0)
                    return (true);
                else
                    return (false);
            case NOT_EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp != 0)
                    return (true);
                else
                    return (false);
            case REGEXP:
                if (_Debug)
                    System.out.println("Left Operand: '" + lop.getValue() + "'  Right Operand: '" + rop.getValue() + "'");
                try {
//                    RE regexp = new RE(lop.getValue());
//                    return (regexp.isMatch(rop.getValue()));
                    return rop.getValue().matches(lop.getValue());
                }
//		catch (REException e1) {
	        catch (PatternSyntaxException e1) {
                    throw new RegExpException(e1.getMessage());
                }
                catch (Exception e2) {
                    throw new SBHException(e2.getMessage());
                }

            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              UInt16 Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DUInt16 lop, DByte rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DFloat32 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DFloat64 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DInt16 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DInt32 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DUInt16 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DUInt32 rop) throws InvalidOperatorException {
        int lval = ((int) lop.getValue()) & 0xFFFF;
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt16 lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              UInt32 Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DUInt32 lop, DByte rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DFloat32 rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DFloat64 rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DInt16 rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DInt32 rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rop.getValue());
            case LESS_EQL:
                return (lval <= rop.getValue());
            case GREATER:
                return (lval > rop.getValue());
            case GREATER_EQL:
                return (lval >= rop.getValue());
            case EQUAL:
                return (lval == rop.getValue());
            case NOT_EQUAL:
                return (lval != rop.getValue());
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DString rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DUInt16 rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        int rval = ((int) rop.getValue()) & 0xFFFF;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DUInt32 rop) throws InvalidOperatorException {
        long lval = ((long) lop.getValue()) & 0xFFFFFFFFL;
        long rval = ((long) rop.getValue()) & 0xFFFFFFFFL;
        switch (oprtr) {
            case LESS:
                return (lval < rval);
            case LESS_EQL:
                return (lval <= rval);
            case GREATER:
                return (lval > rval);
            case GREATER_EQL:
                return (lval >= rval);
            case EQUAL:
                return (lval == rval);
            case NOT_EQUAL:
                return (lval != rval);
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DUInt32 lop, DURL rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

//******************************************************************************************
//***                              URL Vs The World                                 ****
//------------------------------------------------------------------------------------------


    private static boolean op(int oprtr, DURL lop, DByte rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DFloat32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DFloat64 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DString rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        int cmp;
        switch (oprtr) {
            case LESS:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp < 0)
                    return (true);
                else
                    return (false);
            case LESS_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp <= 0)
                    return (true);
                else
                    return (false);
            case GREATER:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp > 0)
                    return (true);
                else
                    return (false);
            case GREATER_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp >= 0)
                    return (true);
                else
                    return (false);
            case EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp == 0)
                    return (true);
                else
                    return (false);
            case NOT_EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp != 0)
                    return (true);
                else
                    return (false);
            case REGEXP:
                if (_Debug)
                    System.out.println("Left Operand: '" + lop.getValue() + "'  Right Operand: '" + rop.getValue() + "'");
                try {
//                    RE regexp = new RE(lop.getValue());
//                    return (regexp.isMatch(rop.getValue()));
                      return rop.getValue().matches(lop.getValue());
                }
//                catch (REException e1) {
                catch (PatternSyntaxException e1) {
                    throw new RegExpException(e1.getMessage());
                }
                catch (Exception e2) {
                    throw new SBHException(e2.getMessage());
                }

            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DUInt16 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DUInt32 rop) throws InvalidOperatorException {
        switch (oprtr) {
            case LESS:
            case LESS_EQL:
            case GREATER:
            case GREATER_EQL:
            case EQUAL:
            case NOT_EQUAL:
            case REGEXP:
                throw new InvalidOperatorException(opErrorMsg(oprtr, lop.getTypeName(), rop.getTypeName()));
            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }

    private static boolean op(int oprtr, DURL lop, DURL rop) throws InvalidOperatorException,
            RegExpException,
            SBHException {
        int cmp;
        switch (oprtr) {
            case LESS:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp < 0)
                    return (true);
                else
                    return (false);
            case LESS_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp <= 0)
                    return (true);
                else
                    return (false);
            case GREATER:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp > 0)
                    return (true);
                else
                    return (false);
            case GREATER_EQL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp >= 0)
                    return (true);
                else
                    return (false);
            case EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp == 0)
                    return (true);
                else
                    return (false);
            case NOT_EQUAL:
                cmp = lop.getValue().compareTo(rop.getValue());
                if (cmp != 0)
                    return (true);
                else
                    return (false);
            case REGEXP:
                if (_Debug)
                    System.out.println("Left Operand: '" + lop.getValue() + "'  Right Operand: '" + rop.getValue() + "'");
                try {
//                    RE regexp = new RE(lop.getValue());
//                    return (regexp.isMatch(rop.getValue()));
                      return rop.getValue().matches(lop.getValue());
                }
//                catch (REException e1) {
                catch (PatternSyntaxException e1) {
                    throw new RegExpException(e1.getMessage());
                }
                catch (Exception e2) {
                    throw new SBHException(e2.getMessage());
                }

            default:
                throw new InvalidOperatorException("Unknown Operator Requested! RTFM!");
        }
    }


}


