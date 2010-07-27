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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;

import opendap.dap.BaseType;
//import opendap.dap.NoSuchVariableException;


/**
 * This interface extends the <code>ArrayMethods</code> for OPeNDAP types that
 * extend <code>DArray</code> and <code>DGrid</code> classes. It contains
 * additional projection methods needed by the Server side implementations
 * of these types.
 * <p>A projection for an array must include the start, stride and stop
 * information for each dimension of the array in addition to the basic
 * information that the array itself is <em>projected</em>. This interface
 * provides access to that information.
 *
 * @author jhrg & ndp
 * @version $Revision: 15901 $
 * @see opendap.dap.DArray
 * @see opendap.dap.DGrid
 * @see SDArray
 * @see SDGrid
 * @see ServerMethods
 * @see Operator
 */


public interface ServerArrayMethods extends ServerMethods {

    public void setProjection(int dimension, int start, int stride, int stop)
            throws InvalidParameterException, SBHException;

    public int getStart(int dimension) throws InvalidParameterException;

    public int getStride(int dimension) throws InvalidParameterException;

    public int getStop(int dimension) throws InvalidParameterException;

}


