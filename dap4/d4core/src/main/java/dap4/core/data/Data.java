/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapNode;
import dap4.core.util.DapSort;

/**
 * The Data*.java classes
 * provide a new representation for
 * accessing data sources.
 * They are intended to be analogous to the ucar.ma2.Array
 * classes in CDM. The big differences are:
 * <ol>
 * <li> They closely parallel the ucar.ma2.Array system, but
 * with much cruft removed.
 * <li> They incorporate parts of the oc API model.
 * <li> They are divided into interfaces with supporting
 * implementations.
 * <li> They separate out the datatype conversions
 * from the core arrays. That is if you have an array of
 * integers, then if you want to view it as an array of
 * floats, you wrap the integer array with an appropriate
 * conversion array that does the int->float conversions.
 * <li> These are intended to eventually subsume DAP2.
 * </ol>
 * The possible kinds of arrays are defined by a subset of
 * the <i>DapSort</i> enum, namely, the following
 * <ul>
 * <li> ATOMICVARIABLE (DataAtomic)
 * <li> STRUCTURE (DataStructure)
 * <li> ARRAY (DataStructureArray)
 * <li> RECORD (DataRecord)
 * <li> SEQUENCE (DataSequence)
 * <li> DATASET (DataDataset)
 * </ul>
 * Notes:
 * <ul>
 * <li> For the compound types (Sequence,Structure),
 *      it is desirable to have both classes to
 *      represent both the collection and the instances.
 *      Thus for Sequences, we need a Singular sort (RECORD)
 *      and a set of Records (SEQUENCE). Similarly for
 *      structures: STRUCTURE(singular) STRUCTUREARRAY(set).
 *      Note that ATOMICVARIABLE does not have two sorts:
 *      it always represents an array of primitive typed objects.
 * <li> DATASET in this context is different
 *      from the DATASET when used elsewhere.
 *      Specifically, DATASET here refers to the aggregation
 *      of the data of all variables independent of any group scopes.
 * <li> The term data was used rather than array to avoid
 *      confusion when using ucar.ma2.Array classes.
 * </ul>
 */

public interface Data
{
    public DataSort getSort();
    public DapNode getTemplate();
}
