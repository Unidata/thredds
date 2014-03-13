/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.dap4shared.DSP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Provide an LRU cache of DSPs.
 * The cache key is assumed to be the DSP object.
 * The cache is synchronized to avoid race conditions.
 * Note that we do not have to release because Java
 * uses garbage collection and entries will be purged
 * if the LRU cache is full.
 */

abstract public class DapCache
{

    //////////////////////////////////////////////////
    // Constants

    static final int MAXFILES = 100; // size of the cache

    //////////////////////////////////////////////////
    // Instance variables

    /**
     * Define an lru cache of known DSP objects: oldest first.
     */
    static protected List<DSP> lru = new ArrayList<DSP>();

    //////////////////////////////////////////////////
    // API

    static public synchronized DSP
    open(String path)
        throws IOException
    {
        int lrusize = lru.size();
        for(int i = lrusize - 1;i >= 0;i--) {
            DSP dsp = lru.get(i);
            if(dsp.getPath() == path) {
                // move to the front of the queue to maintain LRU property
                lru.remove(i);
                lru.add(dsp);
                return dsp;
            }
        }
        // No match found, create and initialize it.
        // If cache is full, remove oldest entry
        if(lrusize == MAXFILES) {
            // make room
            lru.remove(0);
        }
        // Find dsp that can process this path
        DSP dsp = DSPFactory.create(path);
        lru.add(dsp);
        return dsp;
    }


} // DapCache
