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




package opendap.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;

import opendap.dap.BaseType;
import opendap.dap.InvalidDimensionException;
import opendap.Server.InvalidParameterException;
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
            throws InvalidDimensionException, SBHException;

    public int getStart(int dimension) throws InvalidDimensionException;

    public int getStride(int dimension) throws InvalidDimensionException;

    public int getStop(int dimension) throws InvalidDimensionException;

}


