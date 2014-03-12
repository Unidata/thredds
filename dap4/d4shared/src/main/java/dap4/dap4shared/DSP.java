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
    /* All implementing classes must implement a parameterless constructor */
    public boolean match(String path, DapContext context);
    public DSP open(String path, DapContext context) throws DapException;
    public DSP open(String path) throws DapException;

    public String getPath();
    public Object getContext();
    public DapDataset getDMR() throws DapException;

    public DataDataset getDataDataset() throws IOException;

    public void close() throws IOException;

}
