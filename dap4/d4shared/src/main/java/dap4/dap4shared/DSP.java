/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataDataset;
import dap4.core.data.DataVariable;
import dap4.core.dmr.DapDataset;
import dap4.core.util.*;

import java.io.IOException;

public interface DSP
{
    /* Unfortunately, Java does not (yet, sigh!, as of java 7) allow
       including static methods in an interface.
       As with IOSPs, we need a quick match function to indicate
       that this DSP is likely to be able to process this file.
     */
    /* All implementing classes must implement:
       1. a static match() function
       2. A parameterless constructor
     */

    // static public boolean match(String path, DapContext context);

    public DSP open(String path, DapContext context) throws DapException;
    public DSP open(String path) throws DapException;

    public String getPath();
    public Object getContext();
    public DapDataset getDMR() throws DapException;

    public DataDataset getDataDataset() throws IOException;

    public void close() throws IOException;

}
