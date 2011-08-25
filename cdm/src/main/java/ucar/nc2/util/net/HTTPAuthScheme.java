/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.httpclient.auth.*;

import static ucar.nc2.util.net.HTTPAuthStore.*;

/**
 * HTTPAuthScheme contains the necessary information to support a given
 * authorization scheme in the context of HTTPSession.
 * <p/>
 * It is intended to be thread safe using, currently,
 * serial (synchronized) access.
 * <p/>
 * The primary component of HTTPAuthScheme is a (key,value) pair
 * store implementing the HttpParams Interface.  The contents of the pair
 * store depends on the particular auth scheme (HTTP Basic, ESG Keystore,
 * etc.)
 *
 * HTTPAuthScheme implements the AuthScheme interface, but its functionality
 * is intended to be broader than that interface.
 */

public class HTTPAuthScheme implements Serializable, AuthScheme
{

//////////////////////////////////////////////////
// Predefined keys

// Package access only

    static final String PRINCIPAL = "ucar.nc2.principal";
    static final String URI = "ucar.nc2.uri";
    static final String CREDENTIALSPROVIDER = "ucar.nc2.credentialsprovider";
    static final String KEYSTORE = "ucar.nc2.keystore";
    static final String KEYSTOREPASSWORD = "ucar.nc2.keystorepassword";
    static final String TRUSTSTORE = "ucar.nc2.truststore";
    static final String TRUSTSTOREPASSWORD = "ucar.nc2.truststorepassword";
    static final String CREDENTIALS = "ucar.nc2.credentials";
    static final String AUTHSTRING = "ucar.nc2.authstring";
    static String SCHEME = "ucar.nc2.scheme";
    static String PASSWORD = "ucar.nc2.password";
    static String USER = "ucar.nc2.user";
    static public final String WWW_AUTH_RESP = "Authorization";   // from HttpMethodDirector
    static public final String PROXY_AUTH_RESP = "Proxy-Authorization"; // from HttpMethodDirector

//////////////////////////////////////////////////

//////////////////////////////////////////////////
// Instance variables
    protected HashMap<String, Object> params;

//////////////////////////////////////////////////
// Constructor(s)

    public HTTPAuthScheme()
    {
        params = new HashMap<String, Object>();
    }

//////////////////////////////////////////////////
// External API: One or more procedures per scheme

// Factory methods

// Scheme.BASIC

    static public HTTPAuthScheme
    createSchemeHttpBasic(CredentialsProvider provider)
    {
        HTTPAuthScheme creds = new HTTPAuthScheme();
        creds.insert(SCHEME, Scheme.BASIC);
        creds.insert(CREDENTIALSPROVIDER, provider);
        return creds;
    }

    static public HTTPAuthScheme
    createSchemeHttpBasic(String user, String pwd)
    {
        HTTPAuthScheme creds = new HTTPAuthScheme();
        creds.insert(SCHEME, Scheme.BASIC);
        creds.insert(PASSWORD, pwd);
        creds.insert(USER, user);
        return creds;
    }

// Scheme.DIGEST

    static public HTTPAuthScheme
    createSchemeHttpDigest(CredentialsProvider provider)
    {
        HTTPAuthScheme creds = new HTTPAuthScheme();
        creds.insert(SCHEME, Scheme.DIGEST);
        creds.insert(CREDENTIALSPROVIDER, provider);
        return creds;
    }

    static public HTTPAuthScheme
    createSchemeHttpDigest(String user, String pwd)
    {
        HTTPAuthScheme creds = new HTTPAuthScheme();
        creds.insert(SCHEME, Scheme.DIGEST);
        creds.insert(PASSWORD, pwd);
        creds.insert(USER, user);
        return creds;
    }

// Scheme.KEYSTORE
    static public HTTPAuthScheme
    createSchemeKeystore(String keypath, String keypwd)
    {
        return createSchemeKeystore(keypath, keypwd, null, null);
    }

    static public HTTPAuthScheme
    createSchemeKeystore(String keypath, String keypwd, String trustpath, String trustpwd)
    {
        HTTPAuthScheme creds = new HTTPAuthScheme();
        creds.insert(SCHEME, Scheme.KEYSTORE);
        creds.insert(KEYSTORE, keypath);
        creds.insert(KEYSTOREPASSWORD, keypwd);
        creds.insert(TRUSTSTORE, trustpath);
        creds.insert(TRUSTSTOREPASSWORD, trustpwd);
        return creds;
    }

// Scheme.OTHER

