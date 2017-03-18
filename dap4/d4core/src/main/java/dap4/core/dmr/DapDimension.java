/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

/**
 * This class defines a Dimension:
 * Modified 10/15/2016 to support zero-sized dimensions.
 */

public class DapDimension extends DapNode implements DapDecl, Cloneable
{

    //////////////////////////////////////////////////
    // Constants

    static final public long UNDEFINED = -2;

    //////////////////////////////////////////////////
    // Instance variables

    protected long size = UNDEFINED;

    protected boolean isshared = false;

    protected boolean isunlimited = false;

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
        this.isshared = (name != null);
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
        if(size == UNDEFINED)
            throw new IllegalStateException("Undefined dimension size");
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

    public boolean isUnlimited()
    {
        return this.isunlimited;
    }

    public void setUnlimited(boolean tf)
    {
        this.isunlimited = tf;
    }

    //////////////////////////////////////////////////
    // Clone Interface

    public Object clone()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }


    //////////////////////////////////////////////////

    public String toString()
    {
        String sortname = (sort == null ? "" : sort.name());
        String name = null;
        if(name == null) name = getShortName();
        if(name == null) name = "";
        return sortname + "::" + name + "[" + getSize() + "]";
    }


} // class DapDimension

