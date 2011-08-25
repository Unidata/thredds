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

import java.net.MalformedURIException;
import java.util.*;
import java.io.*;

import org.apache.commons.httpclient.auth.*;
import javax.crypto.*;
import javax.crypto.spec.*;

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

static public enum Scheme {
    NULL,
    ANY,
    BASIC,
    DIGEST,
    KEYSTORE,
    PROXY
}


//////////////////////////////////////////////////
/**
 * The auth store is (conceptually) a set of tuples (rows) of the form
 *     String(principal) X String(uri) X HTTPAuthScheme
 * The scheme indicates the kind of authorization (e.g. basic, keystore, etc).
 * 
 * The primary component of HTTPAuthScheme is (key,value) pair store.
 * The contents of the pair store depends on the particular scheme.
 * As a rule, it will contain either credentials or a credentials provider.
*/


static public class Entry implements Serializable, Comparable
{
    public String principal;
    public String host;
    public int port;
    public String path;
    public HTTPAuthScheme scheme;

    public Entry()
    {
	constructor(ANY_PRINCIPAL,ANY_HOST,ANY_PORT,ANY_PATH,null);
    }

    public Entry(String principal,
                 String host,
                 int port,
		 String path,
		 HTTPAuthScheme scheme)
    {
	constructor(principal,host,port,path,scheme);
    }

    @Urlencoded
    public Entry(String principal, String uriencoded, HTTPAuthScheme scheme)
    {
	URI uri = new URI(uriencoded);
	constructor(principal == null ? uri.getUserInfo() : principal,
                    uri.getHost(),	
                    uri.getPort(),	
                    uri.getRawPath(),
		    scheme);
    }

    @Urlencoded
    public Entry(String uriencoded, HTTPAuthScheme scheme)
    {
	this(null,uri);
    }

    public void constructor(String principal,
                            String host,
                            int port,
		            String path,
		            HTTPAuthScheme scheme)
    {
        this.principal = principal;
	this.host = host;
	this.port = port;
	this.path = path;
	this.scheme = scheme;
    }

    public Entry duplicate()
    {
	Entry clone = new Entry(this.principal,
				this.host,
				this.port,
				this.path,
				this.scheme.duplicate());
        return clone;
    }

    public String toString()
    {
        return String.format("http://%s%s%s/%s %s",
		principal==ANY_PRINCIPAL ? "_@" : principal+"@",
		host==ANY_HOST ? "_" : uri.getHost()
		port > 0 ? ":" + port : ":_",
		path==ANY_PATH ? "/_" : uri.getPath()
		scheme.toString());
    }

    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        oos.writeObject(this.principal);
        oos.writeObject(this.host);
        oos.writeInt(this.port);
        oos.writeObject(this.path);
        oos.writeObject(this.scheme);

    }

    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
    {
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
      * - for realm/realm, longer matches should precede
      *   shorter matches.
      * - the fields are considered in this order:
      * 	principal,host,port,path
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
      int cmp = e1.path.compareTo(e2.path);
      if(cmp != 0) return cmp;
      }
      return 0;
  }

}

private Hashtable<String, List<Entry>> rows
	= new Hashtable<String,List<Entry>>();

static public final HTTPSession ANY_SESSION = null;
static public final String ANY_PRINCIPAL = "";
static public final String ANY_HOST = "";
static public final int ANY_PORT = -1;
static public final String ANY_PATH = "";
static public final HTTPAuthScheme ANY_SCHEME = null;

//////////////////////////////////////////////////
/**
Primary external interface
 */

    /**
     *
     * @param principal
     * @param host
     * @param port
     * @param pat
     * @param scheme
     * @return true if entry already existed and was replaced
     * @throws HTTPException
     */
synchronized protected boolean
insert(String principal, String host, int port, String path, HTTPAuthScheme scheme)
{
    boolean rval = false;

    List<Entry> list = rows.get(principal);
    if(list == null) {
	list = new ArrayList<Entry>();
	rows.put(principal,list);
    }
    Entry found = null;
    for(Entry e: list) {
	if(host.equals(e.host)
	   && port == e.port
	   && path.equals(path)) {
	    found = e;
	    break;
	}
    }
    // If the entry already exists, then overwrite it and return true
    HTTPAuthScheme newscheme = scheme.duplicate();
    newscheme = (HTTPAuthScheme)scheme.duplicate(); // to avoid external modification

    if(found != null) {
        found.scheme = newscheme;
	rval = true;
    } else {
	Entry e = new Entry(principal,host,port,path,newscheme);
        list.add(e);
    }

    return rval;
}

/**
 *
 * @param uri
 * @param scheme
 * @return
 * @throws HTTPException
 */

