/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.data.DSP;
import dap4.core.util.DapContext;
import dap4.core.util.DapDump;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.ChunkInputStream;
import dap4.dap4lib.DapCodes;
import dap4.dap4lib.RequestMode;
import dap4.dap4lib.XURI;
import dap4.dap4lib.serial.D4DSP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Provide a DSP interface to synthetic data (see Generator.java).
 */

public class SynDSP extends D4DSP
{
    static final protected boolean DEBUG = false;

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
    public boolean dspMatch(String path, DapContext context)
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
    public SynDSP
    open(String filepath)
            throws DapException
    {
        // convert the relative path to real path
        assert this.context != null;
        if(filepath.startsWith("file:")) try {
            XURI xuri = new XURI(filepath);
            filepath = xuri.getPath();
        } catch (URISyntaxException use) {
            throw new DapException("Malformed filepath: " + filepath)
                    .setCode(DapCodes.SC_NOT_FOUND);
        }
        setLocation(filepath);
        // Read the .dmr/.syn file
        String document;
        try {
            try (FileInputStream stream = new FileInputStream(filepath);) {
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
            Generator generator = new Generator(dmr, Value.ValueSource.RANDOM);
            generator.generate(null, cw, true, getChecksumMode());
            cw.close();
            bos.close();
            byte[] raw = bos.toByteArray();
            if(DEBUG)
                DapDump.dumpbytes(ByteBuffer.wrap(raw).order(getOrder()), true);
            ByteArrayInputStream bis = new ByteArrayInputStream(raw);
            ChunkInputStream crdr = new ChunkInputStream(bis, RequestMode.DAP, getOrder());
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
