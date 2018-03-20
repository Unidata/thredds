/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
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
    static public boolean TRACE = false;

    //////////////////////////////////////////////////
    // Constants

    static int DFALTMAXCONNS = 20;


    static public int setDefaultMaxConections(int n)
    {
        int old = DFALTMAXCONNS;
        if(n > 0) DFALTMAXCONNS = n;
        return old;
    }


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
                    if(TRACE)
                        System.err.println("HTTPConnections: open connection: "+method.hashCode());
                } else if(TRACE)
                    throw new IllegalStateException("HTTPConnections: too many connections");
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
            if(TRACE)
                System.err.println("HTTPConnections: close connection: "+method.hashCode());
        }
    }

    @Override
    public void close()
    {
        synchronized (this) {
            if(this.closed) return;
            this.closed = true;
            if(TRACE)
                System.err.println("HTTPConnections: close with open connections: "+methodmap.size());
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
    public HttpClientConnectionManager newManager(HTTPMethod m)
    {
        synchronized (this) {
            if(TRACE)
                System.err.println("HTTPConnections: open connection: "+m.hashCode());
            this.actualconnections++;
            return getPool();
        }
    }

    public void freeManager(HTTPMethod m)
    {
        if(TRACE)
            System.err.println("HTTPConnections: close connection: "+m.hashCode());
        this.actualconnections--;
        // Do notneed to reclaim anything for pooling manager
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
