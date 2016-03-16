/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.util.net;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.jcip.annotations.Immutable;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Assert;
import org.junit.Test;
import thredds.inventory.CollectionUpdateEvent;
import thredds.inventory.CollectionUpdateType;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.UnitTestCommon;
import ucar.nc2.util.log.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * THREDDS Data Manager.
 * Currently only manages GRIB Collection indices.
 *
 * @author caron
 * @since 12/13/13
 */
public class TestTdm extends UnitTestCommon
{

    static final String[] sources = new String[] {"source1", "source2"};

    @Immutable
    static public class TestEvent
    {
        public final String name;
        public  final String source;

        public TestEvent(String name, String source)
        {
            this.name = name;
            this.source = source;
        }
    }

    private static org.slf4j.Logger tdmLogger = org.slf4j.LoggerFactory.getLogger(TestTdm.class);
    private static org.slf4j.Logger detailLogger = org.slf4j.LoggerFactory.getLogger("tdmDetail");
    private static final boolean debug = false;
    private static final boolean debugOpenFiles = false;
    private static final boolean debugTasks = true;

    private EventBus eventBus;

    private UpdateTester testUpdater;

    private Path contentDir;
    private Path contentThreddsDir;
    private Path contentTdmDir;
    private Path threddsConfig;

    private String user, pass;
    private boolean sendTriggers;
    private String[] serverNames;
    private List<Server> servers;

    private java.util.concurrent.ExecutorService executor;
    private boolean showOnly = false; // if true, just show dirs and exit

    private String loglevel;
    private boolean forceOnStartup = false; // if true, just show dirs and exit

    LoggerFactory loggerFactory;

    private static class Server
    {
        String name;
        HTTPSession session;

        private Server(String name, HTTPSession session)
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
        executor = Executors.newFixedThreadPool(n);
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
    public void setExecutor(ExecutorService executor)
    {
        this.executor = executor;
    }

