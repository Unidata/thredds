/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataDataset;
import dap4.core.data.DataException;
import dap4.core.dmr.DapAttribute;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.DapFactoryDMR;
import dap4.core.dmr.DapNode;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import org.xml.sax.SAXException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    protected DapContext context = null;
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
        return open(path, new DapContext());
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

    protected void setContext(DapContext context)
    {
        this.context = context;
    }

    protected void
    setRequestResponse()
    {
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
        DapDataset result = pushparser.getDMR();
        processAttributes(result);
        return result;
    }

    /**
     * Walk the dataset tree and remove selected attributes
     * such as _Unsigned
     *
     * @param dataset
     */
    protected void
    processAttributes(DapDataset dataset)
            throws DapException
    {
        List<DapNode> nodes = dataset.getNodeList();
        for(DapNode node : nodes) {
            switch (node.getSort()) {
            case GRID:
            case SEQUENCE:
            case STRUCTURE:
            case GROUP:
            case DATASET:
            case ATOMICVARIABLE:
                Map<String, DapAttribute> attrs = node.getAttributes();
                if(attrs.size() > 0) {
                    List<DapAttribute> suppressed = new ArrayList<>();
                    for(DapAttribute dattr : attrs.values()) {
                        if(suppress(dattr.getShortName()))
                            suppressed.add(dattr);
                    }
                    for(DapAttribute dattr : suppressed) {
                        node.removeAttribute(dattr);
                    }
                }
                break;
            default:
                break; /*ignore*/
            }
        }
    }

    /**
     * Some attributes that are added by the NetcdfDataset
     * need to be kept out of the DMR. This function
     * defines that set.
     *
     * @param attrname A non-escaped attribute name to be tested for suppression
     * @return true if the attribute should be suppressed, false otherwise.
     */
    protected boolean suppress(String attrname)
    {
        if(attrname.startsWith("_Coord")) return true;
        if(attrname.equals("_Unsigned"))
            return true;
        return false;
    }
}
