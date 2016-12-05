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

import org.junit.Assert;
import org.junit.Test;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

/**
 * Test interaction of multi-threading with httpservices.
 */
public class TestThreading extends UnitTestCommon
{
    //////////////////////////////////////////////////.
    // Constants

    static public final boolean DEBUG = false;

    static public final boolean AWAIT = true;


    static protected final int DFALTTHREADS = 100;
    static protected final int DFALTMAXCONNS = (DFALTTHREADS/2);

    static protected final String DFALTSERVER = "http://" + TestDir.dap2TestServer;

    static protected final String DFALTURLFMT = DFALTSERVER + "/dts/test.%02d";

    static {
        HTTPSession.TESTING = true;
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected String[] testurls;

    protected int nthreads = DFALTTHREADS;

    //////////////////////////////////////////////////

    public TestThreading()
    {
        setTitle("HTTP Threading tests");
        String sn = System.getProperty("nthreads");
        if(sn != null) {
            try {
                this.nthreads = Integer.parseInt(sn);
                if(this.nthreads <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.err.println("-Dnthreads value must be positive integer: " + sn);
                this.nthreads = DFALTTHREADS;
            }
        }
        definetests();
    }

    protected void
    definetests()
    {
        this.testurls = new String[this.nthreads];
        for(int i = 0; i < this.nthreads; i++) {
            String s = String.format(DFALTURLFMT, i);
            this.testurls[i] = s;
        }
    }

    @Test
    public void
    testThreadingN() throws HTTPException, InterruptedException {
        HTTPSession.setGlobalMaxConnections(DFALTMAXCONNS);
        Thread[] runners = new Thread[this.nthreads];
        try (HTTPSession session = HTTPFactory.newSession(DFALTSERVER)) {
            // Set some timeouts
            //session.setConnectionTimeout(10000);
            //session.setSOTimeout(10000);
            for(int i = 0; i < this.nthreads; i++) {
                if(DEBUG)
                    System.err.printf("[%d]: %s%n", i, testurls[i]);
                runners[i] = new Thread(new Runner(session, testurls[i], i));
                runners[i].start();
            }
            if(AWAIT) {
                Thread.sleep(nthreads * 100);
                boolean[] state = new boolean[this.nthreads];
                for(int i = 0; i < this.nthreads; i++) {
                    Thread t = runners[i];
                    state[i] = t.isAlive();
                }
                if(DEBUG)
                    for(int i = 0; i < this.nthreads; i++) {
                        System.err.printf("[%d]: %s%n", i, (state[i] ? "up" : "down"));
                    }
                for(int i = 0; i < this.nthreads; i++) {
                    Thread t = runners[i];
                    if(t.isAlive()) {
                        System.err.printf("[%d] forced %n");
                        t.interrupt();
                    }
                    t.join();
                }
            } else {
                for(; ; ) {
                    int upcount = 0;
                    for(int i = 0; i < this.nthreads; i++) {
                        Thread t = runners[i];
                        if(t.isAlive())
                            upcount++;
                        else t.join();
                    }
                    if(upcount == 0) break;
                    System.err.println("upcount=" + upcount);
                    System.err.flush();
                    Thread.sleep(1 * 1000);
                }
            }
        }
        System.err.println("All threads terminated");
        HTTPSession.validatestate();
    }

    @Test
    public void
    testThreading1() throws HTTPException {
        Runner[] runners = new Runner[this.nthreads];
        try (HTTPSession session = HTTPFactory.newSession(DFALTSERVER)) {
            // Set some timeouts
            //session.setConnectionTimeout(10000);
            //session.setSOTimeout(10000);
            for(int i = 0; i < this.nthreads; i++) {
                if(DEBUG)
                    System.err.printf("[%d]: %s%n", i, testurls[i]);
                runners[i] = new Runner(session, testurls[i], i);
            }
            for(int i = 0; i < this.nthreads; i++) {
                Runner runner = runners[i];
                runner.run();
            }
        }
        System.err.println("All threads terminated");
        HTTPSession.validatestate();
    }

    static class Runner implements Runnable
    {
        final HTTPSession session;
        final String url;
        final int index;

        public Runner(final HTTPSession session, final String url, int i)
        {
            this.session = session;
            this.url = url;
            this.index = i;
        }

        public void
        run()
        {

            try {
                try (HTTPMethod m = HTTPFactory.Head(session, this.url)) {
                    int status = m.execute();
                    Assert.assertTrue("Bad return code: " + status, (status == 200 || status == 404));
                }
            } catch (Exception e) {
                new Failure("Index: " + index, e).printStackTrace(System.err);
            }
        }
    }

    static public class Failure extends Exception
    {
        Failure(String m, Exception e)
        {
            super(m, e);
        }
    }

}

