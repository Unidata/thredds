/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapVariable;
import dap4.core.util.DapSort;

import java.io.IOException;
import java.util.List;

/**
DataVariable is purely to allow
unified reference to various kinds of
variables:
-DataAtomic
-DataCompoundArray
-DataStructure
-DataSequence
*/

public interface DataVariable extends Data
{
}
