/*
 * CVS identifier:
 *
 * $Id: SimpleAppletDecoder.java,v 1.2 2000/12/04 17:22:04 grosbois Exp $
 *
 * Class:                   SimpleAppletDecoder
 *
 * Description:             A very simple applet that embeds a decoder in a
 *                          web page.
 *
 *
 *
 * COPYRIGHT:
 * 
 * This software module was originally developed by Rapha?l Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askel?f (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, F?lix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 * 
 * Copyright (c) 1999/2000 JJ2000 Partners.
 *  */
package ucar.jpeg.jj2000.j2k.decoder;

import ucar.jpeg.jj2000.j2k.util.*;
import ucar.jpeg.jj2000.j2k.*;
import ucar.jpeg.jj2000.disp.*;

import java.applet.*;
import java.awt.*;
import java.awt.image.*;
import java.net.*;

/**
 * This applet implements a simple mean to embed the JJ2000 decoder in an HTML
 * page. The applet accepts all the parameters of the JJ2000 decoder, except
 * output files. The image is downloaded, decoded and shown in the applet
 * area. Scrollbars appear if the image is larger than the applet area.
 * 
 * <P>An example use could be:<BR><BR><tt>
 *
 * &lt;applet code="jj2000.j2k.decoder.SimpleAppletDecoder.class"<BR>
 * &nbsp;archive="jj2000-aplt.jar"<BR>
 * &nbsp;width="512" height="512"&gt;<BR>
 * &nbsp;&nbsp;&nbsp;&lt;param name="i" value="tools.jp2"&gt;<BR>
 * &nbsp;&nbsp;&nbsp;&lt;param name="res" value"3"&gt;<BR>
 * &lt;/applet&gt;</tt><BR>
 *
 * */
public class SimpleAppletDecoder extends Applet implements Runnable {

    /** The parameter info, with all possible options. */
    private static String pinfo[][] = Decoder.getAllParameters();

    /** The decoder */
    private Decoder dec;

    /** The parameter list to give to the decoder */
    private ParameterList pl;

    /** The thread where the decoder is run */
    private Thread decThread;

    /** If the decoder thread has already been started */
    private boolean decStarted;

    /** Where the image is displayed */
    private ImgScrollPane isp;

    /**
     * Initializes the applet. It reads the parameters and creates the decoder 
     * thread.
     * */
    public void init() {
        ParameterList defpl;
        String param;
        int i;
        URL input;

        // Get the dfault parameter values
        defpl = new ParameterList();
	for (i=pinfo.length-1; i>=0; i--) {
	    if(pinfo[i][3]!=null)
		defpl.put(pinfo[i][0],pinfo[i][3]);
        }

        // Get all parameters from and put them in a parameter list
        pl = new ParameterList(defpl);
        for (i=0; i<pinfo.length; i++) {
            param = getParameter(pinfo[i][0]);
            if (param != null) {
                pl.put(pinfo[i][0],param);
            }
        }
        // Add base to relative URL
        param = pl.getParameter("i");
        if (param != null) {
            try {
                input = new URL(getDocumentBase(),param);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Malformed URL in "+
                                                   "parameter 'i'");
            }
            param = input.toExternalForm();
            pl.put("i",param);
        }
        else {
            throw new IllegalArgumentException("Missing input");
        }
        if (pl.getParameter("o") != null) {
            throw new IllegalArgumentException("Can not specify output files "+
                                               "for applet");
        }
        // Set the layout to something without gaps
        setLayout(new BorderLayout());
        // Set the im,age scroll pane to display the image
        isp = new ImgScrollPane(ImgScrollPane.SCROLLBARS_AS_NEEDED);
        add(isp,BorderLayout.CENTER);
        validate();
        setVisible(true);
        // Set a thread to run the applet
        decThread = new Thread(this);
        decStarted = false;
    }

    /**
     * Starts the decoding thread.
     * */
    public void start() {
        if (decStarted) return; // already started => nothing to do
        decStarted = true;
        decThread.start();
    }

    /**
     * Does nothing, since the decoder thread can not be stopped.
     * */
    public void stop() {
        // Nothing we can do, we can not stop the decoding thread
    }

    /**
     * Waits for the decoder thread to end and sets it to null.
     * */
    public void destroy() {
        if (!decStarted) return;
        while (decThread != null && decThread.isAlive()) {
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException e) {
            }
        }
        decThread = null;
    }

    /**
     * The run method for the decoding thread. Instantiates the decoder, runs
     * it and updates the status message.
     * */
    public void run() {
        ImgMouseListener iml;
        Image img;
        int status;

        if (dec != null) return; // already running a decoder!
        showStatus("Initializing JJ2000 decoder...");
        // Create and start the decoder
        dec = new Decoder(pl,isp);
        dec.setChildProcess(true);
        showStatus("Decoding...");
        dec.run();
        // Check decoding status
        img = isp.getImage();
        do {
            status = isp.checkImage(img,null);
            if ((status & ImageObserver.ERROR) != 0) {
                showStatus("An unknown error occurred while "+
                           "producing the image");
                return;
            }
            else if ((status & ImageObserver.ABORT) != 0) {
                showStatus("Image production was aborted for "+
                           "some unknown reason");
            }
            else if ((status & ImageObserver.ALLBITS) != 0) {
                showStatus("Done."); 
            }
            else { // Check again in 100 ms
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) {
                }
            }
        } while ((status &
                  (ImageObserver.ALLBITS|
                   ImageObserver.ABORT|
                   ImageObserver.ERROR)) == 0);
        // Bind mouse and key event handlers
        if ((status & ImageObserver.ERROR) == 0) {
            iml = new ImgMouseListener(isp);
            isp.addKeyListener(new ImgKeyListener(isp,dec));
            isp.addMouseListener(iml);
            isp.addMouseMotionListener(iml);
        }
    }

    /**
     * Returns the applet information (version, copyright, etc.)
     *
     * @return A multi-line string containing the applet name, version,
     * copyright and bug reporting address.
     * */
    public String getAppletInfo() {
        return "JJ2000's JPEG 2000 simple applet decoder\n"+
            "Version: "+JJ2KInfo.version+"\n"+
            "Copyright:\n\n"+JJ2KInfo.copyright+"\n"+
            "Send bug reports to: "+JJ2KInfo.bugaddr+"\n";
    }

    /**
     * Returns the applet parameter information. See Applet class.
     *
     * @see Applet#getParameterInfo
     * */
    public String[][] getParameterInfo() {
        return pinfo;
    }
}
