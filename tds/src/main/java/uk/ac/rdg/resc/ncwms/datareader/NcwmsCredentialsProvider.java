/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.log4j.Logger;
import ucar.nc2.util.net.HttpClientManager;

/**
 * Handles authentication with OPeNDAP servers.  This object is created by
 * the Spring framework and is then injected into the
 * {@link uk.ac.rdg.resc.ncwms.metadata.MetadataLoader MetadataLoader}.
 * The {@link uk.ac.rdg.resc.ncwms.metadata.MetadataLoader MetadataLoader}
 * then looks for usernames and passwords in OPeNDAP
 * URLs, then calls {@link #addCredentials} when it finds them.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NcwmsCredentialsProvider implements CredentialsProvider
{
    private static final Logger logger = Logger.getLogger(NcwmsCredentialsProvider.class);
    
    // Maps "host:port" to a Credentials object
    private Map<String, Credentials> creds = new HashMap<String, Credentials>();
    
    /**
     * Called by the Spring framework.  Registers this class with
     * the NetCDF library
     */
    public void init()
    {
        HttpClientManager.init(this, null);
        logger.debug("NcwmsCredentialsProvider initialized");
    }
    
    public void addCredentials(String host, int port, String usernamePassword)
    {
        logger.debug("Adding credentials for {}:{} - {}", new Object[]{host, port, usernamePassword});
        this.creds.put(host + ":" + port, new UsernamePasswordCredentials(usernamePassword));
    }

    public Credentials getCredentials(AuthScheme authScheme,
        String host, int port, boolean proxy) throws CredentialsNotAvailableException
    {
        Credentials cred = this.creds.get(host + ":" + port);
        if (cred == null)
        {
            logger.debug("No credentials available for ({},{})", host, port);
            throw new CredentialsNotAvailableException();
        }
        else
        {
            logger.debug("Returning credentials for ({},{})", host, port);
            return cred;
        }
    }
    
}
