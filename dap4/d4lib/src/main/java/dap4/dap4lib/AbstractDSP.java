/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import dap4.core.data.ChecksumMode;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DapAttribute;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.DapNode;
import dap4.core.dmr.DapVariable;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.dmr.parser.Dap4ParserImpl;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provide a superclass for DSPs.
 */

abstract public class AbstractDSP implements DSP
{
    static public boolean TESTING = false; /* Turned on by test programs */

    //////////////////////////////////////////////////
    // constants

    static protected final boolean DEBUG = false;
    static protected final boolean PARSEDEBUG = false;

    static public final boolean USEDOM = false;

    static protected final String DAPVERSION = "4.0";
    static protected final String DMRVERSION = "1.0";
    static protected final String DMRNS = "http://xml.opendap.org/ns/DAP/4.0#";

    // Define reserved attributes
    static public final String UCARTAGVLEN = "_edu.ucar.isvlen";
    static public final String UCARTAGOPAQUE = "_edu.ucar.opaque.size";
    static public final String UCARTAGORIGTYPE = "_edu.ucar.orig.type";
    static public final String UCARTAGUNLIMITED = "_edu.ucar.isunlimited";


    protected DapContext context = null;
    protected DapDataset dmr = null;
    protected String location = null;
    private ByteOrder order = null;
    private ChecksumMode checksummode = ChecksumMode.DAP;

    protected Map<DapVariable, DataCursor> variables = new HashMap<>();
    protected DataCursor rootcursor = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public AbstractDSP()  /* must have a parameterless constructor */
    {
    }

    //////////////////////////////////////////////////
    // DSP Interface

    // Subclass defined

    /**
     * "open" a reference to a data source and return the DSP wrapper.
     *
     * @param location - Object that defines the data source
     * @return = wrapping dsp
     * @throws DapException
     */
    @Override
    abstract public AbstractDSP open(String location) throws DapException;

    /**
     * @throws IOException
     */
    abstract public void close() throws IOException;

    //////////////////////////////////////////////////
    // Implemented

    @Override
    public DataCursor
    getVariableData(DapVariable var) throws DapException
    {
        return this.variables.get(var);
    }

    @Override
    public DapContext getContext()
    {
        return this.context;
    }

    @Override
    public String
    getLocation()
    {
        return this.location;
    }

    @Override
    public AbstractDSP
    setLocation(String loc)
    {
        this.location = loc;
        return this;
    }

    @Override
    public DapDataset getDMR()
    {
        return this.dmr;
    }

    // DSP Extensions

    @Override
    public void setContext(DapContext context)
    {
        this.context = context;
        // Extract some things from the context
        Object o = this.context.get(Dap4Util.DAP4ENDIANTAG);
        if(o != null)
            setOrder((ByteOrder) o);
        o = this.context.get(Dap4Util.DAP4CSUMTAG);
        if(o != null)
            setChecksumMode(ChecksumMode.modeFor(o.toString()));
    }

    public void
    setDMR(DapDataset dmr)
    {
        this.dmr = dmr;
        if(getDMR() != null) {
            // Add some canonical attributes to the  <Dataset>
            getDMR().setDataset(getDMR());
            getDMR().setDapVersion(DAPVERSION);
            getDMR().setDMRVersion(DMRVERSION);
            getDMR().setNS(DMRNS);
        }
    }

    protected void
    setDataset(DapDataset dataset)
            throws DapException
    {
        this.dmr = dataset;
    }

    public ByteOrder
    getOrder()
    {
        return this.order;
    }

    public AbstractDSP
    setOrder(ByteOrder order)
    {
        this.order = order;
        return this;
    }

    public ChecksumMode
    getChecksumMode()
    {
        return this.checksummode;
    }

    public AbstractDSP
    setChecksumMode(ChecksumMode mode)
    {
        if(mode != null)
            this.checksummode = mode;
        return this;
    }

    public void
    addVariableData(DapVariable var, DataCursor cursor)
    {
        this.variables.put(var, cursor);
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
        Dap4Parser parser;
        //if(USEDOM)
        parser = new Dap4ParserImpl(null);
        //else
        //    parser = new DOM4Parser(new DefaultDMRFactory());
        if(PARSEDEBUG)
            parser.setDebugLevel(1);
        try {
            if(!parser.parse(document))
                throw new DapException("DMR Parse failed");
        } catch (SAXException se) {
            throw new DapException(se);
        }
        if(parser.getErrorResponse() != null)
            throw new DapException("Error Response Document not supported");
        DapDataset result = parser.getDMR();
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
            case GROUP:
            case DATASET:
            case VARIABLE:
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
        // Try to extract the byte order
        getEndianAttribute(dataset);
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

    void
    getEndianAttribute(DapDataset dataset)
    {
        DapAttribute a = dataset.findAttribute(DapUtil.LITTLEENDIANATTRNAME);
        if(a == null)
            this.order = (ByteOrder.LITTLE_ENDIAN);
        else {
            Object v = a.getValues();
            int len = java.lang.reflect.Array.getLength(v);
            if(len == 0)
                this.order = (ByteOrder.nativeOrder());
            else {
                String onezero = java.lang.reflect.Array.get(v,0).toString();
                int islittle = 1;
                try {
                    islittle = Integer.parseInt(onezero);
                } catch (NumberFormatException e) {
                    islittle = 1;
                }
                if(islittle == 0)
                    this.order = (ByteOrder.BIG_ENDIAN);
                else
                    this.order = (ByteOrder.LITTLE_ENDIAN);
            }
        }
    }

    static public String
    printDMR(DapDataset dmr)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DMRPrinter printer = new DMRPrinter(dmr, pw);
        try {
            printer.print();
            pw.close();
            sw.close();
        } catch (IOException e) {
        }
        return sw.toString();
    }

}
