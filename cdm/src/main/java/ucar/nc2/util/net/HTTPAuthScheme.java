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

import org.apache.commons.httpclient.Credentials;
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
// Scheme enumeration

static public enum Scheme {
    NULL(null),
    BASIC("BASIC"),
    DIGEST("DIGEST"),
    KEYSTORE("KEYSTORE"),
    PROXY("PROXY");

    // Define the associated standard name
    private final String name;
    Scheme(String name) {
        this.name = name;
    }
    public String schemeName()   { return name; }
 
    static public Scheme schemeForName(String name)
    {
	if(name != null) {
  	    for(Scheme s: Scheme.values()) {
  	        if(name.equals(s.name())) return s;
	    }
	}
	return null;
    }

}

// Convenience
static final public Scheme NULL = Scheme.NULL;
static final public Scheme BASIC = Scheme.BASIC;
static final public Scheme DIGEST = Scheme.DIGEST;
static final public Scheme KEYSTORE = Scheme.KEYSTORE;
static final public Scheme PROXY = Scheme.PROXY;

//////////////////////////////////////////////////
// Predefined keys (Used local to the package)

static final String PRINCIPAL = "ucar.nc2.principal";
static final String URI = "ucar.nc2.uri";
static final String CREDENTIALSPROVIDER = "ucar.nc2.credentialsprovider";
static final String KEYSTOREPATH = "ucar.nc2.keystore";
static final String KEYSTOREPASSWORD = "ucar.nc2.keystorepassword";
static final String TRUSTSTOREPATH = "ucar.nc2.truststore";
static final String TRUSTSTOREPASSWORD = "ucar.nc2.truststorepassword";
static final String CREDENTIALS = "ucar.nc2.credentials";
static final String AUTHSTRING = "ucar.nc2.authstring";
static String SCHEME = "ucar.nc2.scheme";
static String PASSWORD = "ucar.nc2.password";
static String USER = "ucar.nc2.user";
static public final String WWW_AUTH_RESP = "Authorization";   // from HttpMethodDirector
static public final String PROXY_AUTH_RESP = "Proxy-Authorization"; // from HttpMethodDirector

//////////////////////////////////////////////////
// Instance variables

protected HashMap<String, Object> params;
protected AuthScheme basescheme;
protected String schemename;
protected HTTPAuthScheme.Scheme scheme;

//////////////////////////////////////////////////
// Constructor(s)

public HTTPAuthScheme(HTTPAuthScheme.Scheme scheme)
{
    params = new HashMap<String, Object>();
    this.scheme = scheme;
    switch (scheme) {
    case BASIC: case NULL:
	basescheme = new BasicScheme();
	break;
    case DIGEST:
	basescheme = new DigestScheme();
	break;
    case KEYSTORE:
	basescheme = new ucar.nc2.util.net.KeyStoreScheme();
	break;
    case PROXY:
	basescheme = new ProxyScheme();
	break;
    }
    insert(SCHEME, this.scheme);
}

public HTTPAuthScheme(HTTPAuthScheme other)
{
    this(other==null?null:other.getScheme());
    if(other != null) {
        Set<String> keys = other.getContents().keySet();
        for(String key: keys) {
            Object value = other.get(key);
            insert(key,value);
        }
    }
}

//////////////////////////////////////////////////

public Scheme getScheme()
{
    return scheme;
}

public String getSchemeName()
{
    return (scheme == null?null:scheme.schemeName());
}

public HTTPAuthScheme
setCredentialsProvider(CredentialsProvider cp)
{
    if(cp != null) {
        insert(SCHEME, this.scheme);
        insert(CREDENTIALSPROVIDER, cp);
    }
    return this;
}

public HTTPAuthScheme
setUserPassword(String user, String pwd)
{
    insert(PASSWORD, pwd);
    insert(USER, user);
    return this;
}

public HTTPAuthScheme
setKeystore(String keypath, String keypwd)
{
    return setKeyStore(keypath, keypwd, null, null);
}

