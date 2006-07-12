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
import java.io.PrintWriter;

/**
 * Contains methods used only by the DODS constructor classes
 * (<code>DStructure</code>, <code>DSequence</code>, <code>DGrid</code>, and
 * <code>DList</code>).
 *
 * @version $Revision$
 * @author jehamby
 * @see DStructure
 * @see DSequence
 * @see DGrid
 * @see DList
 */
abstract public class DConstructor extends BaseType {

//    /** A <code>Vector</code> of DODS BaseTypes to be used by my children */
//    protected Vector vars;

    /** Constructs a new <code>DConstructor</code>. */
    public DConstructor() {}

    /**
    * Constructs a new <code>DConstructor</code> with the given name.
    * @param n The name of the variable.
    */
    public DConstructor(String n) { super(n); }

    /**
    * Adds a variable to the container.
    * @param v the variable to add.
    * @param part The part of the constructor data to be modified.
    */
    abstract public void addVariable(BaseType v, int part);

    /**
    * Adds a variable to the container.  Same as <code>addVariable(v, 0)</code>.
    * @param v the variable to add.
    */
    public final void addVariable(BaseType v) {
        addVariable(v, 0);
    }
  
    /**
    * Gets the named variable.
    * @param name the name of the variable.
    * @return the named variable.
    * @exception NoSuchVariableException if the named variable does not
    *     exist in this container.
    */
    abstract public BaseType getVariable(String name) 
       throws NoSuchVariableException;

    /**
    * Gets the indexed variable. For a DGrid the index 0 returns the <code>DArray</code> and 
    * indexes 1 and higher return the associated map <code>Vector</code>s. 
    * @param index the index of the variable in the <code>Vector</code> Vars.
    * @return the named variable.
    * @exception NoSuchVariableException if the named variable does not
    *     exist in this container.
    */
    abstract public BaseType getVar(int index) 
        throws NoSuchVariableException;

    /** Return an Enumeration that can be used to iterate over all of the
     * members of the class. Each implementation must define what this means.
     * The intent of this method is to support operations on all members of a
     * Structure, Seqeunce or Grid that can be performed equally. So it is
     * not necessary that this methods be usable, for example, when the
     * caller needs to know that it s dealing with the Array part of a grid.
     @return An Enumeration object. */
    abstract public Enumeration getVariables();
    
    
    

    
    
    
}

