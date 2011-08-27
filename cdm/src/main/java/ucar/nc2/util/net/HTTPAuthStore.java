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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.io.*;

import com.sun.org.apache.xerces.internal.parsers.CachingParserPool;
import org.apache.commons.httpclient.auth.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import static ucar.nc2.util.net.HTTPAuthScheme.*;

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
HTTPSession(session) X String(principal) X String(host) X int(port) X String(path) X HTTPAuthScheme
The scheme specifies the kind of authorization
(e.g. basic, keystore, etc) and the info to support it.
*/


static public class Entry implements Serializable, Comparable
{
    private HTTPSession session;
    public String principal;
    public String host;
    public int port;
    public String path;
    public HTTPAuthScheme scheme;

    public Entry()
    {
	this(ANY_SESSION,ANY_PRINCIPAL,ANY_HOST,ANY_PORT,ANY_PATH,null);
    }

    /**
     * @param session
     * @param principal
     * @param host
     * @param port
     * @param path
     * @param scheme
     */
    public Entry(HTTPSession session, String principal, String host, int port, String path, HTTPAuthScheme scheme)
    {
         constructor(session, principal, host, port, path, scheme);
    }

    /**
     * @param entry
     */

    public Entry(Entry entry)
    {
	this(entry.session,
	     entry.principal,
	     entry.host,
	     entry.port,
	     entry.path,
	     entry.scheme);
    }

    /**
     * @param session
     * @param pattern
     * @param scheme
     */

    public Entry(HTTPSession session, String pattern, HTTPAuthScheme scheme)
        throws HTTPException
    {
        URI uri = null;
        try {
            uri = new URI(pattern);
        } catch (URISyntaxException mue) {
            throw new HTTPException(mue);
        }
        if(session == null) session = ANY_SESSION;
        String principal = uri.getUserInfo();
        String host = uri.getHost();        
        int port = uri.getPort();
        String path = uri.getRawPath();
        if(principal == null || principal.equals(PLACEHOLDER))
            principal = ANY_PRINCIPAL;
        if(host == null || host.equals(PLACEHOLDER))
            host = ANY_HOST;
        if(port <= 0)
            port = ANY_PORT;
        if(path == null || path.equals(PLACEHOLDER))
            path = ANY_PATH;
	
	constructor(session,principal,host,port,path,scheme);
    }

    /**
     * Shared constructor code
     * @param session
     * @param principal
     * @param host
     * @param port
     * @param path
     * @param scheme
     */

    protected void constructor(HTTPSession session, String principal, String host, int port, String path, HTTPAuthScheme scheme)
    {
	if(session == null) session = ANY_SESSION;
	if(principal == null) principal = ANY_PRINCIPAL;
	if(host == null) host = ANY_HOST;
	if(port <= 0) port = ANY_PORT;
	if(path == null) path = ANY_PATH;
	if(scheme != null)
            scheme = new HTTPAuthScheme(scheme);
	this.session = session;
        this.principal = principal;
	this.host = host;
	this.port = port;
	this.path = path;
	this.scheme = scheme;
    }

/* NOTUSED
    public Entry duplicate()
    {
	Entry clone = new Entry(this.session,
                                this.principal,
                                this.host,
                                this.port,
                                this.path
                                this.scheme.duplicate());
        return clone;
    }
*/

