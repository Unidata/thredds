/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the COPYRIGHT file for more information. */

/****************************************************/

package opendap.Server.parsers;

import opendap.dap.*;
import opendap.Server.*;
import opendap.dap.parsers.*;

import static opendap.Server.parsers.CeParser.*;

import java.util.*;


/**
 * The constraint expression parser class. <p>
 * <p/>
 * Because it only makes sense to evaluate CEs when serving data, the
 * BaseTyeFactory <em>must</em> create instances of the SDtype classes, not
 * the Dtype classes. The is because we use the setRead method of the class
 * ServerMethods when creating constants (to ensure that the evaluator
 * doesn't try to read tem from the dataset!).
 *
 * @author (ofExprParser.jj) jhrg
 * @author Dennis Heimbigner
 */

public abstract class Ceparse implements ExprParserConstants
{

    BaseTypeFactory factory;

    Celex lexstate = null;

    Ceparse parsestate = null; /* Slight kludge */

    AST.ASTconstraint ast = null;

    protected int cedebug = 0;

    List<AST> astnodeset = new ArrayList<AST>();


    /**
     * **********************************************
     */
    /* Constructor(s) */
    public Ceparse()
    {
        this(null);
    }

    public Ceparse(BaseTypeFactory factory)
    {
        parsestate = this;
        ast = new AST.ASTconstraint(astnodeset);
        if(factory == null) {
            throw new RuntimeException("Ceparse: no factory specified");
        }
        this.factory = factory;
    }

    /**
     * **********************************************
     */
    /* Access into the CeParser for otherwise inaccessible fields */
    abstract public boolean parse() throws ParseException;

    abstract int getDebugLevel();

    abstract void setDebugLevel(int level);

    /**
     * **********************************************
     */
    /* Utilities */

    String strdup(String s)
    {
        return s;
    }

    void cleanup()
    {
        factory = null;
        lexstate = null;
    }

    /**
     * **********************************************
     */
    /* get/set */
    public AST getAST()
    {
        return ast;
    }

    public List<AST> getASTnodeset()
    {
        return astnodeset;
    }


    /**
     * **********************************************
     */
    /* Parser core */

    void
    projections(Ceparse state, Object list0)
            throws ParseException
    {
        ast.projections = (List<AST.ASTprojection>) list0;
    }

    void
    selections(Ceparse state, Object list0)
            throws ParseException
    {
        ast.selections = (List<AST.ASTclause>) list0;
    }

    Object
    projectionlist(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<AST.ASTprojection> list = (List<AST.ASTprojection>) list0;
        if (list == null) list = new ArrayList<AST.ASTprojection>();
        list.add((AST.ASTprojection) decl);
        return list;
    }

    Object
    projection(Ceparse state, Object varorfcn)
            throws ParseException
    {
        AST.ASTprojection p = new AST.ASTprojection(astnodeset);
            if(varorfcn instanceof AST.ASTvar)
            p.var = (AST.ASTvar)varorfcn;
        else
            p.fcn = (AST.ASTfcn)varorfcn;
        return p;
    }

    Object
    segmentlist(Ceparse state, Object var0, Object decl)
            throws ParseException
    {
        AST.ASTvar var = (AST.ASTvar)var0;
        if(var == null) var = new AST.ASTvar(astnodeset);
        var.segments.add((AST.ASTsegment) decl);
        return var;
    }

    Object
    segment(Ceparse state, Object name, Object slices0)
            throws ParseException
    {
        AST.ASTsegment segment = new AST.ASTsegment(astnodeset);
        segment.name = (String) name;
        segment.slices = (List<AST.ASTslice>) slices0;
        return segment;
    }

    Object
    rangelist(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<AST.ASTslice> list = (List<AST.ASTslice>) list0;
        if (list == null) list = new ArrayList<AST.ASTslice>();
        list.add((AST.ASTslice) decl);
        return list;
    }

