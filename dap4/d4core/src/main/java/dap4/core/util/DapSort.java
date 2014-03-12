/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

/**
 Define an enumeration for all the DapNode subclasses to
 avoid use of instanceof().  Note that this mixes
 DAP2 and DAP4 for eventual joint support.
 */

/**
 * Define the kinds of AST objects to avoid having to do instanceof.
 * The name field is for debugging.
 */
public enum DapSort
{
    ATTRIBUTE("Attribute"),
    ATTRIBUTESET("AttributeSet"),
    OTHERXML("OtherXML"),
    XML("XML"),
    DIMENSION("Dimension"),
    MAP("Map"),
    ENUMERATION("Enumeration"),
    ATOMICVARIABLE("AtomicVariable"),
    GRID("Grid"),
    SEQUENCE("Sequence"),
    STRUCTURE("Structure"),
    GROUP("Group"),
    DATASET("Dataset"),
    TYPE("Type"); // for DapType

    private final String name;

    DapSort(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

};

