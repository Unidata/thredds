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

package ucar.httpservices;

import org.apache.http.annotation.ThreadSafe;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized management of the connections used by HTTPSession/HTTPMethod.
 * Reason is to try to control thread safety.
 */

/*package scope*/
@ThreadSafe
abstract class HTTPConnections
{
    //////////////////////////////////////////////////
    // Constants

    static final int DFALTMAXCONNS = 10;

    //////////////////////////////////////////////////
    // Shared Instance variables

    /**
     * Determine wether to use a Pooling connection manager
     * or to manage a bunch of individual connections.
     */
    protected boolean closed = false;
    protected boolean pooling = false;
    protected int maxconnections = DFALTMAXCONNS;
    protected int actualconnections = 0;

    protected Registry<ConnectionSocketFactory> protocolregistry = null;
    // Mirror contents of Registry
    protected Map<String, ConnectionSocketFactory> protocols = new HashMap<>();

    protected HTTPConnections(boolean pooling)
    {
        this.pooling = pooling;
        // define at least "http:"
        addProtocol("http", PlainConnectionSocketFactory.getSocketFactory());
    }

    public void addProtocol(String proto, ConnectionSocketFactory factory)
    {
        protocols.put(proto, factory); // don't care if this overwrites

    }

    public void setMaxConnections(int n)
    {
        if(n > 0) {
            synchronized (this) {
                this.maxconnections = n;
            }
        }
    }


    public void setClientManager(HttpClientBuilder cb, HTTPMethod method)
    {
        HttpClientConnectionManager mgr = newManager(method);
        cb.setConnectionManager(mgr);
    }


    protected Registry<ConnectionSocketFactory>
    getRegistry()
    {
        if(this.protocolregistry == null) {
            RegistryBuilder rb = RegistryBuilder.<ConnectionSocketFactory>create();
            for(HashMap.Entry<String, ConnectionSocketFactory> entry : protocols.entrySet()) {
                rb.register(entry.getKey(), entry.getValue());
            }
            this.protocolregistry = rb.build();
        }
        return this.protocolregistry;
    }

    public void validate()
    {
        assert actualconnections == 0;
    }

    abstract public HttpClientConnectionManager newManager(HTTPMethod m);

    abstract public void freeManager(HTTPMethod method);

    abstract public void close();
}


/**
 * Subclass of HTTPConnections that uses multiple instances of
 * BasicHttpClientConnection
 */

/*package scope*/
@ThreadSafe
class HTTPConnectionSimple extends HTTPConnections
{
    //////////////////////////////////////////////////
    // Instance variables

    // Keep bi-directional maps
    protected Map<HTTPMethod, HttpClientConnectionManager> methodmap = null;
    protected Map<HttpClientConnectionManager, HTTPMethod> mgrmap = null;

    public HTTPConnectionSimple()
    {
        super(false);
        this.mgrmap = new HashMap<>();
        this.methodmap = new HashMap<>();
    }


    @Override
    public HttpClientConnectionManager newManager(HTTPMethod method)
    {
        BasicHttpClientConnectionManager mgr = null;
        for(; ; ) {
            boolean await = true;
            synchronized (this) {
                if(this.actualconnections < this.maxconnections) {
                    HttpClientConnectionManager old = methodmap.get(method);
                    if(old != null)
                        throw new IllegalStateException("Method has previous Connection Manager");
                    mgr = new BasicHttpClientConnectionManager(getRegistry());
                    methodmap.put(method, mgr);
                    mgrmap.put(mgr, method);
                    this.actualconnections++;
                    await = false;
                }
            }
            if(!await)
                break;
            // wait outside the synchronized code
            try {
                Thread.sleep(1 * 1000);
            } catch (InterruptedException e) {
                mgr = null;
                break;
            }
        }
        return mgr;
    }

    @Override
    public void freeManager(HTTPMethod method)
    {
        synchronized (this) {
            HttpClientConnectionManager mgr = methodmap.get(method);
            if(mgr == null)
                throw new IllegalStateException();
            mgrmap.remove(mgr, method);
            methodmap.remove(method);
            ((BasicHttpClientConnectionManager) mgr).close();
            actualconnections--;
        }
    }

    @Override
    public void close()
    {
        synchronized (this) {
            if(this.closed) return;
            this.closed = true;
            for(HTTPMethod m : methodmap.keySet()) {
                freeManager(m);
            }
            assert methodmap.size() == 0;
            assert mgrmap.size() == 0;
        }
    }

    @Override
    public void validate()
    {
        assert methodmap.size() == 0;
        assert mgrmap.size() == 0;
        super.validate();
    }

}


/**
 * Subclass of HTTPConnections that uses
 * PoolingHttpClientConectionManager.
 */

/*packagescope*/
class HTTPConnectionPool extends HTTPConnections
{
    //////////////////////////////////////////////////
    // Instance variables

    protected PoolingHttpClientConnectionManager poolmgr = null;

    public HTTPConnectionPool()
    {
        super(true);
    }

    protected PoolingHttpClientConnectionManager
    getPool()
    {
        if(poolmgr == null) {
            this.poolmgr = new PoolingHttpClientConnectionManager(getRegistry());
            setMaxConnections(this.maxconnections);
        }
        return this.poolmgr;
    }

    @Override
    public HttpClientConnectionManager newManager(HTTPMethod session)
    {
        synchronized (this) {
            this.actualconnections++;
            return getPool();
        }
    }

    public void freeManager(HTTPMethod m)
    {
        this.actualconnections--;
    }

    @Override
    public void close()
    {
        synchronized (this) {
            poolmgr.shutdown();
            poolmgr = null;
        }
    }

    @Override
    public void setMaxConnections(int n)
    {
        if(n > 0) {
            super.setMaxConnections(n);
            synchronized (this) {
                if(poolmgr != null) {
                    poolmgr.setDefaultMaxPerRoute(n);
                    poolmgr.setMaxTotal(n);
                }
            }
        }
    }


}
