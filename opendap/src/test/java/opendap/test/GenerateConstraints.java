/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/*
TODO: add function calls
*/


package opendap.test;

import opendap.dap.*;
import opendap.dap.parser.*;
import opendap.dap.Server.*;
import opendap.test.servers.*;

import java.util.*;
import java.io.*;

// Generate random constraints

public class GenerateConstraints extends TestCeParser
{
    //////////////////////////////////////////////////"
    // Define the constraint pieces for generating"
    // random constraint expressions"

    static final int DFALTNCONSTRAINTS = 100;

    static final long SEED = 37;

    static final Random random = new Random(SEED);

    // Define some probability levels

    // Probability that we use variable as the rhs of a selection
    static final double VARRHSPROB = (1.0/8.0);

    // Probability that we use a multi-valued rsh for a selection
    static final double NVALUESPROB = (1.0/4.0);

    static final String[] dimrangeset = {
            "[0]",
            "[1:9]",
            "[2:2:9]"
    };

    static final String[] dimset = {
            "[0]",
            "[9]",
            "[2]"
    };

    static final String[] opset = {
            "=", "!=", ">", ">=", "<", "<="
    };

    static String valuefor(BaseType bt)
    {
        if(bt instanceof DInt32) return "101";
        if(bt instanceof DFloat32) return "37.0";
        if(bt instanceof DString) return "\"string\"";
        return "<?>";
    }

    static boolean sametype(BaseType b1, BaseType b2)
    {
        if(b1 instanceof DInt32 && b2 instanceof DInt32) return true;
        if(b1 instanceof DFloat32 && b2 instanceof DFloat32) return true;
        if(b1 instanceof DString && b2 instanceof DString) return true;
        return false;
    }

    //////////////////////////////////////////////////