    public String toString()
    {
	String session = (this.session == ANY_SESSION ? PLACEHOLDER : "this");
	String principal = (this.principal == ANY_PRINCIPAL ? PLACEHOLDER : this.principal);
	String host = (this.host == ANY_HOST ? PLACEHOLDER : this.host);
	String port = (this.port == ANY_PORT ? PLACEHOLDER : ""+this.port);
	String path = (this.path == ANY_PATH ? PLACEHOLDER : this.path);
	String scheme = (this.scheme == null ? "null" : this.scheme.toString());
        return String.format("%s://%s@%s:%s/%s{%s}",session,principal,host,port,path,scheme);
    }

    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
	// Only global sessions are written
        if(this.session != ANY_SESSION) return;
        oos.writeObject(this.principal);
        oos.writeObject(this.host);
        oos.writeInt(this.port);
        oos.writeObject(this.path);
        oos.writeObject(this.path);
        oos.writeObject(this.scheme);
    }

    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
    {
	this.session = ANY_SESSION;
        this.principal = (String)ois.readObject();
        this.host = (String)ois.readObject();
        this.port = ois.readInt();
        this.path = (String)ois.readObject();
        this.scheme = (HTTPAuthScheme)ois.readObject();
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

    public boolean equals(Entry e1) {return (compareTo(e1)==0);}

    public int compareTo(Object e1)
    {
        if(e1 instanceof Entry)
            return compare(this,(Entry)e1);
        else return +1;
    }

    static protected int compare(Entry e1, Entry e2)
  {
      if(e1.session != e2.session) {
          if(e1.session == ANY_SESSION) return 1;
          if(e2.session == ANY_SESSION) return -1;
      }
      if(!e1.principal.equals(e2.principal)) {
          if(e1.principal == ANY_PRINCIPAL) return 1;
          if(e2.principal == ANY_PRINCIPAL) return -1;
      }
      if(!e1.host.equals(e2.host)) {
          if(e1.host == ANY_HOST) return 1;
          if(e2.host == ANY_HOST) return -1;
      }
      if(e1.port != e2.port) {
          if(e1.port == ANY_PORT) return 1;
          if(e2.port == ANY_PORT) return -1;
      }
      if(!e1.path.equals(e2.path)) {
          if(e1.path == ANY_PATH) return 1;
          if(e2.path == ANY_PATH) return -1;
          int cmp = e1.path.compareTo(e2.path);
          if(cmp != 0) return cmp;
      }
      return 0;
  }

}

//////////////////////////////////////////////////

static public final HTTPSession     ANY_SESSION = new HTTPSession();
static public final HTTPSession     GLOBAL_SESSION = ANY_SESSION; // alias
static public final String         ANY_PRINCIPAL = "";
static public final String         ANY_HOST = "";
static public final int            ANY_PORT = -1;
static public final String         ANY_PATH = "";
//static public final HTTPAuthScheme ANY_SCHEME = new HTTPAuthScheme(HTTPAuthScheme.NULL);

static public final String PLACEHOLDER = "_";

static private Hashtable<HTTPSession, List<Entry>> rows;

static {
    rows = new Hashtable<HTTPSession,List<Entry>>();
    // For back compatibility, check some system properties
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
            HTTPAuthScheme scheme = new HTTPAuthScheme(Scheme.KEYSTORE);
            scheme.setKeyStore(kpath,kpwd,tpath,tpwd);
            insert(ANY_SESSION,ANY_PRINCIPAL,ANY_HOST,ANY_PORT,ANY_PATH,scheme);
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

    List<Entry> list = rows.get(entry.principal);
    if(list == null) {
	list = new ArrayList<Entry>();
	rows.put(entry.session,list);
    }
    Entry found = null;
    for(Entry e: list) {
	if(entry.session == e.session
	   && entry.host.equals(e.host)
	   && entry.port == e.port
	   && entry.path.equals(e.path)) {
	    found = e;
	    break;
	}
    }
    // If the entry already exists, then overwrite it and return true
    if(found != null) {
        found.scheme = new HTTPAuthScheme(entry.scheme);
	rval = true;
    } else {
        Entry newentry = new Entry(entry);
        list.add(newentry);
    }

    return rval;
}

/**
 * @param session
 * @param principal
 * @param host
 * @param port
 * @param path
 * @param scheme
 * @return true if entry already existed and was replaced
 */

static synchronized public boolean
insert(HTTPSession session,
       String principal,
       String host,
       int port,
       String path,
       HTTPAuthScheme scheme)
{
    return insert(new Entry(session,principal,host,port,path,scheme));
}


/**
 * @param session
 * @param pattern
 * @param scheme
 * @return true if entry already existed and was replaced
 */

static synchronized public boolean
insert(HTTPSession session, String pattern, HTTPAuthScheme scheme)
    throws HTTPException
{
    return insert(new Entry(session,pattern,scheme));
}

/**
 * @param entry
 * @return true if entry existed and was removed
 */