    void setUpdater(UpdateTester testUpdater, EventBus eventBus)
    {
        this.testUpdater = testUpdater;
        this.eventBus = eventBus;
    }

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
            HTTPSession session = HTTPFactory.newSession(name);
            if(user != null && pass != null)
                session.setCredentials(new UsernamePasswordCredentials(user, pass));
            session.setUserAgent("TDM");
            servers.add(new Server(name, session));
        }
    }

    boolean init() throws HTTPException
    {
        initServers();

        System.setProperty("tds.log.dir", contentTdmDir.toString());
        ;

        return true;
    }

    void start() throws IOException
    {
        System.out.printf("Tdm startup at %s%n", new Date());
        testUpdater.setTdm(true);
        eventBus.register(this);

        for(String source : sources) {

            // now wire for events
            fcMap.put(source, new Listener(source));
            testUpdater.scheduleTasks(source, tdmLogger);
        }

     /* show whats up
     Formatter f = new Formatter();
     f.format("Feature Collections found:%n");
     for (FeatureCollectionConfig fc : fcList) {
       CollectionManager dcm = fc.getDatasetCollectionManager();
       f.format("  %s == %s%n%s%n%n", fc, fc.getClass().getName(), dcm);
     }
     System.out.printf("%s%n", f.toString()); */
    }

    // called by eventBus
    @Subscribe
    public void processEvent(TestEvent event)
    {
        Listener fc = fcMap.get(event.name);
        if(fc == null) {
            tdmLogger.error("Unknown collection name from event bus " + event);
            return;
        }
        fc.processEvent(event);
    }

    Map<String, Listener> fcMap = new HashMap<>();

    // these objects recieve events from quartz schedular via the EventBus
    // one listener for each fc.
    public class Listener
    {
        AtomicBoolean inUse = new AtomicBoolean(false);
        // org.slf4j.Logger logger;
        public String source;

        private Listener(String source)
        {
            this.source = source;
        }

        public void processEvent(TestEvent event)
        {
            executor.execute(new IndexTask(this, event));
        }
    }

    private String makeTriggerUrl(String name)
    {
        return "thredds/admin/collection/trigger?trigger=never&collection=" + name;
    }

    private AtomicInteger indexTaskCount = new AtomicInteger();

    private class IndexTask implements Runnable
    {
        TestEvent event;
        String name;
        Listener liz;

        private IndexTask(Listener liz, TestEvent event)
        {
            this.event = event;
            this.name = event.name;
            this.liz = liz;
        }

        @Override
        public void run()
        {
            try {
                // log.info("Tdm call GribCdmIndex.updateGribCollection "+config.collectionName);
                if(debug)
                    System.out.printf("---------------------%nIndexTask: %s", name);
                long start = System.currentTimeMillis();
                int taskNo = indexTaskCount.getAndIncrement();
                tdmLogger.debug("{} start {}", taskNo, this.name);
		Thread.sleep(3000);
                long took = System.currentTimeMillis() - start;
                tdmLogger.debug("{} done {}: took {} ms", taskNo, this.name, took);
                System.out.printf("%s: %s took %d msecs%n", CalendarDate.present(), this.name, took);

                if(debugTasks) {
                    System.out.printf("executor=%s%n", executor);
                }

                String path = makeTriggerUrl(name);
                sendTriggers(path);
            } catch (Throwable e) {
                tdmLogger.error("Tdm.IndexTask " + name, e);
                e.printStackTrace();

            } finally {
                // tell liz that task is done
                if(!liz.inUse.getAndSet(false))
                    tdmLogger.warn("Listener InUse should have been set");
            }

        }

        private void sendTriggers(String path)
        {
            for(Server server : servers) {
                String url = server.name + path;
                try (HTTPMethod m = HTTPFactory.Get(server.session, url)) {
                    detailLogger.debug("send trigger to {}", url);
                    int status = 200;
                    //status = m.execute();

                    if(status != 200) {
                        tdmLogger.warn("FAIL send trigger to {} status = {}", url, status);
                        detailLogger.warn("FAIL send trigger to {} status = {}", url, status);
                    } else {
                        int taskNo = indexTaskCount.get();
                        tdmLogger.info("{} trigger sent {} status = {}", taskNo, url, status);
                        detailLogger.debug("return from {} status = {}", url, status);
                    }

                } catch (HTTPException e) {
                    Throwable cause = e.getCause();
                    if(cause instanceof ConnectException) {
                        detailLogger.warn("server {} not running", server.name);
                    } else {
                        tdmLogger.error("FAIL send trigger to " + url + " failed", cause);
                        detailLogger.error("FAIL send trigger to " + url + " failed", cause);
                    }
                }
            }
        }

    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
      System.out.printf("usage: <Java> <Java_OPTS> -Dtds.content.root.path=<contentDir> [-catalog <cat>] [-tds <tdsServer>]
      [-cred <user:passwd>] [-showOnly] [-forceOnStartup]%n");
      System.out.printf("example: /opt/jdk/bin/java -Xmx3g -Dtds.content.root.path=/my/content -jar tdm-4.5.jar -tds http://thredds-dev.unidata.ucar.edu/%n");
        // /opt/jdk/bin/java -d64 -Xmx3g -jar -Dtds.content.root.path=/opt/tds-dev/content tdm-4.5.jar -cred tdm:trigger -tds "http://thredds-dev.unidata.ucar.edu/"

     */
    private static class CommandLine
    {
        @Parameter(names = {"-catalog"}, description = "name a specific catalog (reletive to content dir)", required = false)
        public String catalog;

        @Parameter(names = {"-cred"}, description = "tds credentials (user:password)", required = false)
        public String cred;

        @Parameter(names = {"-forceOnStartup"}, description = "force read all collections on startup (override config)", required = false)
        public boolean forceOnStartup;

        @Parameter(names = {"-log"}, description = "log level (debug | info)", required = false)
        public String log;

        @Parameter(names = {"-nthreads"}, description = "number of threads", required = false)
        public int nthreads = 10;

        @Parameter(names = {"-showOnly"}, description = "show collections and exit", required = false)
        public boolean showOnly;

        @Parameter(names = {"-tds"}, description = "list of tds programs to send triggers to", required = false)
        public String tds;

        @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
        public boolean help = false;

        private final JCommander jc;

        public CommandLine(String progName, String[] args) throws ParameterException
        {
            this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
            jc.setProgramName(progName);           // Displayed in the usage information.
        }

        public void printUsage()
        {
            jc.usage();
            System.out.printf("example: /opt/jdk/bin/java -Xmx3g -Dtds.content.root.path=/my/content -jar tdm-5.0.jar -tds http://thredds-dev.unidata.ucar.edu/ -cred tdm:trigger %n");
        }

    }

    @Test
    public void
    testTdm()
    {
        try {
            String args[] = UnitTestCommon.propertiesToArgs("-",
                    "catalog", "cred", "forceOnStartup", "log", "nthreads", "showOnly", "tds");
            TestTdm app = this;
            EventBus eventBus = new EventBus();
            UpdateTester testUpdater = new UpdateTester();
            testUpdater.setEventBus(eventBus);   // Autowiring not working
            app.setUpdater(testUpdater, eventBus);
            String contentDir = System.getProperty("tds.content.root.path");
            if(contentDir == null) contentDir = "../content";
            app.setContentDir(contentDir);

            HTTPSession.setGlobalUserAgent("TDM v5.0");
            String logLevel;
            String progName = TestTdm.class.getName();
            try {
                CommandLine cmdLine = new CommandLine(progName, args);
                if(cmdLine.help) {
                    cmdLine.printUsage();
                    return;
                }
                if(cmdLine.cred != null) {  // LOOK could be http://user:password@server
                    String[] split = cmdLine.cred.split(":");
                    app.user = split[0];
                    app.pass = split[1];
                    app.sendTriggers = true;
                }

                if(cmdLine.forceOnStartup)
                    app.setForceOnStartup(true);

                if(cmdLine.log != null) {
                    app.setLoglevel(cmdLine.log);
                }

                if(cmdLine.nthreads != 0)
                    app.setNThreads(cmdLine.nthreads);

                if(cmdLine.showOnly)
                    app.setShowOnly(true);

                if(cmdLine.tds != null) {
                    if(cmdLine.tds.equalsIgnoreCase("none")) {
                        app.setServerNames(null);
                        app.sendTriggers = false;

                    } else {
                        String[] tdss = cmdLine.tds.split(","); // comma separated
                        app.setServerNames(tdss);
                        app.sendTriggers = true;
                    }
                }

            } catch (ParameterException e) {
                System.err.println(e.getMessage());
                System.err.printf("Try \"%s --help\" for more information.%n", progName);
            }

            if(!app.showOnly && app.pass == null && app.sendTriggers) {
                Scanner scanner = new Scanner(System.in, CDM.UTF8);
                String passw;
                while(true) {
                    System.out.printf("%nEnter password for tds trigger: ");
                    passw = scanner.nextLine();
                    System.out.printf("%nPassword = '%s' OK (Y/N)?", passw);
                    String ok = scanner.nextLine();
                    if(ok.equalsIgnoreCase("Y")) break;
                }
                if(passw != null) {
                    app.pass = passw;
                    app.user = "tdm";
                } else {
                    app.sendTriggers = false;
                }
            }

            if(app.init()) {
                app.start();
            } else {
                System.out.printf("%nEXIT DUE TO ERRORS");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue("Exception", e == null);
        }

    }

}
