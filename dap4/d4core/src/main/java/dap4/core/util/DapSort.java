/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

/**
 * Define an enumeration for all the DapNode subclasses to
 * avoid use of instanceof().  Note that this mixes
 * DAP2 and DAP4 for eventual joint support.
 * TODO: verify that this is faster than using instanceof, and if not,
 * go back to using instanceof.
 */

import dap4.core.dmr.*;

/**
 * Define the kinds of AST objects to avoid having to do instanceof.
 * The name field is for debugging.
 */
public enum DapSort
{
    ATOMICTYPE("AtomicType", DapType.class),
    ATTRIBUTESET("AttributeSet", DapAttributeSet.class),
    OTHERXML("OtherXML", DapOtherXML.class),
    ATTRIBUTE("Attribute", DapAttribute.class, ATTRIBUTESET, OTHERXML),
    DIMENSION("Dimension", DapDimension.class),
    MAP("Map", DapMap.class),
    ATOMICVARIABLE("Variable", DapVariable.class),
    DATASET("Dataset", DapDataset.class),
    GROUP("Group", DapGroup.class, DATASET),
    ENUMERATION("Enumeration", DapEnumeration.class),
    ENUMCONST("EnumConst", DapEnumConst.class),
    SEQUENCE("Sequence", DapSequence.class),
    STRUCTURE("Structure", DapStructure.class,SEQUENCE),;

    private final String name;
    private final Class classfor;
    private final DapSort[] subsorts;

    DapSort(String name, Class classfor, DapSort... subsorts)
    {
        this.name = name;
        this.classfor = classfor;
        this.subsorts = subsorts;
    }

    public final String getName()
    {
        return this.name;
    }

    public final Class getClassFor()
    {
        return this.classfor;
    }

    public boolean isa(DapSort supersort)
    {
        if(supersort == this)
            return true;
        for(DapSort sub : supersort.subsorts) {
            if(sub == this) return true;
        }
        return false;
    }

    public boolean oneof(DapSort... which)
    {
        for(DapSort sub : which) {
            if(sub == this) return true;
        }
        return false;
    }
};

