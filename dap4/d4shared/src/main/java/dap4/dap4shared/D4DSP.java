/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.DataDataset;
import dap4.core.dmr.DapDataset;
import dap4.core.util.DapDump;
import dap4.core.util.DapException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * DAP4 Serial to DSP interface
 * This code should be completely independent of thredds.
 * Its goal is to provide a DSP interface to
 * a sequence of bytes representing serialized data, possibly
 * including a leading DMR.
 */

abstract public class D4DSP extends AbstractDSP
{

    //////////////////////////////////////////////////
    // Constants

    static public boolean DEBUG = false;

    static protected final String DAPVERSION = "4.0";
    static protected final String DMRVERSION = "1.0";

    //////////////////////////////////////////////////
    // Instance variables

    protected D4DataDataset d4data = null; // root of the DataXXX tree
    protected ByteBuffer databuffer = null;
    protected ByteOrder order = null;
    protected ChecksumMode checksummode = ChecksumMode.DAP;

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4DSP()
    {
        super();
    }

    //////////////////////////////////////////////////
    // DSP API
    // Most is left to be subclass defined; 

    @Override
    public DataDataset
    getDataDataset()
    {
        return d4data;
    }

    //////////////////////////////////////////////////
    // (Other) Accessors

    public ByteBuffer getData()
    {
        return this.databuffer;
    }

    public void setDataDataset(D4DataDataset data)
    {
        this.d4data = data;
    }

    public ByteOrder getOrder()
    {
        return this.order;
    }

    public void setOrder(ByteOrder order)
    {
        this.order = order;
    }

    public ChecksumMode getChecksumMode()
    {
        return this.checksummode;
    }

    public void setChecksumMode(ChecksumMode mode)
    {
        this.checksummode = mode;
    }

    ////////////////////////////////////////////////
    // D4DSP specific API

    protected void
    build(String document, byte[] serialdata, ByteOrder order)
        throws DapException
    {
        build(parseDMR(document), serialdata, order);
    }

    protected void
    build(DapDataset dmr, byte[] serialdata, ByteOrder order)
        throws DapException
    {
        this.dmr = dmr;
        // "Compile" the databuffer section of the server response
        this.databuffer = ByteBuffer.wrap(serialdata).order(order);
        DataCompiler compiler = new DataCompiler(this, checksummode, this.databuffer);
        compiler.compile();
    }

}
