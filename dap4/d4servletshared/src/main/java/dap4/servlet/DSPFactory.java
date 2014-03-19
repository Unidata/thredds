/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.core.util.DapException;
import dap4.dap4shared.DSP;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide a factory for DSP instances
 */

abstract public class DSPFactory
{

    //////////////////////////////////////////////////
    // Class variables

    /**
     * Define a map of known DSP classes.
     */
    static List<Class> dspRegistry = new ArrayList<Class>();

    //////////////////////////////////////////////////
    // Static initialization

    static {
        // Register known DSP classes
        try {
            registerDSP(CDMDSP.class);
        } catch (DapException de) {/*ignore*/}
    }

    //////////////////////////////////////////////////
    // Class methods

    /**
     * Register a DSP, using its class string name.
     *
     * @param className Class that implements DSP.
     * @throws IllegalAccessException if class is not accessible.
     * @throws InstantiationException if class doesnt have a no-arg constructor.
     * @throws ClassNotFoundException if class not found.
     */
    static public void registerDSP(String className)
        throws DapException
    {
        try {
            Class klass = DSPFactory.class.getClassLoader().loadClass(className);
            registerDSP(klass);
        } catch (ClassNotFoundException e) {
            throw new DapException(e);
        }
    }

    /**
     * Register a DSP class.
     *
     * @param klass Class that implements DSP.
     * @throws IllegalAccessException if class is not accessible.
     * @throws InstantiationException if class doesnt have a no-arg constructor.
     * @throws ClassCastException     if class doesnt implement DSP interface.
     */
    static public void registerDSP(Class klass)
        throws DapException
    {
        registerDSP(klass, false);
    }

    /**
     * Register a DSP class.
     *
     * @param klass Class that implements DSP.
     * @param last  true=>insert at the end of the list; otherwise front
     * @throws IllegalAccessException if class is not accessible.
     * @throws InstantiationException if class doesnt have a no-arg constructor.
     * @throws ClassCastException     if class doesnt implement DSP interface.
     */
    static synchronized public void registerDSP(Class klass, boolean last)
        throws DapException
    {
        try {
            // is this already defined?
            int pos = dspRegistry.indexOf(klass);
            DSP dsp = (DSP) klass.newInstance(); // fail fast
            if(pos >= 0)
                dspRegistry.set(pos, klass);  // replace existing
            else if(!last)
                dspRegistry.add(0, klass);  // put first
            else
                dspRegistry.add(klass);  // put last
        } catch (IllegalAccessException e1) {
            throw new DapException(e1);
        } catch (InstantiationException e2) {
            throw new DapException(e2);
        }
    }

    /**
     * See if a specific DSP is registered
     *
     * @param klass Class for which to search
     */
    static synchronized public boolean dspRegistered(Class klass)
    {
        return dspRegistry.contains(klass);
    }

    /**
     * Unregister dsp.
     *
     * @param klass Class for which to search
     */
    static synchronized public void dspUnregister(Class klass)
    {
        dspRegistry.remove(klass);
    }

    /**
     *
     * @param path
     * @return DSP object that can process this path
     * @throws DapException
     */
    static synchronized public DSP
    create(String path)
        throws DapException
    {
        for(int i = 0;i < dspRegistry.size();i++) {
            Class testclass = dspRegistry.get(i);
            DSP dsp;
            try {
                dsp = (DSP)testclass.newInstance();
            } catch (Exception e) {
                throw new DapException(e);
            }
            if(dsp.match(path,null))
                return dsp.open(path);
        }
        return null;
    }

} // DSPFactory

