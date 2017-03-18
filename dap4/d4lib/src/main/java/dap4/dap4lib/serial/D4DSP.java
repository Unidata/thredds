/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib.serial;

import dap4.core.dmr.DapAttribute;
import dap4.core.dmr.DapDataset;
import dap4.core.util.DapDump;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.AbstractDSP;
import dap4.dap4lib.DMRPrinter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    static public boolean DUMPDMR = false;
    static public boolean DUMPDAP = false;

    static protected final String DAPVERSION = "4.0";
    static protected final String DMRVERSION = "1.0";

    //////////////////////////////////////////////////
    // Instance variables

    protected ByteBuffer databuffer = null; // local copy of AbstractDSP.getSource

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4DSP()
    {
        super();
    }

    //////////////////////////////////////////////////
    // DSP API
    // Most is left to be subclass defined; 

    //////////////////////////////////////////////////
    // (Other) Accessors

    /*packge*/ ByteBuffer
    getBuffer()
    {
        return databuffer;
    }

    //////////////////////////////////////////////////
    // Compilation

    protected void
    build(String document, byte[] serialdata, ByteOrder order)
            throws DapException
    {
        DapDataset dmr = parseDMR(document);

        if(DEBUG || DUMPDMR) {
            System.err.println("\n+++++++++++++++++++++");
            System.err.println(dmr);
            System.err.println("+++++++++++++++++++++\n");
        }
        if(DEBUG || DUMPDAP) {
            ByteBuffer data = ByteBuffer.wrap(serialdata);
            System.err.println("+++++++++++++++++++++");
            System.err.println("\n---------------------");
            DapDump.dumpbytes(data, false);
            System.err.println("\n---------------------\n");
        }
        build(dmr, serialdata, order);
    }

    /**
     * Build the data from the incoming serial data
     * Note that some DSP's will not use
     *
     * @param dmr
     * @param serialdata
     * @param order
     * @throws DapException
     */
    protected void
    build(DapDataset dmr, byte[] serialdata, ByteOrder order)
            throws DapException
    {
        setDMR(dmr);
        // "Compile" the databuffer section of the server response
        this.databuffer = ByteBuffer.wrap(serialdata).order(order);
        D4DataCompiler compiler = new D4DataCompiler(this, getChecksumMode(), getOrder(), this.databuffer);
        compiler.compile();
    }

}
