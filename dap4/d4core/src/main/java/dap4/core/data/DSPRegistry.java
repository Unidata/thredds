/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.data;

import dap4.core.util.DapContext;
import dap4.core.util.DapException;

import java.util.ArrayList;
import java.util.List;

/**
 */

public class DSPRegistry
{
    //////////////////////////////////////////////////
    // Constants

    // MNemonics
    static public final boolean LAST = true;
    static public final boolean FIRST = false;

    //////////////////////////////////////////////////
    // Type Decls

    static protected class Registration
    {
        Class<? extends DSP> dspclass;
        DSP matcher;

        public Registration(Class<? extends DSP> cl)
        {
            this.dspclass = cl;
            try {
                this.matcher = dspclass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IllegalArgumentException("DSPFactory: cannot create matching instance for Class: " + dspclass.getName());
            }
        }

        public String toString()
        {
            return dspclass.getName();
        }
    }

    //////////////////////////////////////////////////

    static protected ClassLoader loader = DSPRegistry.class.getClassLoader();

    //////////////////////////////////////////////////
    // Instance Variables

    /**
     * Define a map of known DSP classes.
     * Must be ordered to allow control over
     * test order
     */
    protected List<Registration> registry = new ArrayList<>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public DSPRegistry()
    {
    }


    //////////////////////////////////////////////////
    // Accessors

    static public void setLoader(ClassLoader ldr)
    {
        loader = ldr;
    }

    //////////////////////////////////////////////////
    // API

    /**
     * Register a DSP, using its class string name.
     *
     * @param className Name of class that implements DSP.
     * @throws IllegalAccessException if class is not accessible.
     * @throws InstantiationException if class doesnt have a no-arg constructor.
     * @throws ClassNotFoundException if class not found.
     */
    synchronized public void register(String className, boolean last)
            throws DapException
    {
        try {
            Class<? extends DSP> klass = (Class<? extends DSP>) loader.loadClass(className);
            register(klass, last);
        } catch (ClassNotFoundException e) {
            throw new DapException(e);
        }
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
    synchronized public void
    register(Class<? extends DSP> klass, boolean last)
    {
        // is this already defined?
        if(registered(klass)) return;
        if(last)
            registry.add(new Registration(klass));
        else
            registry.add(0, new Registration(klass));
    }

    /**
     * See if a specific DSP is registered
     *
     * @param klass Class for which to search
     */

    synchronized public boolean
    registered(Class<? extends DSP> klass)
    {
        for(Registration r : registry) {
            if(r.dspclass == klass) return true;
        }
        return false;
    }

    /**
     * Unregister dsp.
     *
     * @param klass Class for which to search
     */
    synchronized public void
    unregister(Class<? extends DSP> klass)
    {
        for(int i = 0; i < registry.size(); i++) {
            if(registry.get(i).dspclass == klass) {
                registry.remove(i);
                break;
            }
        }
    }

    /**
     * @param path
     * @return new DSP object that can process this path
     * @throws DapException
     */

    synchronized public DSP
    findMatchingDSP(String path, DapContext cxt)
            throws DapException
    {
        for(int i = 0; i < registry.size(); i++) {
            try {
                Registration tester = registry.get(i);
                boolean ismatch = (Boolean) tester.matcher.dspMatch(path, cxt);
                if(ismatch) {
                    DSP dsp = (DSP) tester.dspclass.newInstance();
                    return dsp;
                }
            } catch (Exception e) {
                throw new DapException(e);
            }
        }
        throw new IllegalArgumentException("Cannot open " + path);
    }

}

