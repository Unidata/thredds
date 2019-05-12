/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.ui.prefs.Debug;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Dynamic proxy for Debug
 */
class DebugProxyHandler implements InvocationHandler {

/** */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("toString")) {
            return super.toString();
        }

        // System.out.println("proxy= "+proxy+" method = "+method+" args="+args);
        if (method.getName().equals("isSet")) {
            return Debug.isSet((String) args[0]);
        }
        if (method.getName().equals("set")) {
            Debug.set((String) args[0], (Boolean) args[1]);
            return null;
        }
        return Boolean.FALSE;
    }
}
