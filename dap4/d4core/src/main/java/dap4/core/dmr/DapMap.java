/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

/**
 * This class holds a reference to a map variable.
 */

public class DapMap extends DapNode
{
    DapVariable actualvar = null;

    //////////////////////////////////////////////////
    // Constructors

    public DapMap()
    {
        super();
    }

    public DapMap(DapVariable var)
    {
	    this();
	    setVariable(var);
    }

    //////////////////////////////////////////////////
    // Get/set
    
    public DapVariable getVariable() {return actualvar;}
    public void setVariable(DapVariable var) {this.actualvar = var;}

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

