/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

/**
 * This class holds a reference to a map variable.
 */

public class DapMap extends DapNode
{
    DapAtomicVariable actualvar = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapMap()
    {
        super();
    }

    public DapMap(DapAtomicVariable var)
    {
	    this();
	    setVariable(var);
    }

    //////////////////////////////////////////////////
    // Get/set
    
    public DapAtomicVariable getVariable() {return actualvar;}
    public void setVariable(DapAtomicVariable var) {this.actualvar = var;}

    //////////////////////////////////////////////////
    // Overrides

    public String getShortName()
    {
	    if(actualvar != null)
	        return actualvar.getShortName();
	    return null;
    }

    public String getFQN()
    {
        if(actualvar != null)
            return actualvar.getFQN();
        return null;
    }

} // class DapMap

