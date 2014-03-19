/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import   dap4.cdm.DapNetcdfFile;
import dap4.core.dmr.DapDataset;
import dap4.core.util.*;
import dap4.servlet.DMRPrint;
import dap4.dap4shared.D4DSP;
import ucar.nc2.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Dump DMR and/or data part of a DSP
 */

public class TestPrinter
{
    //////////////////////////////////////////////////
    // Constants

    // Could use enumset, but it is so ugly,
    // so use good old OR'able flags
    static final int NILFLAGS = 0;
    static final int PERLINE = 1; // print xml attributes 1 per line
    static final int NONAME = 2; // do not print name xml attribute
    static final int NONNIL = 4; // print empty xml attributes

    //////////////////////////////////////////////////
    // Instance Variables

    protected NetcdfFile ncfile = null; // dsp can be extracted from this
    protected D4DSP dsp = null;
    protected DapDataset dmr = null;
    protected PrintWriter writer = null;
    protected Map<Variable, ParsedSectionSpec> varmap = null;
    protected IndentWriter printer = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestPrinter(NetcdfFile ncfile, PrintWriter writer)
    {
        setDataset(ncfile);
        setWriter(writer);
        setVarMap(null);
    }

    public TestPrinter(D4DSP dsp, PrintWriter writer)
    {
        setDSP(dsp);
        setWriter(writer);
        setVarMap(null);
    }

    //////////////////////////////////////////////////
    // Accessors

    public void setWriter(PrintWriter writer)
    {
        this.writer = writer;
        this.printer = new IndentWriter(writer);
    }

    public void setDataset(NetcdfFile ncfile)
    {
        this.ncfile = ncfile;
        setDSP(((DapNetcdfFile) this.ncfile).getDSP());
    }

    public void setDSP(D4DSP dsp)
    {
        this.dsp = dsp;
        this.dmr = dsp.getDMR();
    }

    public void setVarMap(Map<Variable, ParsedSectionSpec> map)
    {
        this.varmap = map;
    }

    //////////////////////////////////////////////////
    // Print methods

    public void flush()
        throws IOException
    {
        printer.flush();
        writer.flush();
    }

    /**
     * Print the CDM metadata for a NetcdfFile object in DMR format
     *
     * @throws IOException
     */

    public void
    print()
        throws IOException
    {
        printer.setIndent(0);
        DMRPrint dmrprinter = new DMRPrint(writer);
        dmrprinter.printDMR(this.dmr);
        dmrprinter.flush();
    }

} // class TestPrinter
