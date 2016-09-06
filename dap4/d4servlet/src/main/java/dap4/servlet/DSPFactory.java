/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.core.data.DSP;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide a factory for DSP instances
 */

abstract public class DSPFactory
{

    //////////////////////////////////////////////////
    // Constructor(s)

    public DSPFactory()
    {
        // Subclasses should Register known DSP classes: order is important
        // in event that two or more dsps can match a given file
        // (e.q. FileDSP vs Nc4DSP).
        // Only used in server
    }



} // DSPFactory