    Object
    range(Ceparse state, Object sfirst, Object sstride, Object slast)
            throws ParseException
    {
        AST.ASTslice slice = new AST.ASTslice(astnodeset);
        long first = -1;
        long stride = -1;
        long last = -1;
        /* Note: that incoming arguments are strings; we must convert to long;
           but we do know they are legal integers or null */
        try {
            first = Long.parseLong((String) sfirst);
        } catch (NumberFormatException nfe) {
        }
        ;
        if (slast != null) {
            try {
                last = Long.parseLong((String) slast);
            } catch (NumberFormatException nfe) {
            }
            ;
        } else
            last = first;
        if (sstride != null) {
            try {
                stride = Long.parseLong((String) sstride);
            } catch (NumberFormatException nfe) {
            }
            ;
        } else
            stride = 1; /* default */

        if (first < 0)
            throw new ParseException("Illegal index for range first index");
        if (stride < 0)
            throw new ParseException("Illegal index for range stride index");
        if (last < 0)
            throw new ParseException("Illegal index for range last index");

        if (last < first)
            throw new ParseException("Range last index less than first index");


        slice.first = first;
        slice.stride = stride;
        slice.last = last;

        return slice;
    }

    Object range1(Ceparse state, Object rangenumber)
        throws ParseException
    {
        try {
            Long.parseLong((String)rangenumber);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Index is not an integer");
        }
        return rangenumber;
    }

    /* Selection Procedures */

    Object
    clauselist(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<AST.ASTclause> list = (List<AST.ASTclause>) list0;
        if (list == null) list = new ArrayList<AST.ASTclause>();
        list.add((AST.ASTclause) decl);
        return list;
    }

    Object
    sel_clause(Ceparse state, int selcase,
               Object lhs, Object relop0, Object values)
            throws ParseException
    {
        AST.ASTclause sel = new AST.ASTclause(astnodeset);
        sel.lhs =  (AST.ASTvalue)lhs;
        sel.operator = (Integer) relop0;
        sel.lhs = (AST.ASTvalue)lhs;
        if (selcase == 2) { // singleton value
            sel.rhs = new ArrayList<AST.ASTvalue>();
            sel.rhs.add((AST.ASTvalue) values);
        } else
            sel.rhs = (List<AST.ASTvalue>) values;
        return sel;
    }

    Object
    indexpath(Ceparse state, Object list0, Object index)
            throws ParseException
    {
        List<AST.ASTsegment> list = (List<AST.ASTsegment>) list0;
        if (list == null) list = new ArrayList<AST.ASTsegment>();
        list.add((AST.ASTsegment)index);
        return list;
    }

    Object
    array_indices(Ceparse state, Object list0, Object indexno)
            throws ParseException
    {
        List<AST.ASTslice> list = (List<AST.ASTslice>) list0;
        if (list == null) list = new ArrayList<AST.ASTslice>();
        long start = 0;
        try {start = Long.parseLong((String)indexno);} catch (NumberFormatException nfe) {/*already checked*/};
        AST.ASTslice slice = new AST.ASTslice(astnodeset);
        slice.first = start;
        slice.stride = 1;
        slice.last = start;
        list.add(slice);
        return list;
    }

    Object
    index(Ceparse state, Object name, Object indices)
            throws ParseException
    {
        AST.ASTsegment segment = new AST.ASTsegment(astnodeset);
        segment.name = (String) name;
        segment.slices = (List<AST.ASTslice>)indices;
        return segment;
    }

    Object
    function(Ceparse state, Object fcnname, Object args)
            throws ParseException
    {
        AST.ASTfcn fcn = new AST.ASTfcn(astnodeset);
        fcn.fcnname = (String)fcnname;
        fcn.args = (List<AST.ASTvalue>)args;
        return fcn;
    }

    Object
    arg_list(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<AST.ASTvalue> list = (List<AST.ASTvalue>) list0;
        if (list == null) list = new ArrayList<AST.ASTvalue>();
        list.add((AST.ASTvalue) decl);
        return list;
    }

