/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap.Server;

import java.util.*;

import opendap.dap.NoSuchFunctionException;


/**
 * Represents a library of available server-side functions for use
 * in evaluating constraint expressions. <p>
 * <p/>
 * When created, a FunctionLibrary is empty.
 * There are two ways to populate it, described below. Once the
 * FunctionLibrary has been created and populated, it should be
 * used to create a ClauseFactory, which can then be given to
 * the CEEvaluator for use in parsing.<p>
 * <p/>
 * <p> The most straightforward is to pass an instance of each
 * ServerSideFunction class to the add() method.  If you have a large
 * number of ServerSideFunctions, or if you want to have choose class names
 * for your functions independent of the actual function name, this is the
 * approach to use.
 * <p/>
 * A second, more complex method resolves
 * server-side function names at runtime. The function library automatically
 * looks for a class whose
 * name matches the name requested (if a prefix is set, it will
 * look for a class whose name is prefix + name requested). If such a class
 * exists, and it implements the ServerSideFunction interface, a new instance
 * of the class is created using the default constructor, added to the list
 * of available functions, and returned to the caller. <p>
 * <p/>
 * This is not quite as complicated as it sounds. For instance, say the
 * FunctionLibrary's prefix is set to "opendap.servers.test.SSF". Then the
 * first time
 * the server gets a constraint expression containing the function "dummy()",
 * the FunctionLibrary will
 * automatically load the class "opendap.servers.test.SSFdummy", and return
 * a new instance using the class's default constructor. <p>
 * <p/>
 * This avoids the need to hardcode a list of server-side functions, and
 * allows you to create new functions at any time. For instance to create
 * a function "newfn()" you can simply
 * create the class "opendap.servers.test.SSFnewfn" and put it somewhere
 * in the server's classpath . Then the first CE containing this function will
 * cause that class to be automatically loaded, just the like the dummy()
 * function was.
 *
 * @author joew
 */
public class FunctionLibrary {

    /**
     * Creates a new FunctionLibrary with no prefix set.
     */
    public FunctionLibrary() {
        this("");
    }

    /**
     * Creates a new FunctionLibrary.
     *
     * @param prefix A string that will be prepended to function names in order
     *               to create a classname for that function. For example,
     */
    public FunctionLibrary(String prefix) {
        this.prefix = prefix;
        this.boolFunctions = new HashMap();
        this.btFunctions = new HashMap();
    }

    /**
     * Sets the prefix to use for classname lookup.
     * For instance, if the prefix
     * is "myserver.SSF", then when the library doesn't have an entry
     * for function "xyz" it will look for a class "myserver.SSFxyz".
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix being used for classname lookup.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Adds a function to the library. The function will be inspected
     * to determine whether it is a boolean or BaseType function.
     */
    public void add(ServerSideFunction function) {
        if (function instanceof BoolFunction) {
            boolFunctions.put(function.getName(), function);
        }
        if (function instanceof BTFunction) {
            btFunctions.put(function.getName(), function);
        }
    }

    /**
     * Retrieves a boolean function from the library. If the function
     * is not found the library will attempt to load it using the
     * mechanism described in the class documentation.
     *
     * @param name The name of the function being requested.
     * @return Null if the function is not
     *         in the library, and the attempt to load it fails.
     */
    public BoolFunction getBoolFunction(String name)
            throws NoSuchFunctionException {

        if (!boolFunctions.containsKey(name)) {
            loadNewFunction(name);
        }
        return (BoolFunction) boolFunctions.get(name);
    }


    /**
     * Retrieves a BaseType function from the library. If the function
     * is not found the library will attempt to load it using the
     * mechanism described in the class documentation.
     *
     * @param name The name of the function being requested.
     * @return Null if the function is not
     *         in the library, and the attempt to load it fails.
     */
    public BTFunction getBTFunction(String name)
            throws NoSuchFunctionException {

        if (!btFunctions.containsKey(name)) {
            loadNewFunction(name);
        }
        return (BTFunction) btFunctions.get(name);
    }

    /**
     * Tries to load a function with the given name.
     */
    protected void loadNewFunction(String name) {
        try {
            String fullName = prefix + name;
            Class value = Class.forName(fullName);
            if ((ServerSideFunction.class).isAssignableFrom(value)) {
                add((ServerSideFunction) value.newInstance());
                return;
            }
        } catch (ClassNotFoundException e) {
        } catch (IllegalAccessException e) {
        } catch (InstantiationException e) {
        }
    }

    protected Map boolFunctions;
    protected Map btFunctions;
    protected String prefix;
}



