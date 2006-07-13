/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Stack;
import java.io.*;

import dods.dap.parser.DDSParser;
import dods.dap.parser.ParseException;

/**
 * The DODS Data Descriptor Object (DDS) is a data structure used by
 * the DODS software to describe datasets and subsets of those
 * datasets.  The DDS may be thought of as the declarations for the
 * data structures that will hold data requested by some DODS client.
 * Part of the job of a DODS server is to build a suitable DDS for a
 * specific dataset and to send it to the client.  Depending on the
 * data access API in use, this may involve reading part of the
 * dataset and inferring the DDS.  Other APIs may require the server
 * simply to read some ancillary data file with the DDS in it.
 * <p>
 * On the server side, in addition to the data declarations, the DDS
 * holds the clauses of any constraint expression that may have
 * accompanied the data request from the DODS client.  The DDS object
 * includes methods for modifying the DDS according to the given
 * constraint expression.  It also has methods for directly modifying
 * a DDS, and for transmitting it from a server to a client.
 * <p>
 * For the client, the DDS object includes methods for reading the
 * persistent form of the object sent from a server. This includes parsing
 * the ASCII representation of the object and, possibly, reading data
 * received from a server into a data object.
 * <p>
 * Note that the class DDS is used to instantiate both DDS and DataDDS
 * objects. A DDS that is empty (contains no actual data) is used by servers
 * to send structural information to the client. The same DDS can becomes a
 * DataDDS when data values are bound to the variables it defines.
 * <p>
 * For a complete description of the DDS layout and protocol, please
 * refer to <em>The DODS User Guide</em>.
 * <p>
 * The DDS has an ASCII representation, which is what is transmitted
 * from a DODS server to a client.  Here is the DDS representation of
 * an entire dataset containing a time series of worldwide grids of
 * sea surface temperatures:
 *
 * <blockquote><pre>
 *  Dataset {
 *      Float64 lat[lat = 180];
 *      Float64 lon[lon = 360];
 *      Float64 time[time = 404];
 *      Grid {
 *       ARRAY:
 *          Int32 sst[time = 404][lat = 180][lon = 360];
 *       MAPS:
 *          Float64 time[time = 404];
 *          Float64 lat[lat = 180];
 *          Float64 lon[lon = 360];
 *      } sst;
 *  } weekly;
 * </pre></blockquote>
 *
 * If the data request to this dataset includes a constraint
 * expression, the corresponding DDS might be different.  For
 * example, if the request was only for northern hemisphere data
 * at a specific time, the above DDS might be modified to appear like
 * this:
 *
 * <blockquote><pre>
 *  Dataset {
 *      Grid {
 *       ARRAY:
 *          Int32 sst[time = 1][lat = 90][lon = 360];
 *       MAPS:
 *          Float64 time[time = 1];
 *          Float64 lat[lat = 90];
 *          Float64 lon[lon = 360];
 *      } sst;
 *  } weekly;
 * </pre></blockquote>
 *
 * Since the constraint has narrowed the area of interest, the range
 * of latitude values has been halved, and there is only one time
 * value in the returned array.  Note that the simple arrays (<em>lat</em>,
 * <em>lon</em>, and <em>time</em>) described in the dataset are also part of
 * the <em>sst</em> Grid object.  They can be requested by themselves or as
 * part of that larger object.
 * <p>
 * See <em>The DODS User Guide</em>, or the documentation of the
 * BaseType class for descriptions of the DODS data types.
 *
 * @version $Revision: 48 $
 * @author jehamby
 * @see BaseType
 * @see BaseTypeFactory
 * @see DAS
 */
public class DDS implements Cloneable {
    /** The dataset name. */
    protected String name;

    /** Variables at the top level. */
    protected Vector vars;

    /** Factory for new DAP variables. */
    private BaseTypeFactory factory;

    /** Creates an empty <code>DDS</code>. */
    public DDS() {
        this(null, new DefaultFactory());
    }

    /**
    * Creates an empty <code>DDS</code> with the given dataset name.
    * @param n the dataset name
    */
    public DDS(String n) {
        this(n, new DefaultFactory());
    }

