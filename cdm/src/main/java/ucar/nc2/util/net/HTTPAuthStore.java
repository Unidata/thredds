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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.io.*;

import org.apache.commons.httpclient.auth.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import static ucar.nc2.util.net.HTTPAuthCreds.*;

/**

HTTPAuthStore stores tuples of authorization information in a thread
safe manner.  It currently supports serial access, but can be extended
to support single writer / multiple reader semantics
using the procedures
{acquire,release}writeaccess
and
{acquire,release}readaccess

 */

public class HTTPAuthStore implements Serializable
{


//////////////////////////////////////////////////
/**

The auth store is (conceptually) a set of tuples (rows) of the form
HTTPAuthCreds.Scheme(scheme) X String(url) X HTTPAuthCreds(creds)
the creds column specifies the kind of authorization
(e.g. basic, keystore, etc) and the info to support it.
*/

static public class Entry implements Serializable, Comparable
{
    public boolean isGlobal;
    public String uri;
    public HTTPAuthCreds scheme;

    public Entry()
    {
	this(ANY_ENTRY);
    }

    /**
     * @param entry
     */

    public Entry(Entry entry)
    {
	if(entry == null) entry = ANY_ENTRY;
	constructor(entry.isGlobal,entry.url,entry.scheme);
    }

    /**
     * @param url
     * @param scheme
     */

    public Entry(boolean isGlobal, String uri, HTTPAuthCreds scheme)
        throws HTTPException
    {
        URI u = null;
	if(uri == null) uri = ANY_URI;
	constructor(isGlobal, uri,scheme);
    }

    /**
     * Shared constructor code
     * @param isGlobal
     * @param uri
     * @param scheme
     */

    protected void constructor(boolean isGlobal, String uri, HTTPAuthCreds scheme)
    {
	if(uri == null) uri = ANY_URI;
	if(scheme != null)
            scheme = new HTTPAuthCreds(scheme);
	this.isGlobal = isGlobal;
	this.uri = uri;
	this.scheme = scheme;
    }

    public String toString()
    {
	String scheme = (this.scheme == null ? "null" : this.scheme.toString());
        return String.format("%s:%s{%s}",
		(isGlobal?"global":"local"),uri,scheme);
    }

    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(this.isGlobal);
        oos.writeObject(this.uri);
        oos.writeObject(this.scheme);
    }

    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
    {
        this.isGlobal = (boolean)ois.readObject();
        this.uri = (String)ois.readObject();
        this.scheme = (HTTPAuthCreds)ois.readObject();
    }

    /**
      * return 0 if e1 == e2, 1 if e1 > e2
      * and -1 if e1 < e2 using the following tests
      * - for a given field, all ANY_XXX should
      *   follow all fields with specific values.
      * - fields both with defined values, but different
      *   are considered equally restrictive
      * - for path/path, longer matches should precede
      *   shorter matches.
      * - the fields are considered in this order:
      * 	principal,host,port,path,scheme
      */

    public boolean equals(Entry e)
    {
        return compareTo(e) == 0);
    }

    public int compareTo(Object e1)
    {
        if(e1 instanceof Entry) {
            return compare(this,(Entry)e1);
	}
        return +1;
    }

    static protected int compare(Entry e1, Entry e2)
    {
	if(e1 == null && e2 == null) return 0;
	if(e1 != null && e2 == null) return +1;
	if(e1 == null && e2 != null) return -1;

        if(e1.isGlobal && !e2.isGlobal) return -1;
        if(!e1.isGlobal && e2.isGlobal) return +1;

	if(compareURI(e1.uri,e2.uri)) return 0l
        return e1.uri.compare(e2.uri);
    }

    /**
     * Define URI compatibility.
     */
    static protected boolean compatibleURI(String u1, String u2)
    {   
	if(u1 == u2) return true;
	if(u1 == null) return false;
	if(u2 == null) return false;

	if(u1.equals(u2)
	   || u1.startsWith(u2)
	   || u2.startsWith(u1)) return true;

        // See if u1 or u2 has a principal.
	URI ur1;
	URI ur2;
        try {
	    uu1 = new URI(u1);
	} catch (URISyntaxException use) {
	    return false;
	}
        try {
	    uu2 = new URI(u2);
	} catch (URISyntaxException use) {
	    return false;
	}

        if(!testStringEq(uu1.getProtocol(),uu2.getProtocol()))
	    return false;
        if(!testStringEq(uu1.getUserInfo(),uu2.getUserInfo()))
	    return false;
        if(!testStringEq(uu1.getHost(),uu2.getHost()))
	    return false;
        if(!testIntEq(uu1.getPort(),uu2.getPort()))
	    return false;
        if(!testInt(uu1.getPort(),uu2.getPort()))
	    return false;
        if(!testStringLex(uu1.getRawPath(),uu2.getRawPath()))
	    return false;
	return true;
    }
}

