/* 
 * CVS identifier:
 * 
 * $Id: ImgMouseListener.java,v 1.7 2000/09/21 16:12:42 dsanta Exp $
 * 
 * Class:                   ImgMouseListener
 * 
 * Description:             Handles the mouse events for scrolling an image
 *                          displayed in an ImgScrollPane.
 * 
 * 
 * 
 * COPYRIGHT:
 * 
 * This software module was originally developed by Raphal Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askelf (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, Flix Henry, Gerard Mozelle and Patrice Onno (Canon Research
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
 * 
 * 
 * 
 */
package ucar.jpeg.jj2000.disp;

import ucar.jpeg.jj2000.j2k.decoder.*;

import java.awt.event.*;
import java.awt.*;

/**
 * This class handles the dragging of an image displayed in an
 * ImgScrollPane. When the mouse is dragged the image scrolls accordingly.
 *
 * <P>Objects of this class must be registerd as both mouse listener and mouse 
 * motion listener.
 *
 * <P>While the dragging is taking place the cursor is changed to the
 * MOVE_CURSOR type. The original cursor is restored when the mouse is
 * released after the drag.
 *
 * */
public class ImgMouseListener extends MouseAdapter 
    implements MouseMotionListener {
    /** The component where the image is displayed */
    ImgScrollPane isp;

    /** The horizontal coordinate where the drag starts */
    int startMouseX;

    /** The vertical coordinate where the drag starts */
    int startMouseY;

    /** The horizontal scroll position when the drag started */
    int startScrollX;

    /** The vertical scroll position when the drag started */
    int startScrollY;

    Cursor prevCursor;

    /**
     * Instantiate a new ImgMouseListener that will work on the specified
     * ImgScrollPane.
     *
     * @param isp The image scroll pane on which the actions should operate.
     * */
    public ImgMouseListener(ImgScrollPane isp) {
        this.isp = isp;
    }

    public void mousePressed(MouseEvent e) {
        // Get the possibly start drag position
        startMouseX = e.getX();
        startMouseY = e.getY();
        // Get the start scroll position
        startScrollX = isp.getHAdjustable().getValue();
        startScrollY = isp.getVAdjustable().getValue();
    }

    public void mouseReleased(MouseEvent e) {
        // Restore the last cursor, if any
        if (prevCursor != null) {
            isp.setCursor(prevCursor);
            prevCursor = null;
        }
    }

    public void mouseDragged(MouseEvent evt) {
        int scrollX,scrollY;

        // Set the drag cursor
        if (prevCursor == null) {
            prevCursor = isp.getCursor();
            isp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        // Calculate new scroll position and set it
        scrollX = startScrollX + startMouseX - evt.getX();
        scrollY = startScrollY + startMouseY - evt.getY();
        isp.setScrollPosition(scrollX,scrollY);
    }

    public void mouseMoved(MouseEvent evt) {
    }
}
