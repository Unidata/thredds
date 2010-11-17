/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.servlet;

import opendap.util.EscapeStrings;

import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User requests get cached here so that downstream code can access
 * the details of the request information.
 *
 * @author Nathan Potter
 */

public class ReqState {


    /**
     * ************************************************************************
     * Default directory for the cached DDS files. This
     * presupposes that the server is going to use locally
     * cached DDX files.
     *
     * @serial
     */
    private String defaultDDXcache;


    /**
     * ************************************************************************
     * Default directory for the cached DDS files. This
     * presupposes that the server is going to use locally
     * cached DDS files.
     *
     * @serial
     */
    private String defaultDDScache;


    /**
     * ************************************************************************
     * Default directory for the cached DAS files. This
     * presupposes that the server is going to use locally
     * cached DAS files.
     *
     * @serial
     */
    private String defaultDAScache;


    /**
     * ************************************************************************
     * Default directory for the cached INFO files. This
     * presupposes that the server is going to use locally
     * cached INFO files.
     *
     * @serial
     */
    private String defaultINFOcache;

    /**
     * ************************************************************************
     * Default directory for the cached INFO files. This
     * presupposes that the server is going to use locally
     * cached INFO files.
     *
     * @serial
     */
    private final String defaultSchemaName = "opendap-0.0.0.xsd";
    private String defaultSchemaLocation;


    private String dataSetName;
    private String requestSuffix;
    private String CE;
    private Object obj = null;
    private String serverClassName;
    private String requestURL;

    private ServletConfig myServletConfig;
    private HttpServletRequest myHttpRequest;
    private HttpServletResponse response;


    public ReqState(HttpServletRequest myRequest, HttpServletResponse response,
                    ServletConfig sc,
                    String serverClassName) throws BadURLException {

        this.myServletConfig = sc;
        this.myHttpRequest = myRequest;
        this.response = response;
        this.serverClassName = serverClassName;

        // Get the constraint expression from the request object and
        // convert all those special characters denoted by a % sign
        //this.CE = prepCE(myHttpRequest.getQueryString());
        String query = myHttpRequest.getQueryString();
        this.CE = (query == null) ? "" : EscapeStrings.www2ce(query);

        // If there was simply no constraint then prepCE() should have returned
        // a CE equal "", the empty string. A null return indicates an error.
        if (this.CE == null) {
            this.CE = "";
            //throw new BadURLException();
        }


        processDodsURL();


        String servletPath = myHttpRequest.getServletPath();
        defaultDDXcache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/ddx") + "/";
        defaultDDScache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/dds") + "/";
        defaultDAScache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/das") + "/";
        defaultINFOcache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                servletPath + "/info") + "/";

        int index = myHttpRequest.getRequestURL().lastIndexOf(
                myHttpRequest.getServletPath());

        defaultSchemaLocation = myHttpRequest.getRequestURL().substring(0, index) +
                "/schema/" +
                defaultSchemaName;

        //System.out.println("Default Schema Location: "+defaultSchemaLocation);
        //System.out.println("Schema Location: "+getSchemaLocation());


