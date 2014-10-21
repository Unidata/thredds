/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.core.util.DapUtil;
import dap4.cdmshared.CDMUtil;
import dap4.cdmshared.NodeMap;
import dap4.dap4shared.*;
import ucar.ma2.*;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.WritableByteChannel;
import java.util.*;

public class DapNetcdfFile extends ucar.nc2.NetcdfFile
{
    static final boolean DEBUG = false;
    static final boolean PARSEDEBUG = false;
    static final boolean MERGE = false;

    // NetcdfDataset enhancement to use: need only coord systems
    //static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    //////////////////////////////////////////////////
    // Constants
    static final String QUERYSTART = "?";
    static final String FRAGSTART = "#";

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class NullCancelTask implements CancelTask
    {
        public boolean isCancel()
        {
            return false;
        }

        public void setError(String msg)
        {
        }

        public void setProgress(String msg, int progress)
        {
        }

    }

    //////////////////////////////////////////////////
    // Static variables

    static protected final NullCancelTask nullcancel = new NullCancelTask();

    //////////////////////////////////////////////////
    // Instance Variables

    protected boolean allowCompression = true;
    protected boolean closed = false;

    protected String originalurl = null;
    protected String finalurl = null;
    protected XURI xuri = null;
    protected D4DSP dsp = null;

    protected CancelTask cancel = null;

    protected NodeMap nodemap = null;

    /**
     * Originally, the array for a variable was stored
     * using var.setCacheData(). However, that is illegal
     * for Structures and Sequences, so (for now)
     * we maintain a map variable->array.
     */
    protected Map<Variable, Array> arraymap = new HashMap<>();

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * Open a Dap4 connection or file via a D4DSP.
     *
     * @param url        URL for the request.
     * @param cancelTask check if task is cancelled; may be null.
     * @throws IOException
     */
    public DapNetcdfFile(String url, CancelTask cancelTask)
            throws IOException
    {
        super();
        this.originalurl = url;
        // url may have leading dap4:
        XURI xuri;
        try {
            xuri = new XURI(url);
        } catch (URISyntaxException use) {
            throw new IOException(use);
        }
        List<String> protocols = xuri.getProtocols();
        url = xuri.assemble(XURI.URLALL);
        switch (protocols.size()) {
        case 0:
            if(xuri.isFile())
                url = "file://" + url;
            break;
        case 1:
            if(protocols.get(0).equalsIgnoreCase("dap4"))
                url = "http" + url.substring(4/*dap4*/, url.length());
            break;
        case 2:
            if(protocols.get(0).equalsIgnoreCase("dap4"))
                url = url.substring(5/*dap4:*/, url.length());
            break;
        default:
            break;

        }
        this.finalurl = url;
        cancel = (cancelTask == null ? nullcancel : cancelTask);
        // 1. Get and parse the constrained DMR and Data v-a-v URL
        if(xuri.isFile())
            this.dsp = (D4DSP) new FileDSP().open(url);
        else
            this.dsp = (D4DSP) new HttpDSP().open(url);

        // 2. Construct an equivalent CDM tree and populate 
        //    this NetcdfFile object.
        CDMCompiler compiler = new CDMCompiler(this, this.dsp);
        compiler.compile(arraymap);
        // set the pseudo-location, otherwise we get a name that is full path.
        setLocation(this.dsp.getDMR().getDataset().getShortName());
        finish();
    }

    /**
     * Open a Dap4 connection
     *
     * @param url URL for the request.
     * @throws IOException
     */

    public DapNetcdfFile(String url)
            throws IOException
    {
        this(url, null);
    }

    //////////////////////////////////////////////////
    // Close

    /**
     * Close all resources (files, sockets, etc) associated with this file.
     *
     * @throws java.io.IOException if error when closing
     */
    @Override
    public synchronized void close()
            throws java.io.IOException
    {
        if(closed) return;
        closed = true; // avoid circular calls
        dsp = null;
        nodemap = null;
    }

    //////////////////////////////////////////////////
    // Accessors

    /**
     * @return true if we can ask the server to do constraint processing
     */
    public boolean isconstrainable()
    {
        return true;
    }

    public String getURL()
    {
        return originalurl;
    }

    public DSP getDSP()
    {
        return this.dsp;
    }

    //////////////////////////////////////////////////
    // Override NetcdfFile.readXXX Methods

    /**
     * Do a bulk read on a list of Variables and
     * return a corresponding list of Array that contains the results
     * of a full read on each Variable.
     * TODO: optimize to make only a single server call and cache the results.
     *
     * @param variables List of type Variable
     * @return List of Array, one for each Variable in the input.
     * @throws IOException if read error
     */

    @Override
    public List<Array>
    readArrays(List<Variable> variables)
            throws IOException
    {
        List<Array> result = new ArrayList<Array>();
        for(Variable variable : variables) {
            result.add(variable.read());
        }
        return result;
    }

    /**
     * Read databuffer from a top level Variable
     * and send databuffer to a WritableByteChannel.
     * Experimental.
     *
     * @param v       a top-level Variable
     * @param section the section of databuffer to read.
     *                There must be a Range for each Dimension in the variable,
     *                in order.
     *                Note: no nulls allowed. IOSP may not modify.
     * @param channel write databuffer to this WritableByteChannel
     * @return the number of databuffer written to the channel
     * @throws java.io.IOException            if read error
     * @throws ucar.ma2.InvalidRangeException if invalid section
     */

    @Override
    public long readToByteChannel(Variable v, Section section, WritableByteChannel channel)
            throws java.io.IOException, ucar.ma2.InvalidRangeException
    {
        Array result = readData(v, section);
        return IospHelper.transferData(result, channel);
    }

    public Array
    readSection(String variableSection)
            throws IOException, InvalidRangeException
    {
        ParsedSectionSpec cer
                = ParsedSectionSpec.parseVariableSection(this, variableSection);
        return cer.v.read(cer.section);
    }

    /**
     * Primary read entry point.
     * This is the primary implementor of Variable.read.
     *
     * @param cdmvar  A top-level variable
     * @param section the section of databuffer to read.
     *                There must be a Range for each Dimension in the variable,
     *                in order. Note: no nulls allowed.
     * @return An Array object for accessing the databuffer
     * @throws IOException           if read error
     * @throws InvalidRangeException if invalid section
     */

    @Override
    protected Array
    readData(Variable cdmvar, Section section)
            throws IOException, InvalidRangeException
    {
        // The section is applied wrt to the DataDMR, so it
        // takes into account any constraint used in forming the dataDMR.
        // We use the Section to produce a view of the underlying variable array.

        assert this.dsp != null;
        Array result = arraymap.get(cdmvar);
        if(result == null)
            throw new IOException("No data for variable: " + cdmvar.getFullName());
        if(section != null) {
            if(cdmvar.getRank() != section.getRank())
                throw new InvalidRangeException(String.format("Section rank != %s rank", cdmvar.getFullName()));
            List<Range> ranges = section.getRanges();
            // Case out the possibilities
            if(CDMUtil.hasVLEN(ranges)) {
                ranges = ranges.subList(0, ranges.size() - 1);// may produce empty list
            }
            if(ranges.size() > 0 && !CDMUtil.isWhole(ranges, cdmvar))
                result = result.sectionNoReduce(ranges);
        }
        return result;
    }
}
