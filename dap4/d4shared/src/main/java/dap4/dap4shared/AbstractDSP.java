/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataDataset;
import dap4.core.data.DataException;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.DapFactoryDMR;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import org.xml.sax.SAXException;

import java.net.URISyntaxException;

/**
 * Provide a superclass for DSPs.
 */

abstract public class AbstractDSP implements DSP
{
    //////////////////////////////////////////////////
    // constants

    static protected final boolean PARSEDEBUG = false;

    //////////////////////////////////////////////////
    // Instance variables

    protected Object context = null;
    protected DapDataset dmr = null;
    protected String path = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public AbstractDSP()  /* must have a parameterless constructor */
    {
    }

    //////////////////////////////////////////////////
    // DSP Interface

    // Subclass defined

    abstract public DSP open(String path, DapContext context) throws DapException;

    abstract public DataDataset getDataDataset();

    //abstract public String getPath();

    @Override
    public DSP open(String path)
        throws DapException
    {
        return open(path, null);
    }

    @Override
    public Object getContext()
    {
        return this.context;
    }

    public String
    getPath()
    {
        return path;
    }

    @Override
    public DapDataset getDMR()
    {
        return this.dmr;
    }

    // DSP Extensions

    protected void setContext(Object context)
    {
        this.context = context;
    }

    protected void
    setDataset(DapDataset dataset)
        throws DapException
    {
        this.dmr = dataset;
    }

    public void
    setPath(String path)
        throws DapException
    {
        this.path = path;
    }

    //////////////////////////////////////////////////
    // Utilities

    /**
     * It is common to want to parse a DMR text to a DapDataset,
     * so provide this utility.
     *
     * @param document the dmr to parse
     * @return the parsed dmr
     * @throws DapException on parse errors
     */

    protected DapDataset
    parseDMR(String document)
        throws DapException
    {
        // Parse the dmr
        Dap4Parser pushparser = new Dap4Parser(new DapFactoryDMR());
        if(PARSEDEBUG)
            pushparser.setDebugLevel(1);
        try {
            if(!pushparser.parse(document))
                throw new DapException("DMR Parse failed");
        } catch (SAXException se) {
            throw new DapException(se);
        }
        if(pushparser.getErrorResponse() != null)
            throw new DapException("Error Response Document not supported");
        return pushparser.getDMR();
    }
}