    Object
    value_list(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<AST.ASTvalue> list = (List<AST.ASTvalue>) list0;
        if (list == null) list = new ArrayList<AST.ASTvalue>();
        list.add((AST.ASTvalue) decl);
        return list;
    }

    Object
    value(Ceparse state, Object o)
            throws ParseException
    {
        AST.ASTvalue value = new AST.ASTvalue(astnodeset);
        if(o instanceof AST.ASTvar) value.var = (AST.ASTvar)o;
        else if(o instanceof AST.ASTfcn) value.fcn = (AST.ASTfcn)o;
        else if(o instanceof AST.ASTconstant) value.constant = (AST.ASTconstant)o;
        return value;
    }

    Object
    var(Ceparse state, Object indexpath)
            throws ParseException
    {
        AST.ASTvar var = new AST.ASTvar(astnodeset);
        var.segments = (List<AST.ASTsegment>) indexpath;
        return var;
    }

    Object
    constant(Ceparse state, Object path, int tag)
            throws ParseException
    {
        AST.ASTconstant value = new AST.ASTconstant(astnodeset);
        switch (tag) {
        case SCAN_STRINGCONST:
            value.text = (String) path;
            value.tag = STRINGCONST;
            break;
        case SCAN_NUMBERCONST:
            try {
                value.intvalue = Long.parseLong((String) path);
                value.tag = INTCONST;
            } catch (NumberFormatException nfe) {
                try {
                    value.floatvalue = Float.parseFloat((String) path);
                    value.tag = FLOATCONST;
                } catch (NumberFormatException nfe2) {
                    throw new ParseException("Illegal integer constant");
                }
            }
            break;
        default:
            assert(false);
        }
        return value;
    }

    //////////////////////////////////////////////////
    // Convert AST.AST to CeEval


    /**************************************************/
    /* Misc functions from original expr.jj */

    /**
     * Run the named projection function. Projection functions are run for
     * their side effect; the return value is discarded.
     *
     * @param name The name of the projection function, look this up in the
     *             ServerDDS.
     * @param btv  A vector of BaseType variables that are the arguments of
     *             the projection function.
     */
    void runProjectionFunction(String name, Vector btv)
    {
    }

    /**
     * Remove double quotes from around a string. If there's not both start
     * and ending quotes, does nothing.
     *
     * @param s The source string.
     * @return The string without double quotes.
     */
    String removeQuotes(String s)
    {
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        else
            return s;
    }

    /**
     * Given a stack of BaseType variables, mark these as part of the
     * current projection. This function assumes that if the TOS contains a
     * Ctor type variable, all of its members are to be projected. Also
     * assume  all variables under the TOS are Ctor variables and
     * only the ctor itself is to be projected; the member within the Ctor
     * that is part of the projection will be on the stack, too.
     */
    void markStackedVariables(Stack s)
    {
        // Reverse the stack.
        Stack bts = new Stack();
        // System.err.println("Variables to be marked:");
        while (!s.empty()) {
            // System.err.println(((BaseType)s.peek()).getName());
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
        while (bts.size() > 1) {
            ServerMethods ct = (ServerMethods) bts.pop();
            ct.setProject(true, false);
        }

        // For the last element, project the entire variable.
        ServerMethods bt = (ServerMethods) bts.pop();
        bt.setProject(true, true);
    }

    /**
     * *********************
     */
    // This parses, then fills in the evaluator from the AST.AST
    public boolean constraint_expression(CEEvaluator ceEval,
                                         BaseTypeFactory factory,
                                         ClauseFactory clauseFactory)
            throws DAP2Exception, ParseException
    {
        ServerDDS sdds = ceEval.getDDS();
        if (!parse()) return false;
        ast.init(ceEval,factory,clauseFactory,sdds,getASTnodeset());
        ast.walkConstraint();
        return true;
    }

}
