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

import opendap.dap.BaseType;

/**
 * The RelOps interface defines how each type responds to relational
 * operators. Most (all?) types will not have sensible responses to all of
 * the relational operators (e.g. DByte won't know how to match a regular
 * expression but DString will). For those operators that are nonsensical a
 * class should throw InvalidOperator.
 *
 * @author jhrg
 * @version $Revision: 15901 $
 */

public interface RelOps {
    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
}