public HTTPAuthScheme
setKeyStore(String keypath, String keypwd, String trustpath, String trustpwd)
{
    insert(KEYSTOREPATH, keypath);
    insert(KEYSTOREPASSWORD, keypwd);
    insert(TRUSTSTOREPATH, trustpath);
    insert(TRUSTSTOREPASSWORD, trustpwd);
    return this;
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
{
    return params;
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
        else if(value instanceof CredentialsProvider || value instanceof Credentials)
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
    case BASIC:  case PROXY:
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
        ostream.writeObject(get(KEYSTOREPATH));
        ostream.writeObject(get(KEYSTOREPASSWORD));
        ostream.writeObject(get(TRUSTSTOREPATH));
        ostream.writeObject(get(TRUSTSTOREPASSWORD));
        break;
    case NULL:
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
    case BASIC: case PROXY:
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
        if (keystore != null) insert(KEYSTOREPATH, keystore);
        if (keystorepassword != null) insert(KEYSTOREPASSWORD, keystorepassword);
        if (truststore != null) insert(TRUSTSTOREPATH, truststore);
        if (truststorepassword != null) insert(TRUSTSTOREPASSWORD, truststorepassword);
        break;
    case NULL:
        throw new NotSerializableException();
    }

}

//////////////////////////////////////////////////
// AuthScheme Interface

/* NOT IMPLEMENTED

public void
processChallenge(String s) throws MalformedChallengeException
{
    if(basescheme != null)
	return basescheme.processChallenge(s);
}
    
public String
getSchemeName()
{
    if(basescheme != null)
	return basescheme.getSchemeName(s);
    if(schemeName != null)
	return schemeName;
    return null;
}
    
public String
getParameter(String s)
{
    if(basescheme != null)
	return basescheme.getParameter(s);
    return null;
}
    
public String
getRealm()
{
    if(basescheme != null)
	return basescheme.getRealm();
    return null;
}
    
public boolean
isConnectionBased()
{
    if(basescheme != null)
	return basescheme.isConnectionBased();
    return false;
}
    
public boolean
isComplete()
{
    if(basescheme != null)
	return basescheme.isComplete();
    return false;
}
    
@Deprecated
public String
getID()
{
    if(basescheme != null)
	return basescheme.getID();
    return null;
}
    
@deprecated
public String
authenticate(Credentials credentials, String s, String s1) throws AuthenticationException
{
    if(basescheme != null)
	return basescheme.authenticate(credentials,s,s1);
    return null;
}
    
public String
authenticate(Credentials credentials, HttpMethod httpMethod) throws AuthenticationException
{
    if(basescheme != null)
	return basescheme.authenticate(credentials,httpMethod);
    return null;
}
NOT IMPLEMENTED */

//////////////////////////////////////////////////
// AuthScheme Interface

public void
processChallenge(String url)
    throws MalformedChallengeException
{
    this.basescheme.processChallenge();
}
    
/**
 * Subclass must implement
 */

public String getSchemeName()
{
    return this.schemename;
}
    
public String
getParameter(String key)
{
    Object value = params.get(key);
    if(value == null)
	value = this.basescheme.getParameter();
    if(value != null) value = value.toString();
    return value;
}
    
public String
getRealm()
{
    return this.basescheme.getRealm();
}
    
@Deprecated
public String
getID()
{
    return this.basescheme.getID();
}
    
public boolean
isConnectionBased()
{
    return this.basescheme.isConnectionBased();
}
    
public boolean
isComplete()
{
    return this.basescheme.isComplete();
}
    
@Deprecated
public String
authenticate(Credentials credentials, String url, String url1)
    throws AuthenticationException
{
    return this.basescheme.authenticate(credentials,url,url1);
}
    
public String
authenticate(Credentials credentials, HttpMethod httpMethod)
    throws AuthenticationException
{
    return this.basescheme.authenticate(credentials,method);
}

}//HTTPAuthScheme

