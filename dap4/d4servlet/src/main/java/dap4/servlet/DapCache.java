/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.core.ce.CEConstraint;
import dap4.core.data.DSP;
import dap4.core.data.DSPRegistry;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.dap4lib.DapCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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

    static public final String MATCHMETHOD = "dspMatch";

    //////////////////////////////////////////////////
    // Static variables

    /**
     * Define a map of known DSP classes.
     */
    static public DSPRegistry dspregistry = new DSPRegistry();

    /**
     * Define an lru cache of known DSP objects: oldest first.
     */
    static protected List<DSP> lru = new ArrayList<DSP>();

    // This should be set by any subclass
    static protected DSPFactory factory = null;

    static public void setFactory(DSPFactory f)
    {
        factory = f;
    }

    static public DSPFactory getFactory()
    {
        return factory;
    }

    static public synchronized DSP open(String path, DapContext cxt)
            throws IOException
    {
        assert cxt != null;
        int lrusize = lru.size();
        for(int i = lrusize - 1; i >= 0; i--) {
            DSP dsp = lru.get(i);
            String dsppath = dsp.getLocation();
            if(dsppath.equals(path)) {
                // move to the front of the queue to maintain LRU property
                lru.remove(i);
                lru.add(dsp);
                CEConstraint.release(lru.get(0).getDMR());
                return dsp;
            }
        }
        // No match found, create and initialize it.
        // If cache is full, remove oldest entry
        if(lrusize == MAXFILES) {
            // make room
            lru.remove(0);
            CEConstraint.release(lru.get(0).getDMR());
        }
        // Find dsp that can process this path
        DSP dsp = dspregistry.findMatchingDSP(path,cxt);
        if(dsp == null)
            throw new DapException("Resource has no matching DSP: " + path)
                    .setCode(DapCodes.SC_FORBIDDEN);
        dsp.setContext(cxt);
        dsp.open(path);
        lru.add(dsp);
        return dsp;
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


} // DapCache