    /**
    * Creates an empty <code>DDS</code> with the given
    * <code>BaseTypeFactory</code>.  This will be used for DODS servers which
    * need to construct subclasses of the various <code>BaseType</code> objects
    * to hold additional server-side information.
    *
    * @param factory the server <code>BaseTypeFactory</code> object.
    */
    public DDS(BaseTypeFactory factory) {
        this(null, factory);
    }

    /**
    * Creates an empty <code>DDS</code> with the given dataset name and
    * <code>BaseTypeFactory</code>.  This will be used for DODS servers which
    * need to construct subclasses of the various <code>BaseType</code> objects
    * to hold additional server-side information.
    *
    * @param n the dataset name
    * @param factory the server <code>BaseTypeFactory</code> object.
    */
    public DDS(String n, BaseTypeFactory factory) {
        name = n;
        vars = new Vector();
        this.factory = factory;
    }

    /**
    * Returns a clone of this <code>DDS</code>.  A deep copy is performed on
    * all variables inside the <code>DDS</code>.
    *
    * @return a clone of this <code>DDS</code>.
    */
    public Object clone() {
      BaseType element;
      Object clone;
      try {
        DDS d = (DDS) super.clone();
        d.vars = new Vector();
        for (int i = 0; i < vars.size(); i++) {
          element = (BaseType) vars.elementAt(i);
          clone = element.clone();
          d.vars.addElement(clone);
        }
        d.name = new String(this.name);

        // Question:
        // What about copying the BaseTypeFactory?
        // Do we want a reference to the same one? Or another instance?
        // Is there a difference? Should we be building the clone
        // using "new DDS(getFactory())"??

        // Answer:
        // Yes. Use the same type factory!

        d.factory = this.factory;


        return d;
      }
      catch (CloneNotSupportedException e) {
        // this shouldn't happen, since we are Cloneable
        throw new InternalError();
      }
      catch (NullPointerException e) { // debug
        e.printStackTrace();
        throw e;
      }
    }

    /**
    * Get the Class factory.  This is the machine that builds classes
    * for the internal representation of the data set.
    *
    * @return the BaseTypeFactory.
    */
    public final BaseTypeFactory getFactory() {
        return factory;
    }

    /**
    * Get the dataset's name.  This is the name of the dataset
    * itself, and is not to be confused with the name of the file or
    * disk on which it is stored.
    *
    * @return the name of the dataset.
    */
    public final String getName() {
        return name;
    }

    /**
    * Set the dataset's name.  This is the name of the dataset
    * itself, and is not to be confused with the name of the file or
    * disk on which it is stored.
    *
    * @param name the name of the dataset.
    */
    public final void setName(String name) {
        this.name = name;
    }

    /**
    * Adds a variable to the <code>DDS</code>.
    *
    * @param bt the new variable to add.
    */
    public final void addVariable(BaseType bt) {
        vars.addElement(bt);
    }

    /**
    * Removes a variable from the <code>DDS</code>.
    * Does nothing if the variable can't be found.
    * If there are multiple variables with the same name, only the first
    * will be removed.  To detect this, call the <code>checkSemantics</code>
    * method to verify that each variable has a unique name.
    *
    * @param name the name of the variable to remove.
    * @see DDS#checkSemantics(boolean)
    */
    public final void delVariable(String name) {
        try {
            BaseType bt = getVariable(name);
            vars.removeElement(bt);
        }
        catch (NoSuchVariableException e) {}
    }

