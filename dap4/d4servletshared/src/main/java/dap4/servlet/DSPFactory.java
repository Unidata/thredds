/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.dap4shared.DSP;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide a factory for DSP instances
 */

abstract public class DSPFactory
{

    //////////////////////////////////////////////////
    // Instance variables

    /**
     * Define a map of known DSP classes.
     */
    protected  List<Class> dspRegistry = new ArrayList<Class>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public DSPFactory()
    {
        // Register known DSP classes: order is important.
        // Only used in server
        registerDSP(SynDSP.class, true);
        registerDSP(CDMDSP.class, true);
    }

    //////////////////////////////////////////////////
    // Methods

    /**
     * Register a DSP, using its class string name.
     *
     * @param className Class that implements DSP.
     * @throws IllegalAccessException if class is not accessible.
     * @throws InstantiationException if class doesnt have a no-arg constructor.
     * @throws ClassNotFoundException if class not found.
     */
    public void registerDSP(String className)
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
    public void registerDSP(Class klass)
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
    synchronized public void registerDSP(Class klass, boolean last)
    {
        // is this already defined?
        int pos = dspRegistry.indexOf(klass);
        if(pos < 0) {
            if(last)
                dspRegistry.add(klass);  // put last
            else
                dspRegistry.add(0, klass);  // put first
        }
    }


    /**
     * See if a specific DSP is registered
     *
     * @param klass Class for which to search
     */

    synchronized public boolean dspRegistered(Class klass)
    {
        return dspRegistry.contains(klass);
    }

    /**
     * Unregister dsp.
     *
     * @param klass Class for which to search
     */
    synchronized public void dspUnregister(Class klass)
    {
        dspRegistry.remove(klass);
    }

    /**
     * @param path
     * @return DSP object that can process this path
     * @throws DapException
     */
    synchronized public DSP
    create(String path)
        throws DapException
    {
        for(int i = 0;i < dspRegistry.size();i++) {
            try {
                Class testclass = dspRegistry.get(i);
                Method match = testclass.getMethod("match", String.class, DapContext.class);
                boolean ismatch = (Boolean) match.invoke(null, path, (DapContext) null);
                if(ismatch) {
                    DSP dsp = (DSP) testclass.newInstance();
                    return dsp.open(path);
                }
            } catch (Exception e) {
                throw new DapException(e);
            }

        }
        throw new IllegalArgumentException("Cannot open "+path);
    }

} // DSPFactory

