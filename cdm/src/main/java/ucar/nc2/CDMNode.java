/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Indent;

import java.util.HashMap;
import java.util.Map;

/**
 * Define a superclass for all the CDM node classes: Group, Dimension, etc.
 * Define the sort of the node {@link CDMSort} so that we can
 * 1. do true switching on node type
 * 2. avoid use of instanceof
 * 3. Use container classes that have more than one kind of node
 * <p>
 * Also move various common fields and methods to here.
 *
 * @author Heimbigner
 */

public abstract class CDMNode
{

    protected CDMSort sort = null;
    protected Group group = null;
    protected Structure parentstruct = null;
    protected boolean immutable = false;
    protected String shortName = null;

    protected Map<String, Object> annotations = new HashMap<>();

    //String fullName = null; // uses backslash escaping

    // HACK: sadly, because of the existing class structure,
    // we need to track the original name of a
    // Variable/Dimension/Attribute object.
    // This is primarily used in DAP processing
    // because the names in the DAP DDS and/or DAS
    // may contain group information separated
    // by forward slash.

    protected String dodsname = null;

    //////////////////////////////////////////////////
    // Constructors

    protected CDMNode()
    {
        // Use Instanceof to figure out the sort
        if(this instanceof Attribute)
            setSort(CDMSort.ATTRIBUTE);
        else if(this instanceof Dimension)
            setSort(CDMSort.DIMENSION);
        else if(this instanceof EnumTypedef)
            setSort(CDMSort.ENUMERATION);
        else if(this instanceof Sequence)
            setSort(CDMSort.SEQUENCE);
        else if(this instanceof Structure)
            setSort(CDMSort.STRUCTURE);
        else if(this instanceof Group)
            setSort(CDMSort.GROUP);
        else if(this instanceof Variable) // Only case left is atomic var
            setSort(CDMSort.VARIABLE);
    }

    public CDMNode(String name)
    {
        this();
        setShortName(name);
    }

    // Get/Set
    public CDMSort getSort()
    {
        return this.sort;
    }

    public void setSort(CDMSort sort)
    {
        if(!immutable) this.sort = sort;
    }

    /**
     * Get the short name of this Variable. The name is unique within its parent group.
     */
    public String getShortName()
    {
        return this.shortName;
    }

    /**
     * Set the short name of this Variable. The name is unique within its parent group.
     *
     * @param name new short name
     */
    public void setShortName(String name)
    {
        if(!immutable) this.shortName = NetcdfFile.makeValidCdmObjectName(name);
    }

    /**
     * Get its parent Group, or null if its the root group.
     *
     * @return parent Group
     */
    public Group getParentGroup()
    {
        return this.group;
    }

    /**
     * Alias for getParentGroup
     *
     * @return parent Group
     */
    public Group getGroup()
    {
        return getParentGroup();
    }

    /**
     * Set the parent Group
     *
     * @param parent The new parent group
     */
    public void setParentGroup(Group parent)
    {
        if(!immutable) this.group = parent;
    }

    /**
     * Get its parent structure, or null if not in structure
     *
     * @return parent structure
     */
    public Structure getParentStructure()
    {
        return this.parentstruct;
    }

    /**
     * Set the parent Structure
     *
     * @param parent The new parent structure
     */
    public void setParentStructure(Structure parent)
    {
        if(!immutable) this.parentstruct = parent;
    }

    /**
     * Test for presence of parent Structure
     *
     * @return true iff struct != null
     */

    public boolean isMemberOfStructure()
    {
        return this.parentstruct != null;
    }

    /**
     * Get immutable flag
     * As a rule, subclasses will access directly
     *
     * @return Immutable flag
     */
    public boolean getImmutable()
    {
        return this.immutable;
    }

    /**
     * Set the immutable flag
     *
     * @param tf The new value for the immutable flag
     */
    public void setImmutable(boolean tf)
    {
        this.immutable = tf;
    }


    /**
     * Get the dodsname
     *
     * @return the original names from the DDS or DAS; if null,
     * then return the short name
     */
    public String getDODSName()
    {
        if(dodsname == null)
            return this.shortName;
        else
            return this.dodsname;
    }

    /**
     * Store the original dods name
     *
     * @param name The original name from the DDS/DAS
     */
    public void setDODSName(String name)
    {
        this.dodsname = name;
    }


    /**
     * Get the Full name of this object. Certain characters are
     * backslash escaped (see NetcdfFile)
     *
     * @return full name with backslash escapes
     */
    public String getFullName()
    {
        return NetcdfFile.makeFullName(this);
    /* getting called before complete
    if (this.fullName == null)
      this.fullName = NetcdfFile.makeFullName(this);
    return this.fullName; */
    }

    /**
     * Alias for getFullName
     *
     * @return full name with backslash escapes
     */
    public String getFullNameEscaped()
    {
        return getFullName();
    }


    /**
     * getName is deprecated because, as the code below shows,
     * it has no consistent meaning. Sometimes it returns
     * the short name, sometimes it returns the full name.
     *
     * @deprecated Replaced by {@link #getShortName()} and {@link #getFullName()}
     */
    @Deprecated
    public String getName()
    {
        switch (sort) {
        case ATTRIBUTE:
        case DIMENSION:
        case ENUMERATION:
            // for these cases, getName is getShortName
            return getShortName();

        case VARIABLE: // Atomic
        case SEQUENCE:
        case STRUCTURE:
        case GROUP:
            // for these cases, getName is getFullName
            return getFullName();
        default:
            break;
        }
        return getShortName(); // default
    }

    // experimental
    public abstract void hashCodeShow(Indent indent);

    // Override the node's hashCode for subclasses of CDMNode.

    public int localhash()
    {
        return super.hashCode();
    }

    /**
     * NetcdfDataset can end up wrapping a variable
     * in multiple wrapping classes (e.g. VariableDS).
     * Goal of this procedure is to get down to the
     * lowest level Variable instance
     *
     * @param node possibly wrapped ode
     * @return the lowest level node instance
     */
    static public CDMNode
    unwrap(CDMNode node)
    {
        if(!(node instanceof Variable))
            return node;
        Variable inner = (Variable) node;
        for(; ; ) {
            if(inner instanceof VariableDS) {
                VariableDS vds = (VariableDS) inner;
                inner = vds.getOriginalVariable();
                if(inner == null) {
                    inner = vds;
                    break;
                }
            } else if(inner instanceof StructureDS) {
                StructureDS sds = (StructureDS) inner;
                inner = sds.getOriginalVariable();
                if(inner == null) {
                    inner = sds;
                    break;
                }
            } else break; // base case we have straight Variable or Stucture
        }
        return inner;
    }

    public Map<String, Object> getAnnotations()
    {
        return annotations;
    }

    public Object getAnnotation(String key)
    {
        return annotations.get(key);
    }

    public Object annotate(String key, Object value)
    {
        Object old = annotations.get(key);
        if(key != null && value != null)
            annotations.put(key,value);
        return old;
    }

}
