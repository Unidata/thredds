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


package opendap.dap;

import java.net.URLConnection;

import ucar.nc2.util.net.HTTPMethod;
import org.apache.http.*;

/**
 * Aprses and holds the Server Version information returned by a DAP server.
 * This information is used to determine the version of the DAP protocol used to
 * by the DAP server to encode the data.<br>
 * <br>
 * Currently the Server Version can come from
 * more than one source. An DAP server responding to to a client request
 * over HTTP must include either an <code>XDAP</code> header or an <code>
 * XDODS-Server</code> header. <br>>
 * <br>
 * The  <code>XDAP</code> header content is defined as the protocol version
 * of the transmission and must be of the form: <br>
 * <br>
 * MV.mv[.mmv]<br>
 * <br>
 * Where:<br>
 * <br>
 * MV = Major Version Number<br>
 * mv = Minor Version Number<br>
 * mmv = An optional sub-minor version number.<br>
 * <br>
 * For example an  <code>XDAP</code> header value of 3.2 indicates a major version of 3
 * minor version of 2. An  <code>XDAP</code> header value of 2.6.3 indicates a major
 * version number of 2, a minor version number of 6, and  a subminor version
 * of 3.<br>
 * <br>
 * The <code>XDODS-Server</code> header is a legacy mechanism which
 * identifies the version of the server software generating the response.
 * This is somewhat loosely coupled to the protocol version and was the
 * legacy mechanism for identifying the protocol version. It should be
 * considered deprecated. Thus, clients seeking to read data from OPeNDAP
 * servers should first check the server response for the existence of an
 * <code>XDAP</code> header, if one is not found then the client should
 * check for an <code>XDODS-Server</code> header. If the repsonse is missing
 * both headers then an exception should be thrown as the server response is
 * invalid.
 *
 * @author ndp
 * @see ClientIO
 * @see DDS
 */
public class ServerVersion implements java.io.Serializable {

    static final long serialVersionUID = 1;

    public static final int XDODS_SERVER = 0;
    public static final int XDAP         = 1;

    public static final String DAP2_PROTOCOL_VERSION = "3.2";


    /**
     * Major version number.
     */
    private int major;
    /**
     * Minor version number.
     */
    private int minor;
    /**
     * Sub-Minor version number.
     */
    private int subminor;
    /**
     * Full version string.
     */
    private String versionString;


    /**
     * Determines Server (Protocol) Version based on the headers associated
     * with the passed org.apache.http.methods.GetMethod.
     *
     * @param method The GetMethod containing the DAP2 headers.
     * @throws DAP2Exception When bad things happen (like the headers are
     *                       missing or incorrectly constructed.
     */
    public ServerVersion(HTTPMethod method) throws DAP2Exception {

        // Did the Server send an XDAP header?
        Header h = method.getResponseHeader("XDAP");
        if (h != null) {
            versionString = h.getValue();
            processXDAPVersion(versionString);
            return;

        }

        // Did the Server send an XDODS-Server header?
        h = method.getResponseHeader("XDODS-Server");
        if (h != null) {
            versionString = h.getValue();
            processXDODSServerVersion(versionString);
            return;
        }

        // This is important! If neither of these headers (XDAP or
        // XDODS-Server) is present then we are not connected to a real
        // OPeNDAP server. Period. Without the information contained
        // in these headers some data types (Such as Sequence) cannot
        // be correctly serialized/deserialized.
        throw new DAP2Exception("Not a valid OPeNDAP server - " +
                "Missing MIME Header fields! Either \"XDAP\" " +
                "or \"XDODS-Server.\" must be present.");


    }


    /**
     * Determines Server (Protocol) Version based on the headers associated
     * with the passed java.net.URLConnection.
     *
     * @param connection The URLCOnnection containing the DAP2 headers
     * @throws DAP2Exception When bad things happen (like the headers are
     *                       missing or incorrectly constructed.
     */
    public ServerVersion(URLConnection connection) throws DAP2Exception {

        // Did the Server send an XDAP header?
        String sHeader_server = connection.getHeaderField("XDAP");
        if (sHeader_server != null) {
            processXDAPVersion(sHeader_server);
            return;
        }

        // Did the Server send an XDODS-Server header?
        sHeader_server = connection.getHeaderField("XDODS-Server");

        if(sHeader_server == null)
            // Did the Server send an xdods-server header?
            sHeader_server = connection.getHeaderField("xdods-server");


        if (sHeader_server != null) {
            processXDODSServerVersion(sHeader_server);
            return;

        }


        // This is important! If neither of these headers (XDAP or
        // XDODS-Server is present then we are not connected to a real
        // OPeNDAP server. Period. Without the information contained
        // in these headers some data types (Such as Sequence) cannot
        // be correctly serialized/deserialized.

        throw new DAP2Exception("Not a valid OPeNDAP server - " +
                "Missing MIME Header fields! Either \"XDAP\" " +
                "or \"XDODS-Server.\" must be present.");


    }


