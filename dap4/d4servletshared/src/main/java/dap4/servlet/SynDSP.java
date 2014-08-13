/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.util.*;
import dap4.dap4shared.*;
import sun.nio.cs.Surrogate;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Provide a DSP interface to synthetic data (see Generator.java).
 */

public class SynDSP extends D4DSP
{
    static protected final String[] SYNEXTENSIONS = new String[]{
        ".dmr", ".syn"
    };

    //////////////////////////////////////////////////
    // Instance variables

    protected byte[] raw = null; // Complete serialized binary databuffer

    //////////////////////////////////////////////////
    // Constructor(s)

    public SynDSP()
    {
        super();
        setOrder(ByteOrder.nativeOrder());
    }

    //////////////////////////////////////////////////
    // DSP API


    /**
     * A path is a Synthetic path if it ends in .dmr or .syn
     *
     * @param path
     * @param context Any parameters that may help to decide.
     * @return true if this path appears to be processible by this DSP
     */
    static public boolean match(String path, DapContext context)
    {
        for(String ext : SYNEXTENSIONS) {
            if(path.endsWith(ext))
                return true;
        }
        return false;
    }

    @Override
    public void close()
    {
    }

    @Override
    public DSP
    open(String path, DapContext context)
        throws DapException
    {
        setPath(path);
        // Read the .dmr/.syn file
        String document;
        try {
            try (FileInputStream stream = new FileInputStream(path);) {
                document = DapUtil.readtextfile(stream);
            }
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
        // Parse the dmr.
        this.dmr = parseDMR(document);

        // Use the Generator to generate synthetic data for this dmr.
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ChunkWriter cw = new ChunkWriter(bos, RequestMode.DAP, ByteOrder.nativeOrder());
            Generator generator = new Generator(dmr,Value.ValueSource.RANDOM);
            generator.generate(null, cw);
            cw.close();
            bos.close();
            byte[] raw = bos.toByteArray();
            if(DEBUG)
                DapDump.dumpbytes(ByteBuffer.wrap(raw).order(order), true);
            ByteArrayInputStream bis = new ByteArrayInputStream(raw);
            ChunkInputStream crdr = new ChunkInputStream(bis,RequestMode.DAP, getOrder());
            // Skip the dmr
            crdr.readDMR();
            this.raw = DapUtil.readbinaryfile(crdr);
            super.build(dmr, this.raw, getOrder());
            return this;
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
    }

}
