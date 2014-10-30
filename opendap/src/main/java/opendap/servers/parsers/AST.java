package opendap.servers.parsers;

import opendap.servers.*;
import opendap.dap.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: dmh
 * Date: 4/19/11
 * Time: 11:13 AM
 * To change this template use File | Settings | File Templates.
 */
abstract public class AST
{
    CEEvaluator ceEval;
    ClauseFactory clauseFactory;
    BaseTypeFactory factory;
    ServerDDS sdds;

    opendap.servers.parsers.AST root = null;

    void setRoot(opendap.servers.parsers.AST root)
    {
        this.root = root;
    }

    CEEvaluator getCeEval()
    {
        return root.ceEval;
    }

    ClauseFactory getClauseFactory()
    {
        return root.clauseFactory;
    }

    BaseTypeFactory getFactory()
    {
        return root.factory;
    }

    ServerDDS getSdds()
    {
        return root.sdds;
    }


    public AST(List<AST> nodes)
    {
        if(nodes == null) return;
        if(!nodes.contains(this)) nodes.add(this);
    }


    /* Only call on the root node */
    public void init(CEEvaluator ceEval,
                     BaseTypeFactory factory,
                     ClauseFactory clauseFactory,
                     ServerDDS sdds,
                     List<AST> nodes)
    {
        this.ceEval = ceEval;
        this.clauseFactory = clauseFactory;
        this.factory = factory;
        this.sdds = sdds;
        this.root = this;
        for(AST node : nodes) node.setRoot(this);
    }

    // abstract public String toString();

    Clause translate()
        throws DAP2ServerSideException, DAP2Exception, NoSuchFunctionException, NoSuchVariableException
    {
        return null;
    } // translate returns a subclause

    Stack collect(Stack components)// collect returns a stack of variables
        throws DAP2ServerSideException, DAP2Exception, NoSuchFunctionException, NoSuchVariableException
    {
        return null;
    }

    void walk() // walk operates by side effect
        throws DAP2ServerSideException, DAP2Exception, NoSuchFunctionException, NoSuchVariableException
    {
        return;
    }


} // class AST

class ASTconstraint extends AST
{
    List<ASTprojection> projections = null;
    List<ASTclause> selections = null;

    public ASTconstraint(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        if (projections != null)
            for(ASTprojection p : projections) {
                buf.append((first ? "" : ",") + p.toString());
                first = false;
            }
        first = true;
        if (selections != null)
            for(ASTclause c : selections) {
                buf.append(c.toString());
                first = false;
            }
        return buf.toString();
    }
*/

    public void walkConstraint()
        throws DAP2ServerSideException, DAP2Exception
    {
        if(projections != null)
            for(ASTprojection proj : projections) {
                proj.walk(ceEval);
            }
        else
            getCeEval().markAll(true);
        if(selections != null)
            for(ASTclause cl : selections) {
                getCeEval().appendClause(cl.translate());
            }
    }
}

class ASTprojection extends AST
{
    ASTvar var = null;
    ASTfcn fcn = null;

    public ASTprojection(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
	if(var != null)
	    return var.toString();
	else
	    return fcn.toString();
    }
*/

    void walk(CEEvaluator ceEval)
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        if(fcn != null) {
            SubClause subclause = fcn.translate();
            getCeEval().appendClause(subclause);
        } else {
            Stack components = new Stack();
            components = var.collect(components);
            markStackedVariables(components);
        }
    }


    /** Given a stack of BaseType variables, mark these as part of the
     * current projection. This function assumes that if the TOS contains a
     * Ctor type variable, all of its members are to be projected. Also
     * assume  all variables under the TOS are Ctor variables and
     * only the ctor itself is to be projected; the member within the Ctor
     * that is part of the projection will be on the stack, too.
     */

    /**
     * CEEValuator.markstackedvariables()
     * automatically marks all fields of a constructor as projected.
     * This causes problems with printDecl, which automatically recurses
     * on it fields. This means that constructor fields end up printed twice.
     * The original code, with the bad cloning, did not do this for some reason.
     * The solution taken here is mark which nodes were marked by constructor
     * recursion and which were marked directly. This info is then used in
     * printDecl to properly print the fields once and only once.
     */
    private void markStackedVariables(Stack s)
    {
        // Reverse the stack.
        Stack bts = new Stack();
        // LogStream.err.println("Variables to be marked:");
        while(!s.empty()) {
            //LogStream.err.println(((BaseType)s.peek()).getName());
            bts.push(s.pop());
        }

        // For each but the last stack element, set the projection.
        // setProject(true, false) for a ctor type sets the projection for
        // the ctor itself but *does not* set the projection for all its
        // children. Thus, if a user wants the variable S.X, and S contains Y
        // and Z too, S's projection will be set (so serialize will descend
        // into S) but X, Y and Z's projection remain clear. In this example,
        // X's projection is set by the code that follows the while loop.
        // 1/28/2000 jhrg
        while(bts.size() > 1) {
            DAPNode dn = (DAPNode) bts.pop();
            ServerMethods ct = (ServerMethods) dn;
            ct.setProject(true, false);
            //LogStream.err.println("mark singleton: " + dn.getName());
        }
        DAPNode dn = (DAPNode) bts.pop();
        // For the last element, project the entire variable.
        ServerMethods bt = (ServerMethods) dn;
        bt.setProject(true, true);
        //LogStream.err.println("mark all: " + dn.getName()); LogStream.err.flush();
    }


}

