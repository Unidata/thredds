/* 
 * CVS identifier:
 * 
 * $Id: TitleUpdater.java,v 1.8 2002/06/24 14:04:58 grosbois Exp $
 * 
 * Class:                   TitleUpdater
 * 
 * Description:             Thread to update display window title
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
 * */
package ucar.jpeg.jj2000.disp;

import java.awt.*;

/**
 * This class should run as an independent thread to update tha main
 * display window title with current image scroll position and
 * zoom.
 * */
public class TitleUpdater implements Runnable {

    /** The update perion, in milliseconds: 100 */
    static final int UPDATE_T = 100;

    /** The ImgScrollPane where the image is displayed */
    ImgScrollPane isp;

    /** The frame which title to update */
    Frame win;

    /** The base of the title */
    String btitle;

    /** The thread exits when this is true */
    volatile public boolean done = false;

    /**
     * Instantiates the title updater. The title of 'win' will be
     * regularly updated with the current zoom and scroll point. The
     * current zoom and scroll point are added to the basic title
     * given by 'btitle'.
     *
     * @param isp Where the image is displayed
     *
     * @param win The window which title to update
     *
     * @param btitle The base of the title.
     * */
    public TitleUpdater(ImgScrollPane isp, Frame win, String btitle) {
        this.isp = isp;
        this.win = win;
        this.btitle = btitle;
    }

    /**
     * The method that executes this thread. The method periodically
     * updates the title, if necessary, and puts to sleep the thread
     * for 100 msec. This method never returns. If the sleep of the
     * thread is interrupted, the title will be updated earlier and
     * the cycle will continue.
     * */
    public void run() {
        // Periodically update the window title
        Point lsp,sp;
        float lzf,zf;
        lsp = isp.getScrollPosition();
        lzf = isp.getZoom();
        while (!done) {
            sp = isp.getScrollPosition();
            zf = isp.getZoom();
            // Update title only if necessary
            if (zf != lzf || !sp.equals(lsp)) {
                win.setTitle(btitle+
                             " @ ("+(int)(sp.x/zf)+","+
                             (int)(sp.y/zf)+") : "+isp.getZoom());
            }
            lsp = sp;
            lzf = zf;
            try {
                Thread.currentThread().sleep(UPDATE_T);
            }
            catch (InterruptedException e) {
            }
        }
    }
}