    public GenerateConstraints(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////
    // Control the generation of constraint strings
    int NPROJECTIONS = 3;	// 0..NPROJECTIONS-1
    int NSELECTIONS  = 3;	// 0..NSELECTIONS-1
    int MAXRHSSIZE   = 4;	// 1..MAXRHSSIZE

    List<BaseType> projections = new ArrayList<BaseType>();
    List<BaseType> selections = new ArrayList<BaseType>();
    List<BaseType> valuelist = new ArrayList<BaseType>();

    BaseType choose(List<BaseType> nodeset)
    {
        // choose a random DDS node from a given nodeset
        int choice = random.nextInt(nodeset.size()); // 0..|nodeset|-1
	return nodeset.get(choice);
    }

    BaseType findmatchingvar(BaseType var)
    {
	assert (isprimitive(var));
	BaseType match = null;
	for(BaseType bt: leaves) {
	    if(bt == var) continue;
	    if(sametype(bt,var)) {
		// make sure that this var does not cause conflict 
	        if(selections.contains(bt)) continue;
	        if(projections.contains(bt)) continue;
	        if(valuelist.contains(bt)) continue;
                BaseType parent = containsParent(bt,projections);
                if(parent != null) continue;
		match = bt;
		valuelist.add(bt);
	    }
	}
	return match;
    }

    BaseType containsParent(BaseType node, List<BaseType> list)
    {
	String nodename = node.getLongName();
        for(BaseType parent: list) {
	    String parentname = parent.getLongName();
	    if(nodename.startsWith(parentname)) return parent;
        }
	return null;
    }

    boolean containsChild(BaseType node, List<BaseType> list)
    {
	String nodename = node.getLongName();
        for(BaseType child: list) {
	    String childname = child.getLongName();
	    if(childname.startsWith(nodename)) return true;
        }
	return false;
    }
    void gendimrange(List<BaseType> path, StringBuilder buf)
    {
	for(int i=0;i<path.size();i++) {
	    BaseType bt = path.get(i);
	    int rank = 0;
	    if(bt.getParent() instanceof DArray)
		rank = ((DArray)(bt.getParent())).numDimensions();
	    if(i > 0) buf.append('.');
	    buf.append(bt.getName());
            for (int j = 0; j < rank; j++) {
                String dimprojection = dimrangeset[random.nextInt(dimrangeset.length)];
                buf.append(dimprojection);
            }
	}
    }

    void gendimpoint(List<BaseType> path, StringBuilder buf)
    {
	assert (path.size() > 0);
	for(int i=0;i<path.size();i++) {
	    BaseType bt = path.get(i);
	    int rank = 0;
	    if(bt.getParent() instanceof DArray)
		rank = ((DArray)(bt.getParent())).numDimensions();
	    if(i > 0) buf.append('.');
	    buf.append(bt.getName());
            for (int j= 0; j < rank; j++) {
                String dimprojection = dimset[random.nextInt(dimset.length)];
                buf.append(dimprojection);
            }
	}
    }

    void genprojection(BaseType bt, StringBuilder buf)
    {
	gendimrange(getPath(bt),buf);
    }

    void genvalue(StringBuilder buf, BaseType lhsvar, boolean forceconstant)
    {
	// decide if the rhs is going to be a var or a constant
	boolean isvar = (random.nextDouble() < VARRHSPROB);
        if(isvar && !forceconstant) {
	    BaseType rhsvar = findmatchingvar(lhsvar);
	    if(rhsvar != null)
	        gendimpoint(getPath(rhsvar),buf);
	    else
		genvalue(buf,lhsvar,true); // replace with a constant
	} else { // use a constant
            String rhs = valuefor(lhsvar);
	    while(rhs == null) rhs = valuefor(lhsvar);
	    buf.append(rhs);
	}	
    }

    void genvalueset(StringBuilder buf, BaseType lhsvar)
    {
	// Decide if this will be a multi-valued rhs
	boolean issingleton = (random.nextDouble() >= NVALUESPROB);	
        if(issingleton) {
	    genvalue(buf,lhsvar,false);	
	} else {
	    // Determine the number of of elements in the rhs set (> 1)
	    int setsize = random.nextInt(MAXRHSSIZE)+1; // 1..MAXRHSSIZE
	    while(setsize < 2) 
	        setsize = random.nextInt(MAXRHSSIZE)+1; // 1..MAXRHSSIZE
	    buf.append("{");
	    for(int i=0;i<setsize;i++) {
		if(i > 0) buf.append(",");
		genvalue(buf,lhsvar,false);
	    }
	    buf.append("}");
	}
    }

    void genselection(BaseType lhsvar, StringBuilder buf)
    {
	gendimpoint(getPath(lhsvar),buf);
        String operator = opset[random.nextInt(opset.length)];
	buf.append(operator);
	genvalueset(buf,lhsvar);
    }

    void genconstraint(StringBuilder buf)
    {
        int nprojections = random.nextInt(NPROJECTIONS);
        int nselections = random.nextInt(NSELECTIONS);
	if(nprojections + nselections == 0) nprojections++; // guarantee non-null constraint
	projections = new ArrayList<BaseType>();
	// generate candidate projection list
	while(projections.size() < nprojections) {
	    BaseType node = choose(allnodes);
	    // check for conflicts
	    if(projections.contains(node)) continue; // no duplicates
            if(containsChild(node,projections)) continue; // child => !parent
            BaseType parent = containsParent(node,projections);
            if(parent != null) projections.remove(parent); //parent && child => !parent
            projections.add(node);
	}
	projections = sort(projections); //sort
	for(int i=0;i<projections.size();i++) {
	    BaseType node = projections.get(i);
            if (i > 0) buf.append(",");
            genprojection(node,buf);
        }
	selections = new ArrayList<BaseType>();
	valuelist = new ArrayList<BaseType>();
	// generate candidate selection list
	while(selections.size() < nselections) {
	    BaseType node = choose(leaves);
	    if(selections.contains(node)) continue; // no duplicates
	    // conflict avoidance
	    if(projections.contains(node)) continue; // no duplicates with projection list
            if(containsChild(node,projections)) continue; // child => !parent wrt projection list
            BaseType parent = containsParent(node,projections);
            if(parent != null) continue; //project parent && select child => remove child
            selections.add(node);
	}
        for (int i = 0; i < nselections; i++) {
	    BaseType node = selections.get(i);
            buf.append("&");
            genselection(node,buf);
        }
    }

    //////////////////////////////////////////////////

    public void generate(int nconstraints) throws Exception
    {
	StringBuilder constraint = new StringBuilder();
        for (int i=0;i<nconstraints;i++) {
	    // Parse the DDS to produce a ServerDDS object
	    ServerDDS sdds = new ServerDDS(new test_ServerFactory());
	    StringBufferInputStream teststream = new StringBufferInputStream(testDDS);
	    if(!sdds.parse(teststream))
	        throw new ParseException("Cannot parse DDS");
            collectnodes(sdds);
	    constraint.setLength(0);	
            genconstraint(constraint);
	    System.out.println(constraint.toString());
	}
    }

    public static void main(String args[]) throws Exception {
	int nconstraints = DFALTNCONSTRAINTS;
	if(args.length > 0) {
	    try {
		int n = Integer.parseInt(args[0]);
		if(n > 0) nconstraints = n;
	    } catch (NumberFormatException nfe) {
		System.err.println("GenerateConstraints: non-int argument");
	    }
	}
	try {
            new GenerateConstraints("GenerateConstraints").generate(nconstraints);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
