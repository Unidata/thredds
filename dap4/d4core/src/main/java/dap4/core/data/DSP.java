/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapDataset;
import dap4.core.dmr.DapVariable;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.Slice;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

public interface DSP
{
    /* All implementing classes must implement:
       1. a static dspMatch() function
       2. A parameterless constructor
     */

    /**
     * Determine if a path refers to an object processable by this DSP
     *
     * @param path
     * @param context
     * @return   true if this path can be processed by an instance of this DSP
     */
    public boolean dspMatch(String path, DapContext context);

    /**
     * @param path It is assumed that the path
     *             is appropriate to the dsp
     *             E.g. an absolute path or a url
     * @return DSP wrapping the path source
     * @throws dap4.core.util.DapException
     */
    public DSP open(String path) throws DapException;

    public void close() throws IOException;

    public String getLocation();

    public DSP setLocation(String location);

    public Object getContext();

    public void setContext(DapContext cxt);

    public DapDataset getDMR() throws dap4.core.util.DapException;

    public DataCursor getVariableData(DapVariable var) throws DapException;

    public ByteOrder getOrder();

    public ChecksumMode getChecksumMode();

}