//////////////////////////////////////////////////

static public final boolean          ISGLOBAL = true;
static public final boolean          ISLOCAL = false;
static public final String           ANY_URL = "";

static final public Entry            ANY_ENTRY = new Entry(ISGLOBAL,ANY_URL,null);

static private Hashtable<HTTPSession, List<Entry>> rows;


static {
    rows = new Hashtable<HTTPSession,List<Entry>>();
    // For back compatibility, check some system properties
    // and add appropriate entries
    // 1. ESG keystore support
    String kpath = System.getProperty("keystore");
    if(kpath != null) {
        String tpath = System.getProperty("truststore");
        String kpwd = System.getProperty("keystorepassword");
        String tpwd = System.getProperty("truststorepassword");
        kpath = kpath.trim();
        if(tpath != null) tpath = tpath.trim();
        if(kpwd != null) kpwd = kpwd.trim();
        if(tpwd != null) tpwd = tpwd.trim();
        if(kpath.length() == 0) kpath = null;
        if(tpath.length() == 0) tpath = null;
        if(kpwd.length() == 0) kpwd = null;
        if(tpwd.length() == 0) tpwd = null;

        HTTPAuthCreds scheme = new HTTPAuthCreds(Scheme.KEYSTORE);
        scheme.setKeyStore(kpath,kpwd,tpath,tpwd);
        insert(ANY_URL,scheme);
     }

}

//////////////////////////////////////////////////
/**
Primary external interface
 */

/**
 * @param entry
 * @return true if entry already existed and was replaced
 */

static synchronized public boolean
insert(Entry entry)
{
    boolean rval = false;
    if(entry == null) return false;

    Entry found = null;
    for(Entry e: getAllRows()) {
	if(compatibleURI(entry.uri,e.uri)) {
	    found = e;
	    break;
	}
    }
    // If the entry already exists, then overwrite it and return true
    if(found != null) {
        found.scheme = new HTTPAuthCreds(entry.scheme);
	rval = true;
    } else {
        Entry newentry = new Entry(entry);
        list.add(newentry);
    }
    return rval;
}

/**
 * @param entry
 * @return true if entry existed and was removed
 */

static synchronized public boolean
remove(Entry entry)
{
    Entry found = null;
    for(Entry e: getAllRows()) {
	if(compatibleURI(entry.uri,e.uri)) {
	    found = e;
	    break;
	}
    }
    if(found != null) list.remove(found);
    return (found != null);
}

/**
 * Remove all auth store entries
 */
static synchronized public void
clear() throws HTTPException
{
   rows.clear(); 
}

/**
 * Return all entries in the auth store
 */
static public List<Entry>
getAllRows()
{
  List<Entry> elist = new ArrayList<Entry>();
  for(HTTPSession key: rows.keySet()) elist.addAll(rows.get(key));
  return elist;
}

/**
 * Search:
 * 
 * Search match is defined by the compatibleURI function above.
 * The return list is ordered from most restrictive to least restrictive.
 * 
 * @param entry
 * @return list of matching entries
 */
static synchronized public Entry[]
search(Entry entry)
{
    List<Entry> list = null;

    list = getAllRows();

    List<Entry> matches = new ArrayList<Entry>();

    if(list != null) {
        for(Entry e: list) {
	    if(compare(entry,e) == 0)
                matches.add(e);
        }
    }
    // Sort so isGlobal is after !isGlobal
    Entry[] matchvec = matches.toArray(new Entry[matches.size()]);
    Arrays.sort(matchvec);
    return matchvec;
}

/**
 * 
 * @param session
 * @param principal
 * @param host
 * @param port
 * @param path
 * @return list of matching entries
 */
 
//////////////////////////////////////////////////
// Misc.


static public AuthScope
getAuthScope(Entry entry)
{
    if(entry == null) return null;
    URI uri;
    try {
	uri = new URI(entry.uri);
    } catch(URISyntaxException use) {return null;}

    String host = entry.host;
    int port = entry.port;
    String realm = entry.path;
    String scheme = (entry.scheme == null ? null : entry.scheme.getSchemeName());

    if(host == null) host = AuthScope.ANY_HOST;
    if(port <= 0) port = AuthScope.ANY_PORT;
    if(realm == null) realm = AuthScope.ANY_REALM;
    AuthScope as = new AuthScope(host,port,realm,scheme);
    return as;
}