    /**
     * Construct a new ServerVersion, setting major and minor version based
     * on the full version string. Currently the Server Version can come from
     * more than one source. An OPeNDAP server responding to to a client request
     * over HTTP must include either an <code>XDAP</code> header or an <code>
     * XDODS-Server</code> header. <br>>
     * <br>
     * The  <code>XDAP</code> header content is defined as the protocol version
     * of the transmission and must be of the form: <br>
     * <br>
     * MV.mv[.mmv]<br>
     * <br>
     * Where:<br>
     * <br>
     * MV = Major Version Number<br>
     * mv = Minor Version Number<br>
     * mmv = An optional sub-minor version number.<br>
     * <br>
     * For example an  <code>XDAP</code> header value of 3.2 indicates a major version of 3
     * minor version of 2. An  <code>XDAP</code> header value of 2.6.3 indicates a major
     * version number of 2, a minor version number of 6, and  a subminor version
     * of 3.<br>
     * <br>
     * The <code>XDODS-Server</code> header is a legacy mechanism which
     * identifies the version of the server software generating the response.
     * This is somewhat loosely coupled to the protocol version and was the
     * legacy mechanism for identifying the protocol version. It should be
     * considered deprecated. Thus, clients seeking to read data from OPeNDAP
     * servers should first check the server response for the existence of an
     * <code>XDAP</code> header, if one is not found then the client should
     * check for an <code>XDODS-Server</code> header. If the repsonse is missing
     * both headers then an exception should be thrown as the server response is
     * invalid.
     *
     * @param ver        the full version string.
     * @param headerType The type of header that the version was read from.
     *                   May be set to <code>ServerVersion.XDODS_SERVER</code> or
     *                   <code>ServerVersion.XDAP</code>
     * @throws DAP2Exception When the things go wrong.
     */
    public ServerVersion(String ver, int headerType) throws DAP2Exception {

        this.versionString = ver;
        this.major = this.minor = 0;  // set version to default values

        this.subminor = -1;

        // LogStream.out.println("Server Version String: " + ver);

      
        switch (headerType) {
            case XDAP:
                processXDAPVersion(ver);
                break;
            case XDODS_SERVER:
                processXDODSServerVersion(ver);
                break;
            default:
                throw new DAP2Exception("Invalid Header Type. Must be one of " +
                        "ServerVersion.XDAP or ServerVersion.XDODS_SERVER");

        }


    }


    private void processXDODSServerVersion(String ver) throws DAP2Exception {


        String badVersionMsg = "Invalid XDODS-Server header: " + ver +
                "  Version must contain an identifying word (ex: opendap or " +
                "DODS followed by a \"/\" and then MV.mv (Where MV = " +
                "MajorVersionNumber and mv = MinorVersionNumber)";
        // search for the String, e.g. DODS/2.15, and set major and minor
        // accordingly
        int verIndex = ver.indexOf("/");
        if (verIndex != -1) {
            // This skips over the identifying word (dods, opendap, dap, etc)
            verIndex += 1;  // skip over "/" to number
        }
        else {
            // If the identifying word is missing then we punt and try to
            // read the value as if it is just the Major.Minor number.
            // Which is really bullshit, but a bunch of servers got built that
            // do corrrectly utilze this parameter.
            verIndex = 0;
        }

        int dotIndex = ver.indexOf('.', verIndex);
        if (dotIndex != -1) {
            String majorString = ver.substring(verIndex, dotIndex);
            major = Integer.parseInt(majorString);
            String minorString = ver.substring(dotIndex + 1);
            int minorDotIndex = minorString.indexOf('.');
            if (minorDotIndex != -1) {
                minor = Integer.parseInt(minorString.substring(0, minorDotIndex));
                subminor = Integer.parseInt(minorString.substring(minorDotIndex + 1));
            } else
                minor = Integer.parseInt(minorString);
        } else {
            throw new DAP2Exception(badVersionMsg);
        }



    }


    private void processXDAPVersion(String ver) {

        int dotIndex = ver.indexOf('.');
        if (dotIndex != -1) {
            String majorString = ver.substring(0, dotIndex);
            major = Integer.parseInt(majorString);
            String minorString = ver.substring(dotIndex + 1);
            int minorDotIndex = minorString.indexOf('.');
            if (minorDotIndex != -1)
                minor = Integer.parseInt(minorString.substring(0, minorDotIndex));
            else
                minor = Integer.parseInt(minorString);
        }

    }


    /**
     * Construct a new ServerVersion, setting major and minor version explicitly.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     */
    public ServerVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Returns the major version number.
     *
     * @return the major version number.
     */
    public final int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version number.
     */
    public final int getMinor() {
        return minor;
    }


    /**
     * Returns the sub-minor version number, if it exists.
     *
     * @return the minor version number or -1 if the sub-minor version has not been set.
     */
    public final int getSubMinor() {
        return minor;
    }

    /**
     * Returns the full version string.
     *
     * @return the full version string.
     */
    public final String toString() {

        String version = major + "." + minor;

        if (subminor >= 0)
            version += "." + subminor;


        return "Version string: " + versionString + " produces headers  XDAP: " + version + "  XDODS-Server: DODS/" + version;
    }


    /**
     * Returns the full version string.
     *
     * @return the full version string.
     */
    public final String getVersionString() {
        return versionString;

    }


}