    static public HTTPAuthScheme
    createSchemeOther(String authstring)
    {
        HTTPAuthScheme creds = new HTTPAuthScheme();
        creds.insert(SCHEME, Scheme.OTHER);
        creds.insert(AUTHSTRING, authstring);
        return creds;
    }

//////////////////////////////////////////////////
// Internal API: package access only

    synchronized boolean
    insert(String key, Object value)
    {
        boolean found = params.containsKey(key);
        params.put(key, value);
        return found;
    }

    synchronized boolean
    remove(String key)
    {
        boolean found = params.containsKey(key);
        params.remove(key);
        return found;
    }

    synchronized Object
    get(String key)
    {
        return params.get(key);
    }

    synchronized HashMap<String,Object>
    getContents()
    {return params;}
//////////////////////////////////////////////////
// Clone-like Interface, but never fails

    public HTTPAuthScheme duplicate()
    {
        HTTPAuthScheme clone = new HTTPAuthScheme();
        Set<String> keys = params.keySet();
        for(String key: keys) {
            Object value = params.get(key);
            clone.insert(key,value);
        }
        return clone;
    }

///////////////////////////////////////////////////
// toString

    public String
    toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        boolean first = true;
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (!first) buf.append(",");
            if(value == null)
                buf.append(key + "=" + "null");
            else if(value instanceof CredentialsProvider || value instanceof org.apache.commons.httpclient.Credentials)
                buf.append(key + "=" + value.getClass().getName());
            else
                buf.append(key + "=" + value.toString());
            first = false;
        }
        buf.append("}");
        return buf.toString();
    }

///////////////////////////////////////////////////
// (De-)Serialization support

    private void writeObject(java.io.ObjectOutputStream ostream)
            throws IOException
    {
        Scheme scheme = (Scheme) get(SCHEME);
        ostream.writeObject(scheme);

        switch (scheme) {
        case BASIC:
            CredentialsProvider cp = (CredentialsProvider) get(CREDENTIALSPROVIDER);
            if (cp != null) {
                // This is a bit tricky; if the provider cannot be serialized,
                // then we will serialize the class. This means that any
                // state of provider will be lost.
                // Also, any stored credentials will not be saved.
                boolean canserialize = (cp instanceof Serializable);
                ostream.writeBoolean(canserialize);
                if (canserialize) {
                    ostream.writeObject(cp);
                } else {
                    ostream.writeObject(cp.getClass());
                }
            } else {
                ostream.writeObject(cp);
            }
            ostream.writeObject(get(USER));
            ostream.writeObject(get(PASSWORD));
            break;
        case KEYSTORE:
            ostream.writeObject(get(KEYSTORE));
            ostream.writeObject(get(KEYSTOREPASSWORD));
            ostream.writeObject(get(TRUSTSTORE));
            ostream.writeObject(get(TRUSTSTOREPASSWORD));
            break;
        case OTHER:
            ostream.writeObject(get(AUTHSTRING));
            break;
        default:
            throw new NotSerializableException();
        }
    }

    private void readObject(java.io.ObjectInputStream istream)
            throws IOException, ClassNotFoundException
    {
        if (params == null) params = new HashMap<String, Object>();

        Scheme scheme = (Scheme) istream.readObject();
        if (scheme != null) insert(SCHEME,scheme);

        switch (scheme) {
        case BASIC:
            boolean canserialize = istream.readBoolean();
            CredentialsProvider cp = null;
            if (canserialize) {
                cp = (CredentialsProvider) istream.readObject();
            } else {
                Class c = (Class) istream.readObject();
                try {
                    cp = (CredentialsProvider) c.newInstance();
                } catch (Exception e) {
                    throw new ClassNotFoundException(c.getName(), e);
                }
            }
            String user = (String) istream.readObject();
            String pwd = (String) istream.readObject();
            if (cp != null) insert(CREDENTIALSPROVIDER, cp);
            if (user != null) insert(USER, user);
            if (pwd != null) insert(PASSWORD, pwd);
            break;
        case KEYSTORE:
            String keystore = (String) istream.readObject();
            String keystorepassword = (String) istream.readObject();
            String truststore = (String) istream.readObject();
            String truststorepassword = (String) istream.readObject();
            if (keystore != null) insert(KEYSTORE, keystore);
            if (keystorepassword != null) insert(KEYSTOREPASSWORD, keystorepassword);
            if (truststore != null) insert(TRUSTSTORE, truststore);
            if (truststorepassword != null) insert(TRUSTSTOREPASSWORD, truststorepassword);
            break;
        case OTHER:
            String authstring = (String) istream.readObject();
            if (authstring != null) insert(AUTHSTRING, authstring);
            break;
        default:
            throw new NotSerializableException();
        }

    }


}