class ASTfcn extends ASTvalue
{
    String fcnname = null;
    List<ASTvalue> args = null;

    public ASTfcn(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
	buf.append(fcnname);
	buf.append("(");
        if(args != null)
            for(ASTvalue arg: args) {
	            if(!first) buf.append(","); else {first = false;}
                buf.append(arg.toString());
            }
        buf.append(")");
	return buf.toString();
    }
*/

    SubClause translate()
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        SubClause subclause = null;
        Vector<SubClause> cvtargs = new Vector<SubClause>();
        if(args != null)
            for(ASTvalue arg : args)
                cvtargs.addElement(arg.translate());
        subclause = getClauseFactory().newBTFunctionClause(fcnname, cvtargs);
        return subclause;
    }

}

class ASTvar extends ASTvalue
{
    List<ASTsegment> segments = new ArrayList<ASTsegment>();

    public ASTvar(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
	boolean first = true;
        for(ASTsegment seg : segments) {
            buf.append((first ? "" : ".") + seg.toString());
            first = false;
        }
        return buf.toString();
    }
*/

    Stack collect(Stack components)
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        for(ASTsegment segment : segments) {
            components = segment.collect(components);
        }
        return components;
    }

}

class ASTsegment extends AST
{
    String name = null;    // must be DAP decoded
    List<ASTslice> slices = new ArrayList<ASTslice>();

    public ASTsegment(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(name);
        if (slices != null)
            for(ASTslice slice : slices) {
                buf.append(slice.toString());
            }
        return buf.toString();
    }
*/

    Stack collect(Stack components)
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        BaseType bt = null;
        ServerArrayMethods sam = null;
        components = getSdds().search(name, components);
        if(slices != null && slices.size() > 0) {
            try {
                bt = (BaseType) components.peek();
            } catch (ClassCastException cce) {
                String msg = "Attempt to treat the variable `" + name
                    + "' as if it is an array.";
                throw new DAP2Exception(DAP2Exception.MALFORMED_EXPR, msg);
            }
            if(bt instanceof DGrid) {// project the grid and the coordinate variable
                DGrid grid = ((DGrid) bt);
                bt = grid.getArray();
                sam = (ServerArrayMethods) bt;
                for(int i = 0;i < slices.size();i++) {
                    ASTslice slice = slices.get(i);
                    slice.walk(sam, i);
                }
                // walk the coordinate variables also
                for(int i = 0;i < slices.size();i++) {
                    ASTslice slice = slices.get(i);
                    bt = grid.getVar(i + 1);
                    sam = (ServerArrayMethods) bt;
                    slice.walk(sam, 0);
                }
            } else if(bt instanceof ServerArrayMethods) {
                sam = (ServerArrayMethods) bt;
                for(int i = 0;i < slices.size();i++) {
                    ASTslice slice = slices.get(i);
                    slice.walk(sam, i);
                }
            }
        }
        return components;
    }

}

class ASTslice extends AST
{
    long start = 0;
    long stop = 0;
    long stride = 1;

    public ASTslice(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        if (stride == 1) {
            if (first == last)
                return String.format("[%d]", first);
            else
                return String.format("[%d:%d]", first, last);
        } else
            return String.format("[%d:%d:%d]", first, stride, last);
    }
*/