//////////////////////////////////////////////////
/**
n-readers/1-writer semantics
Note not synchronized because only called
by other synchronized procedures
 */

static int nwriters = 0;
static int nreaders = 0;
static boolean stop = false;

private void
acquirewriteaccess() throws HTTPException
{
    nwriters++;
    while(nwriters > 1) {
	try { wait(); } catch (InterruptedException e) {
	    if(stop) throw new HTTPException("interrupted");
        }
    }
    while(nreaders > 0) {
	try { wait(); } catch (InterruptedException e) {
	    if(stop) throw new HTTPException("interrupted");
        }
    }
}

private void
releasewriteaccess()
{
    nwriters--;
    notify();
}

private void
acquirereadaccess() throws HTTPException
{
    nreaders++;
    while(nwriters > 0) {
	try { wait(); } catch (InterruptedException e) {
	    if(stop) throw new HTTPException("interrupted");
        }
    }
}

private void
releasereadaccess()
{
    nreaders--;
    if(nreaders == 0) notify(); //only affects writers
}

///////////////////////////////////////////////////
// Print functions

static public void
print(PrintStream p)
	throws IOException
{
    print(new PrintWriter(p,true));
}

static public void
print(PrintWriter p)
	throws IOException
{
    List<Entry> elist = getAllRows();
    for(int i=0;i<elist.size();i++) {
	Entry e = elist.get(i);
	p.printf("[%02d] %s\n",e.toString());
    }
}

///////////////////////////////////////////////////
// Seriablizable interface
// Encrypted (De-)Serialize 

static public void
serializeAll(OutputStream ostream, String password)
	throws IOException
{
    serialize(ostream,ANY_PRINCIPAL,password);
}

static public void
serialize(OutputStream ostream, String principal, String password)
	throws HTTPException
{
    try {
        
    // Create Key
    byte key[] = password.getBytes();
    DESKeySpec desKeySpec = new DESKeySpec(key);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

    // Create Cipher
    Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
    desCipher.init(Cipher.ENCRYPT_MODE, secretKey);

    // Create crypto stream
    BufferedOutputStream bos = new BufferedOutputStream(ostream);
    CipherOutputStream cos = new CipherOutputStream(bos, desCipher);
    ObjectOutputStream oos = new ObjectOutputStream(cos);

    List<Entry> byprincipal = null;

    if(principal == ANY_PRINCIPAL) {
        byprincipal = getAllRows();
    } else {
        byprincipal = new ArrayList<Entry>();
       for(Entry e: getAllRows()) {
	    if(e.principal.equals(principal)) byprincipal.add(e);
        }
    }
    oos.writeBytes(principal==ANY_PRINCIPAL?"":principal);
    oos.writeInt(byprincipal.size());
    for(Entry e: byprincipal) {
        oos.writeObject(e);
    }

    oos.flush();
    oos.close();

    } catch (Exception e) {throw new HTTPException(e);}

}

static public void
deserialize(InputStream istream, String password)
    throws HTTPException
{
    List<Entry> entries = getDeserializedEntries(istream,password);
    for(Entry e: entries) {
        List<Entry> hashed = (e.session == ANY_SESSION?getAllRows():rows.get(e.session));
        if(hashed == null) {
            hashed = new ArrayList<Entry>();
            rows.put(e.session,hashed);
        }
        hashed.add(e);
    }
}

static public List<Entry>
getDeserializedEntries(InputStream istream, String password)
	throws HTTPException
{
    try {

    // Create Key
    byte key[] = password.getBytes();
    DESKeySpec desKeySpec = new DESKeySpec(key);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

    // Create Cipher
    Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
    desCipher.init(Cipher.DECRYPT_MODE, secretKey);

    // Create crypto stream
    BufferedInputStream bis = new BufferedInputStream(istream);
    CipherInputStream cis = new CipherInputStream(bis, desCipher);
    ObjectInputStream ois = new ObjectInputStream(cis);

    List<Entry> entries = new ArrayList<Entry>();
    int count = ois.readInt();
    for(int i=0;i<count;i++) {
        Entry e = (Entry)ois.readObject();
         entries.add(e);
    }
    return entries;
    } catch (Exception e) {throw new HTTPException(e);}
}



}