@Urlencoded
synchronized public boolean
insert(String principal, String uri, HTTPAuthScheme scheme)
    throws HTTPException
{
    URI uri = new URI(uriencoded);
    return insert(principal == null ? uri.getUserInfo() : principal,
                  uri.getHost(),	
                  uri.getPort(),	
                  uri.getRawPath(),
	          scheme);
}

@Urlencoded
synchronized public boolean
insert(String uri, HTTPAuthScheme scheme)
    throws HTTPException
{
    return insert(null,uri,scheme);
}

synchronized public boolean
remove(String principal, String host, int port, String path)
    throws HTTPException
{
    boolean rval = true;

    List<Entry> list = (principal==ANY_PRINCIPAL ? getAllRows()
                                                 : rows.get(principal));
    if(list == null) rval = false;
    else {
        Entry found = null;
        for(Entry e: list) {
            if(host.equals(e.host)
               && port == e.port
               && path.equals(path)) {
                found = e;
                break;
            }
        }
        if(found != null) rval = list.remove(found);
    }
    return rval;
}

/**
 *
 * @throws HTTPException
 */
synchronized public void
clear() throws HTTPException
{
    rows.clear(); 
}

public List<Entry>
getAllRows()
{
    List<Entry> elist = new ArrayList<Entry>();
    for(String key: rows.keySet()) elist.addAll(rows.get(key));
    return elist;
}

/**
Search:

The search pattern is defined by the following sub-patterns:
1. Principal - This is the most important colum in the key
   for the rows in the store. It is an abstraction of the username.
   Comparison is by equal function.
2. Host - comparison is by equal function; wildcard is specified by ANY_HOST
3. Port - comparison is by equal function; wildcard is specified by ANY_HOST
4. path - this represents some prefix of the uri path. This allows specifying
   authorization on a specific uri substree.

The return list is ordered from most restrictive to least restrictive.

 */

@Urlencoded
static synchronized public Entry[]
search(String uri, HTTPAuthScheme scheme)
    throws HTTPException
{
    return search(null,uri,scheme);
}

@Urlencoded
static synchronized public Entry[]
search(String principal, String uri, HTTPAuthScheme scheme)
    throws HTTPException
{
    try {
        URI U = new URI(uri);
        return search(principal==null?U.getUserInfo():principal,
		      U.getHost(),
                      U.getPort(),
                      U.getPath(),
                      scheme);
    } catch (MalformedURIException mue) {
        throw new HTTPException(mue);
    }
}

synchronized public Entry[]
search(String principal, String host, int port, String path,
       HTTPAuthScheme scheme)
    throws HTTPException
{
    List<Entry> list = null;

    if(principal == null) principal = ANY_PRINCIPAL;
    if(host == null) host = ANY_HOST;
    if(port <= 0) port = ANY_PORT;
    if(realm == null) realm = ANY_REALM;
    if(scheme == null) scheme = ANY_SCHEME;

    if(principal == ANY_PRINCIPAL) {
	    list = getAllRows();
    } else { // singleton
	    list = rows.get(principal);
    }

    List<Entry> matches = new ArrayList<Entry>();
    for(Entry e: list) {
	if(!principal.equals(ANY_PRINCIPAL) && !e.principal.equals(ANY_PRINCIPAL) && !principal.equals(e.principal)) continue;
	if(!host.equals(ANY_HOST) && !e.host.equals(ANY_HOST) && !host.equals(e.host)) continue;
	if(port != ANY_PORT && e.port != ANY_PORT && port != e.port) continue;
	if(!path.equals(ANY_PATH) && !e.path.equals(ANY_PATH)
           && path.compareTo(e.path) >= 0)
	    continue;
	    Entry newe = (Entry)e.duplicate();
	    matches.add(newe);
    }
    // Sort based on the comparison function below
    Entry[] matchvec = matches.toArray(new Entry[matches.size()]);
    Arrays.sort(matchvec);
    return matchvec;
}

//////////////////////////////////////////////////
// Misc.

static public AuthScope
getAuthScope(Entry entry)
{
    String host = entry.host;
    int port = entry.port;
    String realm = entry.host;
    String scheme = null;

    if(host == ANY_HOST) host = AuthScope.ANY_HOST;
    if(port == ANY_PORT) port = AuthScope.ANY_PORT;
    if(realm == ANY_REALM) realm = AuthScope.ANY_REALM;
    if(entry.scheme == ANY_SCHEME)
        scheme = null;
    else
	scheme = entry.scheme.getScheme();
    default: break;
    }

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
        List<Entry> hashed = (e.principal == ANY_PRINCIPAL?getAllRows():rows.get(e.principal));
        if(hashed == null) {
            hashed = new ArrayList<Entry>();
            rows.put(e.principal,hashed);
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