    void walk(ServerArrayMethods sam, int index)
        throws InvalidDimensionException, SBHException
    {
        sam.setProjection(index, (int) start, (int) stride, (int) stop);
    }
}

class ASTvalue extends AST
{
    ASTconstant constant = null;
    ASTvar var = null; // tag == VAR
    ASTfcn fcn = null; // tag == FUNCTION

    public ASTvalue(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
	if(constant != null)
	    buf.append(constant.toString());
	else if(var != null)
	    buf.append(var.toString());
	else if(fcn != null)
	    buf.append(fcn.toString());
	else
	    assert (false);
        return buf.toString();
    }
*/

    SubClause translate()
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        SubClause subclause = null;
        if(constant != null) {
            subclause = constant.translate();
        } else if(var != null) {
            Stack components = new Stack();
            components = var.collect(components);
            subclause = getClauseFactory().newValueClause((BaseType) components.pop(), false);
        } else if(fcn != null) {
            subclause = fcn.translate();
        } else
            assert (false);
        return subclause;
    }
}

class ASTconstant extends ASTvalue
{
    int tag = 0; // See ExprParsefConstants
    String text = null; // tag == STRINGCONST
    long intvalue = 0; // tag == INTCONST
    double floatvalue = 0.0; // tag == FLOATCONST

    public ASTconstant(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        switch (tag) {
        case STRINGCONST:
            buf.append(String.format("\"%s\"", text));
            break;
        case INTCONST:
            buf.append(String.format("%d", intvalue));
            break;
        case FLOATCONST:
            buf.append(String.format("%.1f", floatvalue));
            break;
        default:
            assert(false);
        }
        return buf.toString();
    }
*/

    SubClause translate()
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        SubClause subclause = null;
        switch (tag) {
        case ExprParserConstants.INTCONST: {
            String s = String.format("%d", intvalue);
            DInt32 i = getFactory().newDInt32(s);
            i.setValue((int) intvalue);
            ((ServerMethods) i).setRead(true);
            ((ServerMethods) i).setProject(true);
            subclause = getClauseFactory().newValueClause(i, false);
        }
        break;
        case ExprParserConstants.FLOATCONST: {
            String s = String.format("%.1f", floatvalue);
            DFloat64 f = getFactory().newDFloat64(s);
            f.setValue(floatvalue);
            subclause = getClauseFactory().newValueClause(f, false);
        }
        break;
        case ExprParserConstants.STRINGCONST: {
            DString s = getFactory().newDString(text);
            s.setValue(text);
            ((ServerMethods) s).setRead(true);
            ((ServerMethods) s).setProject(true);
            subclause = getClauseFactory().newValueClause(s, false);
        }
        break;
        default:
            assert (false);
        }
        return subclause;
    }
}

class ASTclause extends AST
{
    int operator = 0; // See ExprParsefConstants
    ASTvalue lhs = null;
    List<ASTvalue> rhs = null;
    // Coverity[FB.UWF_NULL_FIELD]
    ASTfcn boolfcn = null;

    public ASTclause(List<AST> nodes)
    {
        super(nodes);
    }

/*
    public String toString()
    {
	if(boolfcn != null) {
	    return "&"+boolfcn.toString();
	} else {
            StringBuilder buf = new StringBuilder("&");
  	        buf.append(lhs.toString());
            buf.append(operatorString(operator));
            boolean first = true;
            if (rhs.size() > 1) buf.append("{");
            if (rhs != null)
                for(ASTvalue value : rhs) {
                    buf.append((first ? "" : ",") + value.toString());
                    first = false;
                }
            if (rhs.size() > 1) buf.append("}");
            return buf.toString();
	}
    }
*/

    public Clause translate()
        throws DAP2ServerSideException, DAP2Exception,
        NoSuchFunctionException, NoSuchVariableException
    {
        Clause clause = null;
        if(boolfcn != null)
            clause = boolfcn.translate();
        else {
            Vector<SubClause> cvtrhs = new Vector<SubClause>();
            for(ASTvalue v : rhs) cvtrhs.addElement(v.translate());
            SubClause lhsclause = lhs.translate();
            clause = getClauseFactory().newRelOpClause(operator, lhsclause, cvtrhs);
        }
        return clause;
    }

/*
    static String operatorString(int operator)
    {
        if (operator < 0
            || operator >= operatorImage.length
            || operatorImage[operator] == null)
        LogStream.err.println("Illegal operator");
        return operatorImage[operator];
    }
*/

}
