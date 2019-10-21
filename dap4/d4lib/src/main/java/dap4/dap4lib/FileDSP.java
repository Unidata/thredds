/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.serial.D4DSP;
import ucar.nc2.NetcdfFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Provide a DSP interface to raw data
 */

public class FileDSP extends D4DSP
{
    //////////////////////////////////////////////////
    // Constants

    static protected final String[] EXTENSIONS = new String[]{
            ".dap", ".raw"
    };

    //////////////////////////////////////////////////
    // Instance variables

    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    protected byte[] raw = null; // Complete serialized binary databuffer

    protected File file = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public FileDSP()
    {
        super();
    }

    //////////////////////////////////////////////////
    // DSP API

    public String getLocation() {return this.file.getAbsolutePath();}

    /**
     * A path is file if it has no base protocol or is file:
     *
     * @param location file:/ or possibly an absolute path
     * @param context  Any parameters that may help to decide.
     * @return true if this path appears to be processible by this DSP
     */
    public boolean dspMatch(String location, DapContext context)
    {
        try {
            XURI xuri = new XURI(location);
            if(xuri.isFile()) {
                String path = xuri.getPath();
                for(String ext : EXTENSIONS) {
                    if(path.endsWith(ext))
                        return true;
                }
            }
        } catch (URISyntaxException use) {
            return false;
        }
        return false;
    }

    @Override
    public void close()
    {
    }

    @Override
    public FileDSP
    open(File file)
            throws DapException
    {
        this.file = file;
        try {
            try (FileInputStream stream = new FileInputStream(file)) {
                this.raw = DapUtil.readbinaryfile(stream);
            }
            try (FileInputStream stream = new FileInputStream(file)) { // == rewind
                ChunkInputStream rdr = new ChunkInputStream(stream, RequestMode.DAP);
                String document = rdr.readDMR();
                byte[] serialdata = DapUtil.readbinaryfile(rdr);
                super.build(document, serialdata, rdr.getRemoteByteOrder());
            }
            return this;
        } catch (IOException ioe) {
            throw new DapException(ioe).setCode(DapCodes.SC_INTERNAL_SERVER_ERROR);
        }
    }

    //////////////////////////////////////////////////
    // Extension to access a raw byte stream

    public FileDSP
    open(byte[] rawdata)
            throws DapException
    {
        try {
            this.raw = rawdata;
            ByteArrayInputStream stream = new ByteArrayInputStream(this.raw);
            ChunkInputStream rdr = new ChunkInputStream(stream, RequestMode.DAP);
            String document = rdr.readDMR();
            byte[] serialdata = DapUtil.readbinaryfile(rdr);
            super.build(document, serialdata, rdr.getRemoteByteOrder());
            return this;
        } catch (IOException ioe) {
            throw new DapException(ioe).setCode(DapCodes.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
