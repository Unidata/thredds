/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

/*
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.http.auth.*;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import thredds.featurecollection.CollectionUpdater;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
*/

import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Test;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPSession;
import ucar.nc2.util.log.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Test version of THREDDS Data Manager.
 * Currently only manages GRIB Collection indices.
 *
 * @author caron
 * @since 12/13/13
 */
public class TestTdm
{
    protected static org.slf4j.Logger tdmLogger = org.slf4j.LoggerFactory.getLogger(TestTdm.class);
    protected static final boolean debug = false;
    protected static final boolean debugOpenFiles = false;
    protected static final boolean debugTasks = true;

    protected Path contentDir;
    protected Path contentThreddsDir;
    protected Path contentTdmDir;
    protected Path threddsConfig;

    protected String user, pass;
    protected boolean sendTriggers;
    protected String[] serverNames;
    protected List<Server> servers;

    protected boolean showOnly = false; // if true, just show dirs and exit

    protected String loglevel;
    protected boolean forceOnStartup = false; // if true, just show dirs and exit

    LoggerFactory loggerFactory;

    protected static class Server
    {
        String name;
        HTTPSession session;

        protected Server(String name, HTTPSession session)
        {
            this.name = name;
            this.session = session;
            System.out.printf("Server added %s%n", name);
            tdmLogger.info("TDS server added " + name);
        }
    }

    public void setContentDir(String contentDir) throws IOException
    {
        System.out.printf("contentDir=%s%n", contentDir);
        this.contentDir = Paths.get(contentDir);
        this.contentThreddsDir = Paths.get(contentDir, "thredds");
        this.threddsConfig = Paths.get(contentDir, "thredds", "threddsConfig.xml");
        this.contentTdmDir = Paths.get(contentDir, "tdm");
    }

    public void setShowOnly(boolean showOnly)
    {
        this.showOnly = showOnly;
    }

    public void setNThreads(int n)
    {
        tdmLogger.info(" TDM nthreads= {}", n);
    }

    public void setForceOnStartup(boolean forceOnStartup)
    {
        this.forceOnStartup = forceOnStartup;
    }

    public void setLoglevel(String loglevel)
    {
        this.loglevel = loglevel;
    }

    // spring beaned

    public void setServerNames(String[] serverNames)
    {
        this.serverNames = serverNames;
    }

    public void initServers() throws HTTPException
    {
        if(serverNames == null) {
            servers = new ArrayList<>(); // empty list
            return;
        }
        this.servers = new ArrayList<>(this.serverNames.length);
        for(String name : this.serverNames) {
            try (HTTPSession session = HTTPFactory.newSession(name)) {
                if(user != null && pass != null)
                    session.setCredentials(new UsernamePasswordCredentials(user, pass));
                session.setUserAgent("TDM");
                servers.add(new Server(name, session));
            }
        }
    }

    protected String makeTriggerUrl(String name)
    {
        return "thredds/admin/collection/trigger?trigger=never&collection=" + name;
    }


    @Test
    public void
    testTdm()
            throws IOException, InterruptedException
    {
        HTTPSession.setGlobalUserAgent("TDM v5.0");
        String userpwd = System.getProperty("userpwd");
        if(userpwd == null || userpwd.length() == 0)
            throw new IllegalStateException("No userpwd specified");
        String[] pieces = userpwd.split("[:]");
        this.user = pieces[0];
        this.pass = pieces[1];
        this.sendTriggers = true;
        initServers();
    }
}
