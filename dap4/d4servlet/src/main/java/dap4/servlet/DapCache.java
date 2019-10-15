/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.cdm.dsp.CDMDSP;
import dap4.core.ce.CEConstraint;
import dap4.core.data.DSP;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.HttpDSP;
import dap4.dap4lib.netcdf.Nc4DSP;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Provide an LRU cache of DSPs.
 * It is expected (for now) that this is only used on the server side.
 * The cache key is assumed to be the DSP object.
 * The cache is synchronized to avoid race conditions.
 * Note that we do not have to release because Java
 * uses garbage collection and entries will be purged
 * if the LRU cache is full.
 * Singleton class
 */

abstract public class DapCache
{

    //////////////////////////////////////////////////
    // Constants

    static final int MAXFILES = 100; // size of the cache

    //////////////////////////////////////////////////
    // Types

    //////////////////////////////////////////////////
    // Static variables

    /**
     * Define an lru cache of known DSP objects: oldest first.
     */
    static protected List<DSP> lru = new ArrayList<DSP>();

    /**************************************************/
    /* Check cache */
    static protected DSP locateDSP(String location)
            throws IOException
    {
        int lrusize = lru.size();
        for(int i = lrusize - 1; i >= 0; i--) {
            DSP dsp = lru.get(i);
            String dsppath = dsp.getLocation();
            if(dsppath.equals(location)) {
                // move to the front of the queue to maintain LRU property
                lru.remove(i);
                lru.add(dsp);
                CEConstraint.release(lru.get(0).getDMR());
                return dsp;
            }
        }
        return null; /* no match found */
    }

    static void addDSP(DSP dsp)
            throws DapException
    {
        // If cache is full, remove oldest entry
        if(lru.size() == MAXFILES) {
            // make room
            lru.remove(0);
            CEConstraint.release(lru.get(0).getDMR());
        }
        lru.add(dsp);
    }

    static synchronized public void flush() // for testing
            throws Exception
    {
        while(lru.size() > 0) {
            DSP dsp = lru.get(0);
            CEConstraint.release(dsp.getDMR());
            dsp.close();
            lru.remove(0);
        }
    }

    /**************************************************/
    // DapDSP pass-thrus

    static public synchronized DSP open(DapRequest drq, NetcdfFile ncfile, DapContext cxt)
            throws IOException
    {
	return DapDSP.open(drq,ncfile,cxt);
    }

    static public synchronized DSP open(DapRequest drq, String target, DapContext cxt)
            throws IOException
    {
	return DapDSP.open(drq,target,cxt);
    }


} // DapCache
