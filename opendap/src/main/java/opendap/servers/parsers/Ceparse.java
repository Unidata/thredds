/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the COPYRIGHT file for more information. */

/****************************************************/

package opendap.servers.parsers;

import opendap.dap.*;
import opendap.servers.*;
import opendap.dap.parsers.*;
import ucar.nc2.util.EscapeStrings;

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
    //////////////////////////////////////////////////////////////////////////
    static public org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(Ceparse.class);
     //////////////////////////////////////////////////////////////////////////

    BaseTypeFactory factory;

    Celex lexstate = null;

    Ceparse parsestate = null; /* Slight kludge */

    ASTconstraint ast = null;

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
        ast = new ASTconstraint(astnodeset);
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

    abstract public int getDebugLevel();
    abstract public void setDebugLevel(int level);

    abstract public void setURL(String url);
    abstract public String getURL();

    abstract public void setConstraint(String constraint);
    abstract public String getConstraint();

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
        ast.projections = (List<ASTprojection>) list0;
    }

    void
    selections(Ceparse state, Object list0)
            throws ParseException
    {
        ast.selections = (List<ASTclause>) list0;
    }

    Object
    projectionlist(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<ASTprojection> list = (List<ASTprojection>) list0;
        if (list == null) list = new ArrayList<ASTprojection>();
        list.add((ASTprojection) decl);
        return list;
    }

    Object
    projection(Ceparse state, Object varorfcn)
            throws ParseException
    {
        ASTprojection p = new ASTprojection(astnodeset);
            if(varorfcn instanceof ASTvar)
            p.var = (ASTvar)varorfcn;
        else
            p.fcn = (ASTfcn)varorfcn;
        return p;
    }

    Object
    segmentlist(Ceparse state, Object var0, Object decl)
            throws ParseException
    {
        ASTvar var = (ASTvar)var0;
        if(var == null) var = new ASTvar(astnodeset);
        var.segments.add((ASTsegment) decl);
        return var;
    }

    Object
    segment(Ceparse state, Object name, Object slices0)
            throws ParseException
    {
        ASTsegment segment = new ASTsegment(astnodeset);
        segment.name = (String) name;
        segment.slices = (List<ASTslice>) slices0;
        return segment;
    }

    Object
    rangelist(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<ASTslice> list = (List<ASTslice>) list0;
        if (list == null) list = new ArrayList<ASTslice>();
        list.add((ASTslice) decl);
        return list;
    }

    Object
    range(Ceparse state, Object sstart, Object sstride, Object sstop)
            throws ParseException
    {
        ASTslice slice = new ASTslice(astnodeset);
        int start = -1;
        int stride = -1;
        int stop = -1;
        /* Note: that incoming arguments are strings; we must convert to long;
           but we do know they are legal integers or null */
        try {
            start = Integer.parseInt((String) sstart);
        } catch (NumberFormatException nfe) {
        }
        ;
        if (sstop != null) {
            try {
                stop = Integer.parseInt((String) sstop);
            } catch (NumberFormatException nfe) {
            }
            ;
        } else
            stop = start;
        if (sstride != null) {
            try {
                stride = Integer.parseInt((String) sstride);
            } catch (NumberFormatException nfe) {
            }
            ;
        } else
            stride = 1; /* default */

        if (start < 0)
            throw new ParseException("Illegal index for range first index");
        if (stride <= 0)
            throw new ParseException("Illegal index for range stride index");
        if (stop < 0)
            throw new ParseException("Illegal index for range last index");

        if (stop < start)
            throw new ParseException("Range last index less than first index");


        slice.start = start;
        slice.stride = stride;
        slice.stop = stop;

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
        List<ASTclause> list = (List<ASTclause>) list0;
        if (list == null) list = new ArrayList<ASTclause>();
        list.add((ASTclause) decl);
        return list;
    }

    Object
    sel_clause(Ceparse state, int selcase,
               Object lhs, Object relop0, Object values)
            throws ParseException
    {
        ASTclause sel = new ASTclause(astnodeset);
        sel.lhs =  (ASTvalue)lhs;
        sel.operator = (Integer) relop0;
        sel.lhs = (ASTvalue)lhs;
        if (selcase == 2) { // singleton value
            sel.rhs = new ArrayList<ASTvalue>();
            sel.rhs.add((ASTvalue) values);
        } else
            sel.rhs = (List<ASTvalue>) values;
        return sel;
    }

    Object
    indexpath(Ceparse state, Object list0, Object index)
            throws ParseException
    {
        List<ASTsegment> list = (List<ASTsegment>) list0;
        if (list == null) list = new ArrayList<ASTsegment>();
        list.add((ASTsegment)index);
        return list;
    }

    Object
    array_indices(Ceparse state, Object list0, Object indexno)
            throws ParseException
    {
        List<ASTslice> list = (List<ASTslice>) list0;
        if (list == null) list = new ArrayList<ASTslice>();
        long start = 0;
        try {start = Long.parseLong((String)indexno);} catch (NumberFormatException nfe) {/*already checked*/};
        ASTslice slice = new ASTslice(astnodeset);
        slice.start = start;
        slice.stride = 1;
        slice.stop = start;
        list.add(slice);
        return list;
    }

    Object
    index(Ceparse state, Object name, Object indices)
            throws ParseException
    {
        ASTsegment segment = new ASTsegment(astnodeset);
        segment.name = (String) name;
        segment.slices = (List<ASTslice>)indices;
        return segment;
    }

    Object
    function(Ceparse state, Object fcnname, Object args)
            throws ParseException
    {
        ASTfcn fcn = new ASTfcn(astnodeset);
        fcn.fcnname = (String)fcnname;
        fcn.args = (List<ASTvalue>)args;
        return fcn;
    }

    Object
    arg_list(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<ASTvalue> list = (List<ASTvalue>) list0;
        if (list == null) list = new ArrayList<ASTvalue>();
        list.add((ASTvalue) decl);
        return list;
    }

    Object
    value_list(Ceparse state, Object list0, Object decl)
            throws ParseException
    {
        List<ASTvalue> list = (List<ASTvalue>) list0;
        if (list == null) list = new ArrayList<ASTvalue>();
        list.add((ASTvalue) decl);
        return list;
    }

    Object
    value(Ceparse state, Object o)
            throws ParseException
    {
        ASTvalue value = new ASTvalue(astnodeset);
        if(o instanceof ASTvar) value.var = (ASTvar)o;
        else if(o instanceof ASTfcn) value.fcn = (ASTfcn)o;
        else if(o instanceof ASTconstant) value.constant = (ASTconstant)o;
        return value;
    }

    Object
    var(Ceparse state, Object indexpath)
            throws ParseException
    {
        ASTvar var = new ASTvar(astnodeset);
        var.segments = (List<ASTsegment>) indexpath;
        return var;
    }

    Object
    constant(Ceparse state, Object path, int tag)
            throws ParseException
    {
        ASTconstant value = new ASTconstant(astnodeset);
        switch (tag) {
        case CeParser.Lexer.SCAN_STRINGCONST:
            value.text = (String) path;
            value.tag = STRINGCONST;
            break;
        case CeParser.Lexer.SCAN_NUMBERCONST:
            try {
                value.intvalue = Long.parseLong((String) path);
                value.tag = INTCONST;
            } catch (NumberFormatException nfe) {
                try {
                    value.floatvalue = Float.parseFloat((String) path);
                    value.tag = FLOATCONST;
                } catch (NumberFormatException nfe2) {
                    throw new ParseException("Illegal float constant");
                }
            }
            break;
        default:
            assert(false);
        }
        return value;
    }

    //////////////////////////////////////////////////
    // Utils

    String unescapeDAPName(Object name)
    {
        return EscapeStrings.unescapeDAPIdentifier((String) name);
    }

    //////////////////////////////////////////////////
    // Misc functions from original expr.jj

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
        // LogStream.err.println("Variables to be marked:");
        while (!s.empty()) {
            // LogStream.err.println(((BaseType)s.peek()).getName());
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
    // This parses, then fills in the evaluator from the AST
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