    /** Is the variable <code>var</code> a vector of DConstructors? Return
    *   true if it is, false otherwise. This mess will recurse into a
    *   DVector's template BaseType (which is a BaseTypePrimivitiveVector) and
    *   look to see if that is either a DConstructor or <em>contains</em> a
    *   DConstructor. So the <code>List Strucutre { ... } g[10];</code> should
    *   be handled correctly. <p>
    *
    * Note that the List type modifier may only appear once. */
    private DConstructor isVectorOfDConstructor(BaseType var) {
	if (!(var instanceof DVector))
	    return null;
	if (!(((DVector)var).getPrimitiveVector() 
	      instanceof BaseTypePrimitiveVector))
	    return null;
	// OK. We have a DVector whose template is a BaseTypePrimitiveVector.
	BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector)
	    ((DVector)var).getPrimitiveVector();
	// After that nasty cast, is the template a DConstructor?
	if (btpv.getTemplate() instanceof DConstructor)
	    return (DConstructor)btpv.getTemplate();
	else
	    return isVectorOfDConstructor(btpv.getTemplate());
    }

    /**
    * Returns a reference to the named variable. 
    *
    * @param name the name of the variable to return.
    * @return the variable named <code>name</code>.
    * @exception NoSuchVariableException if the variable isn't found.
    */
    public BaseType getVariable(String name) throws NoSuchVariableException {
        Stack s = new Stack();
        s = search(name, s);
        return (BaseType)s.pop();
    }

    /** Look for <code>name</code> in the DDS. Start the search using the
     * ctor variable (or array/list of ctors) found on the top of the Stack
     * <code>compStack</code> (for component stack). When the named variable
     * is found, return the stack compStack modified so that it now contains
     * each ctor-type variable that on the path to the named variable. If the
     * variable is not found after exhausting all possibilities, throw
     * NoSuchVariable.<p>
     *
     * Note: This method takes the stack as a parameter so that it can be
     * used by a parser that is working through a list of identifiers that
     * represents the path to a variable <em>as well as</em> a shorthand
     * notation for the identifier that is the equivalent to the leaf node
     * name alone. In the form case the caller helps build the stack by
     * repeatedly calling <code>search</code>, in the latter case this method
     * must build the stack itself. This method is over kill for the first
     * case.
     *
     @param name Search for the named variable.
     @param compStack The component stack. This holds the BaseType variables
     * that match the each component of a specific variable's name. This
     * method starts its search using the element at the top of the stack and
     * adds to the stack. The order of BaseType variables on the stack is the
     * reverse of the tree-traverse order. That is, the top most element on
     * the stack is the BaseType for the named variable, <em>under</em> that
     * is the named variable's parent and so on.
     @return A stack of BaseType variables which hold the path from the top
     * of the DDS to the named variable.
     @exception NoSuchVariableException */
    public Stack search(String name, Stack compStack)
	throws NoSuchVariableException
    {
	DDSSearch ddsSearch = new DDSSearch(compStack);

	if (ddsSearch.deepSearch(name))
	    return ddsSearch.components;
	else
	    throw new NoSuchVariableException("The variable `" + name 
				      + "' was not found in the dataset.");
    }

    /** Find variables in the DDS when users name them with either fully- or
     * partially-qualified names. */
    private final class DDSSearch {
	Stack components;

	DDSSearch(Stack c) {
	    components = c;
	}

	BaseType simpleSearch(String name, BaseType start) {
	    Enumeration e = null;
	    DConstructor dcv;
	    if (start == null)
		e = getVariables(); // Start with the whole DDS
	    else if (start instanceof DConstructor)
		e = ((DConstructor)start).getVariables();
	    else if ((dcv = isVectorOfDConstructor(start)) != null)
		e = dcv.getVariables();
	    else 
		return null;

	    while (e.hasMoreElements()) {
		BaseType v = (BaseType)e.nextElement();
		if (v.getName().equals(name)) {
		    return v;
		}
	    }

	    return null;	// Not found
	}
	    
	/** Look for the variable named <code>name</code>. First perform the
	 * shallow search (see simpleSearch) and then look through all the
	 * ctor variables. If there are no more ctors to check and the
	 * variable has not been found, return false.
	 *
	 * Note that this method uses the return value to indicate whether a
	 * particular invocation found <code>name</code>. */
	boolean deepSearch(String name) throws NoSuchVariableException {
	
	    BaseType start = components.empty() ? null 
		: (BaseType)components.peek();
		
	    BaseType found;
	    
	    if ((found = simpleSearch(name, start)) != null) {
		components.push(found);
		return true;
	    }
	
	    Enumeration e;
	    DConstructor dcv;
	    if (start == null)
		e = getVariables(); // Start with the whole DDS
	    else if (start instanceof DConstructor)
		e = ((DConstructor)start).getVariables();
	    else if ((dcv = isVectorOfDConstructor(start)) != null)
		e = dcv.getVariables();
	    else 
		return false;

	    while (e.hasMoreElements()) {
		BaseType v = (BaseType)e.nextElement();
		components.push(v);
		if (deepSearch(name))
		    return true;
		else
		    components.pop();
	    }
	
	    // This second return takes care of the case where a dataset
	    // lists a bunch of ctor variable, one after another. Once the
	    // first ctor (say a Grid) has been searched returning false to
	    // the superior invocation of deepSearch pops it off the stack
	    // and the while loop will search starting with the next variable
	    // in the DDS. 
	    return false;
	}
    }	    

    /**
    * Returns an <code>Enumeration</code> of the dataset variables.
    *
    * @return an <code>Enumeration</code> of <code>BaseType</code>.
    */
    public final Enumeration getVariables() {
        return vars.elements();
    }

    /**
    * Returns the number of variables in the dataset.
    *
    * @return the number of variables in the dataset.
    */
    public final int numVariables() {
        return vars.size();
    }

  /**
   * Reads a <code>DDS</code> from the named <code>InputStream</code>. This
   * method calls a generated parser to interpret an ASCII representation of a
   * <code>DDS</code>, and regenerate that <code>DDS</code> in memory.
   *
   * @param is the InputStream containing the <code>DDS</code> to parse.
   * @exception ParseException thrown on a parser error.
   * @exception DDSException thrown on an error constructing the
   *    <code>DDS</code>.
   * @see dods.dap.parser.DDSParser
   */
  public void parse(InputStream is) throws ParseException, DDSException {
    DDSParser dp = new DDSParser(is);
    dp.Dataset(this, factory);
  }

  /**
   * Check the semantics of the <code>DDS</code>. If
   * <code>all</code> is true, check not only the semantics of the
   * <code>DDS</code> itself, but also recursively check all variables
   * in the dataset.
   *
   * @param all this flag indicates whether to check the semantics of the
   *      member variables, too.
   * @exception BadSemanticsException if semantics are bad
   */
  public void checkSemantics(boolean all)
                             throws BadSemanticsException {
    if (name == null) {
      System.err.println("A dataset must have a name");
      throw new BadSemanticsException("DDS.checkSemantics(): A dataset must have a name");
    }
    Util.uniqueNames(vars, name, "Dataset");

    if (all) {
      for (Enumeration e = vars.elements(); e.hasMoreElements() ;) {
	BaseType bt = (BaseType)e.nextElement();
	bt.checkSemantics(true);
      }
    }
  }

  /**
   * Check the semantics of the <code>DDS</code>.  Same as
   * <code>checkSemantics(false)</code>.
   *
   * @exception BadSemanticsException if semantics are bad
   * @see DDS#checkSemantics(boolean)
   */
  public final void checkSemantics() throws BadSemanticsException {
    checkSemantics(false);
  }

    /**
    * Print the <code>DDS</code> on the given <code>PrintWriter</code>.
    *
    * @param os the <code>PrintWriter</code> to use for output.
    */
    public void print(PrintWriter os) {
        os.println("Dataset {");
        for(Enumeration e = vars.elements(); e.hasMoreElements(); ) {	
            BaseType bt = (BaseType)e.nextElement();

	    
            bt.printDecl(os);
        }
        os.print("} ");
        if(name != null)
            os.print(name);
        os.println(";");
    }

  /**
   * Print the <code>DDS</code> on the given <code>OutputStream</code>.
   *
   * @param os the <code>OutputStream</code> to use for output.
   * @see DDS#print(PrintWriter)
   */
  public final void print(OutputStream os) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    print(pw);
    pw.flush();
  }
  
  
/*
    public void toASCII(PrintWriter pw, boolean addName){  
        
	String s = "";
	Enumeration e = getVariables();
	
	//System.out.println("DDS.toASCII("+addName+")  getName(): "+getName());
	while(e.hasMoreElements()){
	    BaseType bt = (BaseType)e.nextElement();
	    //System.out.println("Type: "+bt.getClass().getName());
	    //bt.toASCII(pw,addName,getNAme(),true);
	    bt.toASCII(pw,addName,null,true);
	}


    }
*/



  
}