static synchronized public boolean
remove(Entry entry)
{
    boolean rval = true;

    List<Entry> list = (entry.principal==ANY_PRINCIPAL?getAllRows():rows.get(entry.principal));
    if(list == null) rval = false;
    else {
        Entry found = null;
        for(Entry e: list) {
	    if(entry == e
               || (entry.host.equals(e.host)
                   && entry.port == e.port
                   && entry.path.equals(entry.path))) {
                found = e;
                break;
            }
        }
        if(found != null) rval = list.remove(found);
    }
    return rval;
}

/**
 * @param session
 * @param principal
 * @param host
 * @param port
 * @param path
 * @return true if entry existed and was removed
 */

static synchronized public boolean
remove(HTTPSession session,
       String principal,
       String host,
       int port,
       String path,
       HTTPAuthScheme scheme)
    throws HTTPException
{
    return insert(new Entry(session,principal,host,port,path,scheme));
}


/**
 *
 * @param session
 * @param pattern
 * @param scheme
 * @return true if entry existed and was removed
 */

static synchronized public boolean
remote(HTTPSession session, String pattern, HTTPAuthScheme scheme)
    throws HTTPException
{
    return remove(new Entry(session,pattern,scheme));
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
 * The search pattern is defined by the following sub-patterns:
 * 1. Session - comparison is by ==; wildcard is specified by ANY_SESSION
 * 2. Principal - comparison is by equal function; wildcard is specified by ANY_PRINCIPAL
 * 3. Host - comparison is by equal function; wildcard is specified by ANY_HOST
 * 4. Port - comparison is by equal function; wildcard is specified by ANY_PORT
 * 5. Path - this represents some prefix of the url path. This allows specifying
 *    authorization on a specific url substree.
 *    Comparison is by lexical order; wildcard is specified by ANY_PATH
 * 
 * The return list is ordered from most restrictive to least restrictive.
 * 
 * @param entry
 * @return list of matching entries
 */
static synchronized public Entry[]
search(Entry entry)
{
    List<Entry> list = null;

    // Search will actually look at both per-session and global session entries.
    // The final result will have all globals after all per-session entries.
    assert(entry.session != null);
    if(entry.session == ANY_SESSION)
        list = getAllRows();
    else {
	list = rows.get(entry.session);
	if(list != null)
            list.addAll(rows.get(ANY_SESSION));
        else
            list = rows.get(ANY_SESSION);
    }
    List<Entry> matches = new ArrayList<Entry>();
    if(list != null) {
        for(Entry e: list) {
            if(entry.session != ANY_SESSION
               && e.session != ANY_SESSION
               && entry.session != e.session) continue;
            if(!entry.principal.equals(ANY_PRINCIPAL)
               && !e.principal.equals(ANY_PRINCIPAL)
               && !entry.principal.equals(e.principal)) continue;
            if(!entry.host.equals(ANY_HOST)
               && !e.host.equals(ANY_HOST)
               && !entry.host.equals(e.host)) continue;
            if(entry.port != ANY_PORT
               && e.port != ANY_PORT
               && entry.port != e.port) continue;
            if(!entry.path.equals(ANY_PATH)
               && !e.path.equals(ANY_PATH)
               && entry.path.compareTo(e.path) >= 0) continue;
            matches.add(new Entry(e));
        }
    }
    // Sort based on the comparison function below
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
 
static synchronized public Entry[]
search(HTTPSession session, String principal, String host, int port, String path)
{
    return search(new Entry(session,principal,host,port,path,null));
}

/**
 * @param session
 * @param pattern
 * @return list of matching entries
 */
static synchronized public Entry[]
search(HTTPSession session, String pattern)
    throws HTTPException
{
    return search(new Entry(session,pattern,null));
}

/**
 * @param session
 * @param pattern
 * @return list of matching entries
 */
static synchronized public Entry[]
search(HTTPSession session, String principal, String pattern)
    throws HTTPException
{
    return search(new Entry(session,pattern,null));
}

//////////////////////////////////////////////////
// Misc.


static public AuthScope
getAuthScope(Entry entry)
{
    if(entry == null) return null;
    String host = entry.host;
    int port = entry.port;
    String realm = entry.path;
    String scheme = (entry.scheme == null ? null : entry.scheme.getSchemeName());

    if(host == ANY_HOST) host = AuthScope.ANY_HOST;
    if(port == ANY_PORT) port = AuthScope.ANY_PORT;
    if(realm == ANY_PATH) realm = AuthScope.ANY_REALM;
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
