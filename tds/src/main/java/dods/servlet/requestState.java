// $Id: requestState.java,v 1.1 2005/12/16 22:37:05 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package dods.servlet;

import java.util.Enumeration;
import javax.servlet.ServletConfig;

/**
 * User requests get parsed into this immutable object.
 *
 * @author jcaron
 * @deprecated This class has been replaced by <code>dods.servlet.ReqState</code>
 *             which allows the servlet to use bundled files for configuration.
 */

public class requestState {


    /**
     * ************************************************************************
     * Default directory for the cached DDS files. This
     * presupposes that the server is going to use locally
     * cached DDS files.
     *
     * @serial
     */
    public static final String defaultDDScache = "/home/dods/dds/";


    /**
     * ************************************************************************
     * Default directory for the cached DAS files. This
     * presupposes that the server is going to use locally
     * cached DAS files.
     *
     * @serial
     */
    public static final String defaultDAScache = "/home/dods/das/";


    /**
     * ************************************************************************
     * Default directory for the cached INFO files. This
     * presupposes that the server is going to use locally
     * cached INFO files.
     *
     * @serial
     */
    public static final String defaultINFOcache = "/home/dods/info/";


    private String dataSet;
    private String requestSuffix;
    private String CE;
    private boolean acceptsCompressed;
    private Object obj = null;
    private String serverName;

    private ServletConfig sc;


    public requestState(String dataSet,
                        String requestSuffix,
                        String CE,
                        boolean acceptsCompressed,
                        ServletConfig sc,
                        String serverName) {

        this.dataSet = dataSet;
        this.requestSuffix = requestSuffix;
        this.CE = CE;
        this.acceptsCompressed = acceptsCompressed;
        this.sc = sc;
        this.serverName = serverName;
    }

    public String getDataSet() {
        return dataSet;
    }

    public String getServerName() {
        return serverName;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public String getConstraintExpression() {
        return CE;
    }

    public boolean getAcceptsCompressed() {
        return acceptsCompressed;
    }

    public Enumeration getInitParameterNames() {
        return (sc.getInitParameterNames());
    }

    public String getInitParameter(String name) {
        return (sc.getInitParameter(name));
    }


    // for debugging, extra state, etc
    public Object getUserObject() {
        return obj;
    }

    public void setUserObject(Object userObj) {
        this.obj = userObj;
    }

    public String toString() {
        String ts;

        ts = "requestState:\n";
        ts += "  dataset: '" + dataSet + "'\n";
        ts += "  suffix: '" + requestSuffix + "'\n";
        ts += "  CE: '" + CE + "'\n";
        ts += "  compressOK: " + acceptsCompressed + "\n";

        ts += "  InitParameters:\n";
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = getInitParameter(name);

            ts += "    " + name + ": '" + value + "'\n";
        }

        return (ts);
    }


}
