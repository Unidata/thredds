/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataDataset;
import dap4.core.data.DataException;
import dap4.core.dmr.DapDataset;
import dap4.core.util.*;

import java.net.URISyntaxException;

/**
 * Provide a Dap equivalent of an IOSP.
 */

abstract public class AbstractDSP implements DSP
{

    //////////////////////////////////////////////////
    // Instance variables

    protected Object context = null;

    protected String path = null;

    protected XURI xuri = null;

    protected DapDataset dmr = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public AbstractDSP()  /* must have a parameterless constructor */
    {
    }

    public AbstractDSP(String path, Object context)
        throws DapException
    {
        this();
        setPath(path);
        setContext(context);
    }

    //////////////////////////////////////////////////
    // DSP Interface

    // Subclass defined

    abstract public boolean match(String path, DapContext context);
    abstract public DSP open(String path, DapContext context) throws DapException;
    abstract public DSP open(String path) throws DapException;
    abstract public DataDataset getDataDataset();

    // Overrideable

    @Override public String getPath()
    {
        return this.path;
    }

    @Override public Object getContext()
    {
        return this.context;
    }

    @Override public DapDataset getDMR()
    {
        return this.dmr;
    }

    // DSP Extensions

    protected void setDataset(DapDataset dataset)
        throws DapException
    {
        this.dmr = dataset;
        this.dmr.finish();
    }

    protected void setContext(Object context)
    {
        this.context = context;
    }

    protected void setPath(String path)
        throws DapException
    {
        this.path = path;
        try {
            this.xuri = new XURI(path);
        } catch (URISyntaxException use) {
            throw new DataException(use);
        }
    }
}
