/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

/**
 Define an enumeration for all the Data subclasses
 */

/**
 * Define the kinds of Data objects to avoid having to do instanceof.
 * The name field is for debugging.
 */
public enum DataSort
{
    DATASET("Dataset"),
    ATOMIC("Atomic"),
    STRUCTURE("Structure"), // DataStructure
    SEQUENCE("Sequence"),   // DataSequence (== set of records)
    RECORD("Record"),       // Sequence equivalent of a Structure instance
    COMPOUNDARRAY("CompoundArray"); // Dimensioned set of Structure or Sequence instances

    private final String name;

    DataSort(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

};