        requestURL = myHttpRequest.getRequestURL().toString();


    }

    public String getDataSet() {
        return dataSetName;
    }

    public String getServerClassName() {
        return serverClassName;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public String getConstraintExpression() {
        return CE;
    }

    public HttpServletRequest getRequest() {
      return myHttpRequest;
    }

    public HttpServletResponse getResponse() {
      return response;
    }


    /**
     * This method will attempt to get the DDX cache directory
     * name from the servlet's InitParameters. Failing this it
     * will return the default DDX cache directory name.
     *
     * @return The name of the DDX cache directory.
     */
    public String getDDXCache() {
        String cacheDir = getInitParameter("DDXcache");
        if (cacheDir == null)
            cacheDir = defaultDDXcache;
        return (cacheDir);
    }

    /**
     * Sets the default DDX Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>DDXCache</i> the default
     * value will be ingnored.
     *
     * @param cachedir
     */
    public void setDefaultDDXCache(String cachedir) {
        defaultDDXcache = cachedir;
    }

    /**
     * This method will attempt to get the DDS cache directory
     * name from the servlet's InitParameters. Failing this it
     * will return the default DDS cache directory name.
     *
     * @return The name of the DDS cache directory.
     */
    public String getDDSCache() {
        String cacheDir = getInitParameter("DDScache");
        if (cacheDir == null)
            cacheDir = defaultDDScache;
        return (cacheDir);
    }

    /**
     * Sets the default DDS Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>DDSCache</i> the default
     * value will be ingnored.
     *
     * @param cachedir
     */
    public void setDefaultDDSCache(String cachedir) {
        defaultDDScache = cachedir;
    }

    /**
     * This method will attempt to get the DAS cache directory
     * name from the servlet's InitParameters. Failing this it
     * will return the default DAS cache directory name.
     *
     * @return The name of the DAS cache directory.
     */
    public String getDASCache() {
        String cacheDir = getInitParameter("DAScache");
        if (cacheDir == null)
            cacheDir = defaultDAScache;
        return (cacheDir);
    }

    /**
     * Sets the default DAS Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>DASCache</i> the default
     * value will be ingnored.
     *
     * @param cachedir
     */
    public void setDefaultDASCache(String cachedir) {
        defaultDAScache = cachedir;
    }


    /**
     * This method will attempt to get the INFO cache directory
     * name from the servlet's InitParameters. Failing this it
     * will return the default INFO cache directory name.
     *
     * @return The name of the INFO cache directory.
     */
    public String getINFOCache() {
        String cacheDir = getInitParameter("INFOcache");
        if (cacheDir == null)
            cacheDir = defaultINFOcache;
        return (cacheDir);
    }

    /**
     * Sets the default INFO Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>INFOcache</i> the default
     * value will be ingnored.
     *
     * @param cachedir
     */
    public void setDefaultINFOCache(String cachedir) {
        defaultINFOcache = cachedir;
    }


    /**
     * This method will attempt to get the Schema Location
     * name from the servlet's InitParameters. Failing this it
     * will return the default Schema Location.
     *
     * @return The Schema Location.
     */
    public String getSchemaLocation() {
        String cacheDir = getInitParameter("SchemaLocation");
        if (cacheDir == null)
            cacheDir = defaultSchemaLocation;
        return (cacheDir);
    }

    /**
     * Sets the default Schema Location to
     * the string <i>location</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>SchemaLocation</i> the default
     * value will be ingnored.
     *
     * @param location
     */
    public void setDefaultSchemaLocation(String location) {
        defaultSchemaLocation = location;
    }


    /**
     * *************************************************************************
     * This method is used to convert special characters into their
     * actual byte values.
     * <p/>
     * For example, in a URL the space character
     * is represented as "%20" this method will replace that with a
     * space charater. (a single value of 0x20)
     *
     * @param ce The constraint expresion string as collected from the request
     *           object with <code>getQueryString()</code>
     * @return A string containing the prepared constraint expression. If there
     *         is a problem with the constraint expression a <code>null</code> is returned.
     */
    private String prepCE(String ce) {

        int index;

        //System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        //System.out.println("Prepping: \""+ce+"\"");

        if (ce == null) {
            ce = "";
            //System.out.println("null Constraint expression.");
        } else if (!ce.equals("")) {

            //System.out.println("Searching for:  %");
            index = ce.indexOf("%");
            //System.out.println("index of %: "+index);

            if (index == -1)
                return (ce);

            if (index > (ce.length() - 3))
                return (null);

            while (index >= 0) {
                //System.out.println("Found % at character " + index);

                String specChar = ce.substring(index + 1, index + 3);
                //System.out.println("specChar: \"" + specChar + "\"");

                // Convert that bad boy!
                char val = (char) Byte.parseByte(specChar, 16);
                //System.out.println("                val: '" + val + "'");
                //System.out.println("String.valueOf(val): \"" + String.valueOf(val) + "\"");


                ce = ce.substring(0, index) + String.valueOf(val) + ce.substring(index + 3, ce.length());
                //System.out.println("ce: \"" + ce + "\"");

                index = ce.indexOf("%");
                if (index > (ce.length() - 3))
                    return (null);
            }
        }

//      char ca[] = ce.toCharArray();
//	for(int i=0; i<ca.length ;i++)
//	    System.out.print("'"+(byte)ca[i]+"' ");
//	System.out.println("");
//	System.out.println(ce);
//	System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");

//        System.out.println("Returning CE: \""+ce+"\"");
        return (ce);
    }
    /***************************************************************************/


    /**
     * *************************************************************************
     * Processes an incoming <code>HttpServletRequest</code>. Uses the content of
     * the <code>HttpServletRequest</code>to create a <code>ReqState</code>
     * object in that caches the values for:
     * <ul>
     * <li> <b>dataSet</b> The data set name.(Accessible using
     * <code> setDataSet() </code>
     * and <code>getDataSet()</code>)</li>
     * <li> <b>CE</b> The constraint expression.(Accessible using
     * <code> setCE() </code>
     * and <code>getCE()</code>)</li>
     * <li> <b>requestSuffix</b> The request suffix, used by OPeNDAP DAP2 to
     * indicate the type of response desired by the client.
     * (Accessible using
     * <code> setRequestSuffix() </code>
     * and <code>getRequestSuffix()</code>)</li>
     * <li> <b>isClientCompressed</b> Does the requesting client
     * accept a compressed response?</li>
     * <li> <b>ServletConfig</b> The <code>ServletConfig</code> object
     * for this servlet.</li>
     * <li> <b>ServerName</b> The class name of this server.</li>
     * <li> <b>RequestURL</b> THe URL that that was used to call thye servlet.</li>
     * </ul>
     *
     * @see ReqState
     */

    protected void processDodsURL() {

        // Figure out the data set name.
        this.dataSetName = myHttpRequest.getPathInfo();
        this.requestSuffix = null;
        if (this.dataSetName != null) {
            // Break the path up and find the last (terminal)
            // end.
            StringTokenizer st = new StringTokenizer(this.dataSetName, "/");
            String endOPath = "";
            while (st.hasMoreTokens()) {
                endOPath = st.nextToken();
            }

            // Check the last element in the path for the
            // character "."
            int index = endOPath.lastIndexOf('.');

            //System.out.println("last index of . in \""+ds+"\": "+index);

            // If a dot is found take the stuff after it as the DAP2 request suffix
            if (index >= 0) {
                // pluck the DAP2 request suffix off of the end
                requestSuffix = endOPath.substring(index + 1);

                // Set the data set name to the entire path minus the
                // suffix which we know exists in the last element
                // of the path.
                this.dataSetName = this.dataSetName.substring(1, this.dataSetName.lastIndexOf('.'));
            } else { // strip the leading slash (/) from the dataset name and set the suffix to an empty string
                requestSuffix = "";
                this.dataSetName = this.dataSetName.substring(1, this.dataSetName.length());
            }
        }
    }

    /**
     * *************************************************************************
     * Evaluates the (private) request object to determine if the client that
     * sent the request accepts compressed return documents.
     *
     * @return True is the client accpets a compressed return document.
     *         False otherwise.
     */

    public boolean getAcceptsCompressed() {

        boolean isTiny;

        isTiny = false;
        String encoding = this.myHttpRequest.getHeader("Accept-Encoding");

        if (encoding != null)
          isTiny = encoding.contains("deflate");
        else
            isTiny = false;

        return (isTiny);
    }

    /**
     * ***********************************************************************
     */


    public Enumeration getInitParameterNames() {
        return (myServletConfig.getInitParameterNames());
    }

    public String getInitParameter(String name) {
        return (myServletConfig.getInitParameter(name));
    }

    public String getDodsBlobURL_OLDANDBUSTED() {

        int lastDot = requestURL.lastIndexOf('.');


        String blobURL = requestURL.substring(0, lastDot) + ".blob";

        if (!CE.equals(""))
            blobURL += "?" + CE;

        return (blobURL);
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

        ts = "ReqState:\n";
        ts += "  serverClassName:    '" + serverClassName + "'\n";
        ts += "  dataSet:            '" + dataSetName + "'\n";
        ts += "  requestSuffix:      '" + requestSuffix + "'\n";
        //ts += "  blobURL:            '" + getDodsBlobURL() + "'\n";
        ts += "  CE:                 '" + CE + "'\n";
        ts += "  compressOK:          " + getAcceptsCompressed() + "\n";

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


