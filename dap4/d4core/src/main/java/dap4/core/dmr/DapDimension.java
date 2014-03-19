/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

/**
 * This class defines a non-Dimensiond Dimension:
 * i.e. one with an atomic type.
 */

public class DapDimension extends DapNode implements DapDecl, Cloneable
{

    //////////////////////////////////////////////////
    // Constants

    static final public long VARIABLELENGTH = -1;
    static final public long UNDEFINED = -2;

    // Provide a single instance of the variable length dimension.
    static final public DapDimension VLEN =
	new DapDimension("*",VARIABLELENGTH);

    //////////////////////////////////////////////////
    // Instance variables

    long size = 0;

    boolean isshared = false;


    //////////////////////////////////////////////////
    // Constructors

    public DapDimension()
    {
        super();
    }

    public DapDimension(String name)
    {
        this();
        setShortName(name);
    }

    public DapDimension(String name, long size)
    {
        this(name);
        setSize(size);
    }

    public DapDimension(long size)
    {
        setSize(size);
	this.isshared = false;
    }

    //////////////////////////////////////////////////
    // Accessors

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public boolean isShared()
    {
        return isshared;
    }

    public void setShared(boolean tf)
    {
        this.isshared = tf;
    }

    public boolean isVariableLength()
    {
        return size == VARIABLELENGTH;
    }

    //////////////////////////////////////////////////
    // Clone Interface

    public Object clone()
    {
	    try {return super.clone(); } catch (CloneNotSupportedException e) {return null;}
    }


    //////////////////////////////////////////////////

    public String toString()
    {
        String sortname = (sort == null ? "" : sort.name());
        //String name = getFQN();
        String name = null;
        if(name == null) name = getShortName();
        if(name == null) name = "";
        return sortname + "::" + name + "[" +getSize()+ "]";
    }


} // class DapDimension

