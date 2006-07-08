
/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, University of Rhode Island
// ALL RIGHTS RESERVED.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: James Gallagher <jgallagher@gso.uri.edu>
//
/////////////////////////////////////////////////////////////////////////////

package dods.dap.Server;
import dods.dap.BaseType;

/** The RelOps interface defines how each type responds to relational
    operators. Most (all?) types will not have sensible responses to all of
    the relational operators (e.g. DByte won't know how to match a regular
    expression but DString will). For those operators that are nonsensical a
    class should throw InvalidOperator.
    @version $Revision: 1.1 $
    @author jhrg */

public interface RelOps {
    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
}
    
