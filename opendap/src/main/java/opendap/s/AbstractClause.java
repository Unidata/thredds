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

import java.util.*;

/**
 * Provides default implementations for the Clause interface methods.
 * This eliminates redundant code and provides a starting point
 * for new implementations of Clause. <p>
 * <p/>
 * Note that every Clause object is expected to implement either
 * SubClause or TopLevelClause. No default implementations are provided
 * for the methods of these subinterfaces <p>
 * <p/>
 * Also note that it is not <i>necessary</i> to use this class to
 * create your own implementations, it's just a convenience.<p>
 * <p/>
 * The class has no abstract methods, but is declared abstract because
 * it should not be directly instantiated.
 *
 * @author joew
 */
public abstract class AbstractClause implements Clause {

    public List getChildren() {
        return children;
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isDefined() {
        return defined;
    }

    /**
     * Value to be returned by isConstant(). Should not change for
     * the lifetime of the object.
     */
    protected boolean constant;

    /**
     * Value to be returned by isDefined(). May change during the
     * lifetime of the object.
     */
    protected boolean defined;

    /**
     * A list of SubClause objects representing
     * the sub-clauses of this clause. Use caution when modifying
     * this list other than at the point of creation, since methods
     * such as evaluate() depend on it.
     */
    protected List children;
}


