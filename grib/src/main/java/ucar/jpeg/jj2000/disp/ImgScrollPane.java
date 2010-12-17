/* 
 * CVS identifier:
 * 
 * $Id: ImgScrollPane.java,v 1.10 2000/12/04 17:19:27 grosbois Exp $
 * 
 * Class:                   ImgScrollPane
 * 
 * Description:             <short description of class>
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

import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;

/**
 * This class implements an image viewer that can display an image larger than
 * the actual display area, and presents scrollbars to scroll the viewable
 * area. This class also supports zooming in and out the image, with no extra
 * memory requirements.
 *
 * <P>The zoom factor by default is 1. It can be changed with the 'zoom()' and
 * 'setZoom()' methods. The maximum zoom factor is defined by MAX_ZOOM.
 *
 * <P>The zoom scaling is done directly by the AWT display system. In general
 * it is performed by dropping or repeating lines. It is just intended to
 * provide a display zoom and not for proper scaling of an image.
 *
 * <P>The scrolling can be performed by copying the actual displayed data to
 * the new scrolled position and redrawing the damaged parts, or by redrawing
 * the entire displayed image portion at the new scrolled position. Which is
 * more efficient depends on the JVM and working environment. By default it is
 * done by copying since it tends to provide less annoying visual artifacts
 * while scrolling, but that can be changed with 'setCopyScroll()'.
 *
 * <P>This class is very similar to the AWT ScrollPane one, but it is
 * optimized for display of large images and does not suffer from the problems
 * of ScrollPane when changing zoom. The Adjustable elements that represent
 * the scrollbars are made available as in ScrollPane, but the minimum,
 * maximum, visible amount and block increment should not be set
 * (IllegalArgumentException is thrown if attempted), since they are set
 * internally by this class.
 *
 * <P>Focus and key event listeners that are registered are in fact registered
 * with the components that implement the three areas (the image display and
 * the two scrollbars) so that if any such event is fired in any of these
 * areas it is handled by the registered listener.
 *
 * <P>Mouse and mouse movement event listeners that are registered are in fact 
 * registered with the image display component only. The mouse and mouse
 * movement events on the scrollbars are handled by the Scrollbar default
 * listeners only.
 *
 * <P>Although it is implemented as a container, it behaves like a
 * component. Specifically no components can be added or removed from objects
 * of this class. Furthermore, no layout manager can be set. It is internally
 * set and it can not be changed.
 *
 * <P>The implementation uses a lightweight container with an inner class to
 * display the image itself, and two scrollbars. The layout manager is a
 * BorderLayout.
 *
 * <P>This class should be really implemented as a Component, but it is
 * implemented as a Container for easyness. It should not be assumed it is a
 * subclass of Container since in the future it might be rewritten as a
 * subclass of Component only.
 *
 *
 * @see ScrollPane
 *
 * @see Adjustable
 * */
public class ImgScrollPane extends Container {

    /** The ID for always visible scrollbars */
    public final static int SCROLLBARS_ALWAYS = ScrollPane.SCROLLBARS_ALWAYS;

    /** The ID for as needed visible scrollbars */
    public final static int SCROLLBARS_AS_NEEDED =
        ScrollPane.SCROLLBARS_AS_NEEDED;

    /** The ID for never visible scrollbars */
    public final static int SCROLLBARS_NEVER = ScrollPane.SCROLLBARS_NEVER;

    /** The maximum possible zoom factor: 32. */
    // This is used because factors too large cause problems with JVMs.
    public static final float MAX_ZOOM = 32f;

    /** The thickness of the scrollbars: 16 pixels */
    final static int SCROLLBAR_THICKNESS = 16;

    /** The inetrnal gap between the elements, in pixels: 0 */
    final static int INTERNAL_GAP = 0;

    /** The propertion between the visible scrollbar length and the block
     * increment amount: 0.8 */
    final static float BLOCK_INCREMENT_PROPORTION = 0.8f;

    /** The horizontal scrollbar.
     *
     * @serial
     */
    ISPScrollbar hsbar;

    /** The vertical scrollabr.
     *
     * @serial
     */
    ISPScrollbar vsbar;

    /** The image display
     *
     * @serial
     */
    private ImageScrollDisplay imgDisplay;

    /** The scrollbar type (always, as needed, etc.) 
     *
     * @serial
     */
    private int sbType;

    /** The zoom to use in displaying the image. A factor larger than one
     * produces a zoom in effect. 
     *
     * @serial
     */
    private float zoom = 1f;

    /** The zoom used in the last scrollbar calculation.
     *
     * @serial
     */
    private float lastZoom;

    /** The viewable size used in the last scrollbar calculation.
     *
     * @serial
     */
    private Dimension lastSize;

    /** If scrolling is to be done by copying ot not. If not done by copying
     * everything is redrawn. 
     *
     * @serial*/
    private boolean copyScroll = true;

    /**
     * Creates a new ImgScrollPane with SCROLLBARS_AS_NEEDED scrollbars.
     * */
    public ImgScrollPane() {
        this(SCROLLBARS_AS_NEEDED);
    }

    /**
     * Creates a new ImgScrollPane with the specified type of scrollbar
     * visibility.
     *
     * @param svt The scrollbar visibility type
     * */
    public ImgScrollPane(int svt) {
        // Initialize
        super.setLayout(new BorderLayout(INTERNAL_GAP,INTERNAL_GAP));
        sbType = svt;
        hsbar = new ISPScrollbar(Scrollbar.HORIZONTAL,0,1,0,1);
        vsbar = new ISPScrollbar(Scrollbar.VERTICAL,0,1,0,1);
        imgDisplay = new ImageScrollDisplay();
        super.add(hsbar,BorderLayout.SOUTH);
        super.add(vsbar,BorderLayout.EAST);
        super.add(imgDisplay,BorderLayout.CENTER);

        // Set the initial scrollbar visibility
        switch (svt) {
        case SCROLLBARS_NEVER:
        case SCROLLBARS_AS_NEEDED:
            hsbar.setVisible(false);
            vsbar.setVisible(false);
            break;
        case SCROLLBARS_ALWAYS:
            hsbar.setVisible(true);
            vsbar.setVisible(true);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the image to display in this component. If the image is not
     * ready for display it will be prepared in the current thread. The
     * current zoom factor applies.
     *
     * <P>If the image is not ready for display (i.e. it has not been
     * rendered at its natural size) it will be rendered in the current
     * thread, if not being already rendered in another one. This means
     * that the current thread can block until the image is ready.
     *
     * <P>If the image is rendered incrementally (it depends on the
     * underlying 'ImageProducer') it will be displayed in that way if the
     * incremental display is set for the Component class. See the
     * 'imageUpdate()' method of the 'Component' class.
     *
     * <P>If the image is the same as the current one nothing is done.
     *
     * @param img The image to display.
     *
     * @see Component#imageUpdate
     * */
    public void setImage(Image img) {
        imgDisplay.setImage(img);
    }

    /**
     * Returns the image that is displayed in this component.
     *
     * @return The image displayed in this component, or null if none.
     * */
    public synchronized Image getImage() {
        return imgDisplay.img;
    }

    /**
     * Sets the zoom factor to display the image. A zoom factor larger than 1
     * corresponds to a zoom in. A factor of 1 corresponds to no
     * scaling. After setting the zoom factor the component is invalidated and
     * 'repaint()' is automatically called so that the image is redrawn at the
     * new zoom factor. In order to revalidate the layout 'validate()' should
     * be called on one of the parent containers. If the new zoom factor is
     * larger than MAX_ZOOM, then MAX_ZOOM will be used.
     *
     * @param zf The zoom factor
     * */
    public synchronized void setZoom(float zf) {
        if (zf == zoom || (zf > MAX_ZOOM && zoom == MAX_ZOOM)) {
            // No change => do nothing
            return;
        }
        // Set the zoom factor and recalculate the component dimensions
        zoom = zf;
        if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;
        setScrollbars();
        // Check if we need to change the scrollbar display
        if (sbType == SCROLLBARS_AS_NEEDED) doLayout();
        // We need to erase previous scaled image
        imgDisplay.erase = true;
        // Redraw image
        imgDisplay.repaint();
    }

    /**
     * Modifies the current zoom factor by the given multiplier. After setting
     * the zoom factor the component is invalidated and 'repaint()' is
     * automatically called so that the image is redrawn at the new zoom
     * factor. In order to revalidate the layout 'validate()' should be called
     * on one of the parent containers. If the resulting zoom factor is larger
     * than MAX_ZOOM, then MAX_ZOOM will be used.
     *
     * @param zm The zoom multiplier to apply.
     * */
    public synchronized void zoom(float zm) {
        setZoom(zoom*zm);
    }

    /**
     * Returns the current zoom factor.
     *
     * @return The current zoom factor
     * */
    public synchronized float getZoom() {
        return zoom;
    }

    /**
     * Returns the Adjustable object which represents the state of the
     * horizontal scrollbar.
     * */
    public Adjustable getHAdjustable() {
        return hsbar;
    }

    /**
     * Returns the Adjustable object which represents the state of the
     * vertical scrollbar.
     * */
    public Adjustable getVAdjustable() {
        return vsbar;
    }

    /**
     * Returns the display policy for the scrollbars.
     *
     * @return the display policy for the scrollbars
     * */
    public int getScrollbarDisplayPolicy() {
        return sbType;
    }

    /**
     * Sets the display policy for the scrollbars.
     *
     * @param v the display policy for the scrollbars
     * */
    public void setScrollbarDisplayPolicy(int v) {
        // If no change do nothing
        if (v == sbType) return;
        switch (sbType) {
        case SCROLLBARS_NEVER:
        case SCROLLBARS_AS_NEEDED:
            hsbar.setVisible(false);
            vsbar.setVisible(false);
            break;
        case SCROLLBARS_ALWAYS:
            hsbar.setVisible(true);
            vsbar.setVisible(true);
            break;
        default:
            throw new IllegalArgumentException();
        }
        // Now redo the layout
        doLayout();
    }

    /**
     * Scrolls to the specified position within the image. Specifying a
     * position outside of the legal scrolling bounds of the image will scroll
     * to the closest legal position. This is a convenience method which
     * interfaces with the Adjustable objects which represent the state of the
     * scrollbars.
     *
     * @param x the x position to scroll to 
     *
     * @param y the y position to scroll to 
     * */
    public synchronized void setScrollPosition(int x, int y) {
        hsbar.setValueI(x);
        vsbar.setValueI(y);
        // Check if we need to repaint
        x = hsbar.getValue(); // get the actual value for check
        y = vsbar.getValue(); // get the actual value for check
        if (imgDisplay.lastUpdateOffset != null &&
            imgDisplay.lastUpdateOffset.x == x &&
            imgDisplay.lastUpdateOffset.y == y) {
            return; // No change
        }
        // New value changes from last drawn => repaint
        imgDisplay.repaint();
    }

    /**
     * Scrolls to the specified position within the image. Specifying a
     * position outside of the legal scrolling bounds of the image will scroll
     * to the closest legal position. This is a convenience method which
     * interfaces with the Adjustable objects which represent the state of the
     * scrollbars.
     *
     * @param p the position to scroll to 
     * */
    public synchronized void setScrollPosition(Point p) {
        setScrollPosition(p.x,p.y);
    }

    /**
     * Returns the current x,y position within the child which is displayed at
     * the 0,0 location of the scrolled panel's view port. This is a
     * convenience method which interfaces with the adjustable objects which
     * represent the state of the scrollbars.
     *
     * @return the coordinate position for the current scroll position
     * */
    public Point getScrollPosition() {
        return new Point(hsbar.getValue(),vsbar.getValue());
    }

    /**
     * Returns the current size of the image scroll pane's view port. This is
     * the size of the image display area. If this component has not been
     * layed out yet the value is not defined.
     *
     * @return The size of the image display area
     * */
    public Dimension getViewportSize() {
        return imgDisplay.getSize();
    }

    /**
     * Sets if the scrolling is to be done by copying and redrawing of damaged
     * parts of the displayed image. Otherwise it is done by redrawing the
     * entire displayed image. In general copy scrolling is faster and
     * produces less annoying effects. See the class description.
     *
     * @param v If true scrolling will be done by copying.
     * */
    public synchronized void setCopyScroll(boolean v) {
        copyScroll = v;
    }
     
    /**
     * Returns true if the scrolling is done by copying.
     *
     * @return If the copy is done by scrolling
     * */
    public synchronized boolean getCopyScroll() {
        return copyScroll;
    }

    /**
     * Causes this container to lay out its components. Most programs should
     * not call this method directly, but should invoke the validate method
     * instead.
     * */
    public synchronized void doLayout() {
        // Let's see if we should include the scrollbars or not
        if (sbType == SCROLLBARS_AS_NEEDED && imgDisplay.calcDim()) {
            Dimension sz = getSize();
            Dimension imsz = imgDisplay.getPreferredSize();

            if (sz.width>=imsz.width+2*INTERNAL_GAP) {
                if (sz.height>=imsz.height+2*INTERNAL_GAP) {
                    // We don't need scrollbars
                    hsbar.setVisible(false);
                    vsbar.setVisible(false);
                }
                else {
                    // We need at least the vertical one, check again for the
                    // horizontal.
                    vsbar.setVisible(true);
                    if (sz.width >=
                        imsz.width+3*INTERNAL_GAP+SCROLLBAR_THICKNESS) {
                        hsbar.setVisible(false);
                    }
                    else {
                        hsbar.setVisible(true);
                    }
                }
            }
            else {
                // We need at least the horizontal, check for the vertical
                // one.
                hsbar.setVisible(true);
                if (sz.height >=
                    imsz.height+3*INTERNAL_GAP+SCROLLBAR_THICKNESS) {
                    vsbar.setVisible(false);
                }
                else {
                    vsbar.setVisible(true);
                }
            }
        }
        // Indicate that we are erasing the image (the doLayout() will erase)
        imgDisplay.erase = true;
        // Now do the layout
        super.doLayout();
        // Trick the lower scrollbar: if both scrollbars are showing then
        // shorten the horizontal one so that the traditional empty square
        // appears at the lower right corner. This is probably not the best
        // solution but it works.
        if (hsbar.isVisible() && vsbar.isVisible()) {
            Rectangle b = hsbar.getBounds();
            if (b.width > SCROLLBAR_THICKNESS+INTERNAL_GAP) {
                b.width -= SCROLLBAR_THICKNESS+INTERNAL_GAP;
            }
            hsbar.setBounds(b);
        }
        // We need to calculate the scrollbars with the possibly new size
        setScrollbars();
    }

    /**
     * Adds the specified focus listener to receive focus events from this
     * component. It is added to the image and scrollbar areas.
     *
     * @param l the focus listener
     * */
    public synchronized void addFocusListener(FocusListener l) {
        super.addFocusListener(l);
        imgDisplay.addFocusListener(l);
        hsbar.addFocusListener(l);
        vsbar.addFocusListener(l);
    }

    /**
     * Removes the specified focus listener so that it no longer receives
     * focus events from this component.
     *
     * @param l the focus listener
     * */
    public synchronized void removeFocusListener(FocusListener l) {
        super.removeFocusListener(l);
        imgDisplay.removeFocusListener(l);
        hsbar.removeFocusListener(l);
        vsbar.removeFocusListener(l);
    }
        
    /**
     * Adds the specified key listener to receive key events from this
     * component. It is added to the image and scrollbar areas.
     *
     * @param l the key listener
     * */
    public synchronized void addKeyListener(KeyListener l) {
        super.addKeyListener(l);
        imgDisplay.addKeyListener(l);
        hsbar.addKeyListener(l);
        vsbar.addKeyListener(l);
    }

    /**
     * Removes the specified key listener so that it no longer receives key
     * events from this component.
     *
     * @param l the key listener
     * */
    public synchronized void removeKeyListener(KeyListener l) {
        super.removeKeyListener(l);
        imgDisplay.removeKeyListener(l);
        hsbar.removeKeyListener(l);
        vsbar.removeKeyListener(l);
    }

    /**
     * Adds the specified mouse listener to receive mouse events from this
     * component. It is actually added to the image area only and not to the
     * scrollbar areas.
     *
     * @param l the mouse listener
     * */
    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
        imgDisplay.addMouseListener(l);
    }

    /**
     * Removes the specified mouse listener so that it no longer receives
     * mouse events from this component.
     *
     * @param l the mouse listener
     * */
    public synchronized void removeMouseListener(MouseListener l) {
        super.removeMouseListener(l);
        imgDisplay.removeMouseListener(l);
    }

    /**
     * Adds the specified mouse motion listener to receive mouse motion events
     * from this component. It is actually added to the image area only and
     * not to the scrollbar areas.
     *
     * @param l the mouse motion listener
     * */
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        super.addMouseMotionListener(l);
        imgDisplay.addMouseMotionListener(l);
    }

    /**
     * Removes the specified mouse motion listener so that it no longer
     * receives mouse motion events from this component.
     *
     * @param l the mouse motion listener
     * */
    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        super.removeMouseMotionListener(l);
        imgDisplay.removeMouseMotionListener(l);
    }
    /**
     * Sets the background color of this component. It sets the background of
     * the 3 areas (image and scrollbars) plus the container itself.
     *
     * @param c The color to become background color for this component
     * */
    public synchronized void setBackground(Color c) {
        super.setBackground(c);
        imgDisplay.setBackground(c);
        hsbar.setBackground(c);
        vsbar.setBackground(c);
    }

    /**
     * Set the cursor image to a predefined cursor. It sets the cursor of the
     * image area and this container to the specified one. It does not set the 
     * cursor of the scrollbars.
     *
     * @param cursor One of the constants defined by the Cursor class.
     * */
    public synchronized void setCursor(Cursor cursor) {
        super.setCursor(cursor);
        imgDisplay.setCursor(cursor);
    }

    /**
     * Enables or disables this component, depending on the value of the
     * parameter b. An enabled component can respond to user input and
     * generate events. Components are enabled initially by default.
     *
     * @param b If true, this component is enabled; otherwise this component
     * is disabled.
     * */
    public synchronized void setEnabled(boolean b) {
        super.setEnabled(b);
        imgDisplay.setEnabled(b);
        hsbar.setEnabled(b);
        vsbar.setEnabled(b);
    }

    /**
     * Sets the foreground color of this component. It sets the foreground of
     * the 3 areas (image display and scrollbars) plus this contaioner's
     * foreground.
     *
     * @param c The color to become this component's foreground color.
     * */
    public synchronized void setForeground(Color c) {
        super.setForeground(c);
        imgDisplay.setForeground(c);
        hsbar.setForeground(c);
        vsbar.setForeground(c);
    }

   /**
     * Throws an IllegalArgumentException since no components can be added to
     * this container.
     * */
    public Component add(Component comp) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since no components can be added to
     * this container.
     * */
    public Component add(String name,Component comp) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since no components can be added to
     * this container.
     * */
    public Component add(Component comp, int index) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since no components can be added to
     * this container.
     * */
    public void add(Component comp, Object constraints) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since no components can be added to
     * this container.
     * */
    public void add(Component comp, Object constraints, int index) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since the components should never be 
     * removed from this container.
     * */
    public void remove(int index) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since the components should never be 
     * removed from this container.
     * */
    public void remove(Component comp) {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since the components should never be 
     * removed from this container.
     * */
    public void removeAll() {
        throw new IllegalArgumentException();
    }

    /**
     * Throws an IllegalArgumentException since the layout manager is
     * internally set and can not be changed.
     * */
    public void setLayout(LayoutManager mgr) {
        throw new IllegalArgumentException();
    }

    /**
     * Sets the scrollbars values, according to the image display area and
     * image size. The current scroll position is kept.
     *
     * */
    private void setScrollbars() {
        Dimension asz; // actual image area size
        Dimension psz; // preferred size
        int pos;       // current scroll position
        int szx,szy;   // actual image display area size

        if (!imgDisplay.calcDim()) {
            // While the image dimensions are not known we can't really update 
            // the scrollbar.
            return;
        }

        // Get the dimensions
        asz = imgDisplay.getSize();
        psz = imgDisplay.getPreferredSize();

        // Initialize lastZoom and lastSize if never done yet
        if (lastZoom == 0f) lastZoom = zoom;
        if (lastSize == null) lastSize = new Dimension(asz.width,asz.height);

        // Get the actual display size
        szx = (asz.width < psz.width) ? asz.width : psz.width;
        szy = (asz.height < psz.height) ? asz.height : psz.height;

        // Set horizontal scrollbar
        pos = (int)((hsbar.getValue()+lastSize.width/2f)/lastZoom*zoom-szx/2f);
        if (pos > (psz.width-asz.width)) pos = psz.width-asz.width;
        if (pos < 0) pos = 0;
        if (asz.width <= 0) asz.width = 1;
        if (psz.width <= 0) psz.width = 1;
        hsbar.setValues(pos,asz.width,0,psz.width);
        asz.width = (int)(asz.width*BLOCK_INCREMENT_PROPORTION);
        if (asz.width <= 0) asz.width = 1;
        hsbar.setBlockIncrementI(asz.width);

        // Set vertical scrollbar
        pos = (int)((vsbar.getValue()+lastSize.height/2f)/lastZoom*zoom-szy/2f);
        if (pos > (psz.height-asz.height)) pos = psz.height-asz.height;
        if (pos < 0) pos = 0;
        if (asz.height <= 0) asz.height = 1;
        if (psz.height <= 0) psz.height = 1;
        vsbar.setValues(pos,asz.height,0,psz.height);
        asz.height = (int)(asz.height*BLOCK_INCREMENT_PROPORTION);
        if (asz.height <= 0) asz.height = 1;
        vsbar.setBlockIncrementI(asz.height);

        // Save the zoom and display size used in the scrollbar calculation
        lastZoom = zoom;
        lastSize.width = szx;
        lastSize.height = szy;
    }

    /**
     * This class implements the component that displays the currently
     * viewable image portion inside the ImgScrollPane. It handles the
     * necessary erasing, zooming and panning.
     *
     * <P>NOTE: extending 'Canvas' instead of 'Component' solves the
     * flickering problem of lightweight components which are in heavyweight
     * containers.
     *
     * */
    private class ImageScrollDisplay extends Canvas {

        /** The image to be displayed 
         *
         * @serial */
        Image img;

        /** The preferred size for this component 
         *
         * @serial */
        Dimension dim = new Dimension();

        /** If the current graphics context should be erased prior to drawing
         * the image. Set when the image and/or zoom factor is changed.
         *
         * @serial */
        boolean erase;

        /** The image dimensions, without any scaling. Set as soon as they are
            known. 
            *
            * @serial */
        Dimension imgDim = new Dimension();

        /** The image dimension flags, as in ImageObserver. The
         * ImageObserver.WIDTH and ImageObserver.HEIGHT flags are set whenever
         * the dimension is stored in imgDim. They are reset whenever the
         * image changes.
         *
         * @serial */
        int dimFlags;

        /** The last offset used in update().
         *
         * @serial */
        Point lastUpdateOffset;

        /**
         * Sets the image to display in this component. If the image is not
         * ready for display it will be prepared in the current thread. The
         * current zoom factor applies.
         *
         * <P>If the image is not ready for display (i.e. it has not been
         * rendered at its natural size) it will be rendered in the current
         * thread, if not being already rendered in another one. This means
         * that the current thread can block until the image is ready.
         *
         * <P>If the image is rendered incrementally (it depends on the
         * underlying 'ImageProducer') it will be displayed in that way if the
         * incremental display is set for the Component class. See the
         * 'imageUpdate()' method of the 'Component' class.
         *
         * <P>If the image is the same as the current one nothing is done.
         *
         * @param img The image to display.
         *
         * @see Component#imageUpdate
         *
         * */
        void setImage(Image img) {
            // Update object state
            synchronized (ImgScrollPane.this) {
                if (img == null) {
                    throw new IllegalArgumentException();
                }
                // If same image do nothing
                if (this.img == img) {
                    return;
                }
                
                // (Re)initialize
                dimFlags = 0;
                this.img = img;
                lastSize = null;
                lastZoom = 0f;
                setScrollbars();
                // Set to erase previous image
                erase = true;
            }
            // Start image production (if the image is already being prepared
            // the method does nothing)
            ImgScrollPane.this.prepareImage(img,this);
        }
    
        /**
         * Returns the minimum size for this component, which is (0,0).
         *
         * @return The minimum size
         *
         * */
        public Dimension getMinimumSize() {
            return new Dimension(0,0);
        }

        /**
         * Returns the maximum size for this component, which is infinite.
         *
         * @return The maximum size
         *
         * */
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
        }

        /**
         * Returns the preferred size for this component, which is the image
         * display size, if known, the previous image display size, if any, or
         * the size specified at the constructor.
         *
         * @return The preferred size for this component.
         *
         * */
        public Dimension getPreferredSize() {
            return dim;
        }

        /**
         * Monitors the image rendering for dimensions and calls the
         * superclass' 'imageUpdate()' method. If the display size of the
         * image is not yet known and the image dimensions are obtained, then
         * the scrollbars' values are set. If 'img' is not the current image
         * to display nothing is done and 'false' is returned indicating that
         * nothing more is necessary for it.
         *
         * @see ImageObserver#imageUpdate
         *
         * @see Component#imageUpdate
         *
         * */
        public boolean imageUpdate(Image img, int infoflags,
                                   int x, int y, int w, int h) {
            if (this.img != img) {
                // Not the image we want to display now (might be an old one)
                // => do nothing and no more info needed on that image
                return false;
            }
            // If got the image dimensions then store them and set component
            // size as appropriate.
            if ((infoflags & (ImageObserver.WIDTH|ImageObserver.HEIGHT)) != 0) {
                // Got some image dimension
                synchronized (ImgScrollPane.this) {
                    // Read the dimensions received
                    if ((infoflags & ImageObserver.WIDTH) != 0) {
                        imgDim.width = w;
                        dimFlags |= ImageObserver.WIDTH;
                    }
                    if ((infoflags & ImageObserver.HEIGHT) != 0) {
                        imgDim.height = h;
                        dimFlags |= ImageObserver.HEIGHT;
                    }
                    // If we got to know the image dimensions force the layout
                    // to be done (to see if it is necessary to show
                    // scrollbars)
                    if (dimFlags ==
                        (ImageObserver.WIDTH|ImageObserver.HEIGHT)) {
                        ImgScrollPane.this.doLayout();
                    }
                }
            }
            // Call the superclass' method to continue processing
            return super.imageUpdate(img,infoflags,x,y,w,h);
        }

        /**
         * Paints the image, if any, on the graphics context by calling
         * update().
         *
         * @param g The graphics context to paint on.
         *
         * */
        public void paint(Graphics g) {
            // Now call update as usual
            update(g);
        }

        /**
         * Updates the component by drawing the relevant part of the image
         * that fits within the Graphics clipping area of 'g'. If the image is
         * not already being prepared for rendering or is not already rendered
         * this method does not start it. This is to avoid blocking the AWT
         * threads for rendering the image. The image rendering is started by
         * the 'setImage()' method.
         *
         * @param g The graphics context where to draw
         *
         * @see #setImage
         *
         * */
        public void update(Graphics g) {
            Image img;         // The image to display
            float zoom;        // The zoom factor
            boolean erase;     // If the display area should be erased
            int dx1,dy1;       // Scaling destionation upper-left corner
            int dx2,dy2;       // Scaling destination down-right corner
            int sx1,sy1;       // Scaling source upper-left corner
            int sx2,sy2;       // Scaling source down-right corner
            int ox,oy;         // Centering offset
            Rectangle b;       // Bounds of the display area
            int offx,offy;     // Offset of the upper-left corner
            int loffx,loffy;   // Offset of the upper-left corner of last update
            boolean copyScroll;// If the scrolling should be done by copying
            Rectangle clip;    // The clipping area
            int status;        // The image fetching status

            // Copy to local variables in a synchronized block to avoid races
            synchronized (ImgScrollPane.this) {
                img = this.img;
                zoom = ImgScrollPane.this.zoom;
                erase = this.erase;
                copyScroll = ImgScrollPane.this.copyScroll;
                this.erase = false;
                
                // If no image or the image has not started preparation yet do
                // nothing. We do not want to start the image preparation in
                // this thread because it can be long.
                if (img == null || (status = this.checkImage(img,null)) == 0) {
                    return;
                }
                // Get the display size and eventual centering offset for the
                // image.
                b = this.getBounds();
                ox = (b.width > dim.width) ? (b.width-dim.width)/2 : 0;
                oy = (b.height > dim.height) ? (b.height-dim.height)/2 : 0;
                // Get the display offset
                clip = g.getClipBounds();
                if (lastUpdateOffset != null &&
                    (clip.width < b.width || clip.height < b.height)) {
                    // The clip is smaller than the display area => we need to 
                    // paint with the last offset to avoid screwing up the
                    // displayed image.
                    offx = lastUpdateOffset.x;
                    offy = lastUpdateOffset.y;
                }
                else {
                    // The clipping area covers the whole display area => we
                    // can use the current offset.
                    offx = hsbar.getValue();
                    offy = vsbar.getValue();
                }
                // Get and update the offset of last update
                if (lastUpdateOffset == null) {
                    lastUpdateOffset = new Point();
                }
                loffx = lastUpdateOffset.x;
                loffy = lastUpdateOffset.y;
                lastUpdateOffset.x = offx;
                lastUpdateOffset.y = offy;
                // Set the display size according to zoom
                if (zoom == 1f) {
                    // Natural image size, no scaling
                    // Displace the origin of the image according to offset
                    ox -= offx;
                    oy -= offy;
                    // No zoom so no translation for scaling compensation needed
                    sx1 = sy1 = 0; // to keep compiler happy
                    sx2 = sy2 = 0; // to keep compiler happy
                    dx1 = dy1 = 0; // to keep compiler happy
                    dx2 = dy2 = 0; // to keep compiler happy
                }
                else {
                    int sox,soy;       // Scaling compensation offset
                    // Calculate coordinates of lower right corner for scaling
                    if (dimFlags != 
                        (ImageObserver.WIDTH|ImageObserver.HEIGHT)) {
                        // Image dims not yet available we can't display
                        return;
                    }
                    sx1 = sy1 = 0;
                    sx2 = imgDim.width;
                    sy2 = imgDim.height;
                    dx1 = dy1 = 0;
                    dx2 = dim.width;
                    dy2 = dim.height;
                    sox = soy = 0;
                    // Limit the scaling area according to display size so
                    // that scaling operates only on the area to be displayed
                    if (dx2 > b.width) {
                        // Calculate coordinates of displayed portion
                        dx2 = b.width+ ((zoom>1f) ? (int)Math.ceil(zoom) : 0);
                        if ((int)zoom == zoom) {
                            // For integer zoom make dx2 a multiple of zoom
                            dx2 = (int)(Math.ceil(dx2/zoom)*zoom);
                        }
                        sx1 = (int)(offx/zoom);
                        sx2 = sx1 + (int)(dx2/zoom);
                        // Compensate the scaling on integer coordinates with
                        // an offset
                        sox = (int)(sx1*zoom-offx);
                    }
                    if (dy2 > b.height) {
                        // Calculate coordinates of displayed portion
                        dy2 = b.height + ((zoom>1f) ? (int)Math.ceil(zoom) : 0);
                        if ((int)zoom == zoom) {
                            // For integer zoom make dy2 a multiple of zoom
                            dy2 = (int)(Math.ceil(dy2/zoom)*zoom);
                        }
                        sy1 = (int)(offy/zoom);
                        sy2 = sy1 + (int)(dy2/zoom);
                        // Compensate the scaling on integer coordinates with
                        // an extra offset
                        soy = (int)(sy1*zoom-offy);
                    }
                    // Apply centering offset and scaling compensation offset
                    dx1 += ox + sox;
                    dy1 += oy + soy;
                    dx2 += ox + sox;
                    dy2 += oy + soy;
                }
            }
            // If the image is not yet complete and we are scrolling set to
            // erase to avoid leftovers of previous scroll on parts of the
            // image which are not yet ready
            if ((status & ImageObserver.ALLBITS) == 0 &&
                (loffx != offx || loffy != offy)) {
                erase = true;
            }
            // Now we have the necessary info for display. We do it outside
            // synchronized to avoid any potential deadlocks with imageUpdate().
            if (erase) {
                // We need to erase the current image. Make sure that we
                // redraw everything by setting the clipping area to the whole 
                // display one.
                g.setClip(0,0,b.width,b.height);
                g.setColor(this.getBackground());
                g.fillRect(0,0,b.width,b.height);
            }

            // Use copy scrolling if the image has not been erased, we are
            // scrolling, the image is complete, and copy scrolling is enabled.
            if (copyScroll && !erase && (loffx != offx || loffy != offy) &&
                (status & ImageObserver.ALLBITS) != 0) {
                // We might be able to move some part of the displayed area
                // instead of redrawing everything.

                // We are just trasnlating the current image, so we can reuse
                // a part of it.

                int culx,culy; // Clipping area upper-left corner (inclusive)
                int cdrx,cdry; // Clipping area down-right corner (exclusive)
                int vulx,vuly; // Valid area upper-left corner (inclusive)
                int vdrx,vdry; // Valid area down-right corner (exclusive)

                culx = clip.x;
                culy = clip.y;
                cdrx = clip.x+clip.width;
                cdry = clip.y+clip.height;

                // Initialize valid area as the current display area after the 
                // translation.
                vulx = loffx-offx;
                vuly = loffy-offy;
                vdrx = vulx+b.width;
                vdry = vuly+b.height;

                // Make new valid area the intersection of the clipping area
                // and the valid area.
                if (culx > vulx) vulx = culx;
                if (culy > vuly) vuly = culy;
                if (cdrx < vdrx) vdrx = cdrx;
                if (cdry < vdry) vdry = cdry;

                // If the new valid area is non-empty then copy current image
                // data
                if (vulx < vdrx && vuly < vdry) {
                    // Ok we can move a part instead of repainting
                    g.copyArea(vulx+offx-loffx,vuly+offy-loffy,
                               vdrx-vulx,vdry-vuly,
                               loffx-offx,loffy-offy);
                    // Now we need to redraw the other parts
                    if (culx < vulx) { // Need to draw at left
                        g.setClip(culx,culy,vulx-culx,cdry-culy);
                        if (zoom == 1f) { // No scaling
                            g.drawImage(img,ox,oy,this);
                        }
                        else { // Draw the image using on the fly scaling
                            g.drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,
                                        this);
                        }
                    }
                    if (vdrx < cdrx) { // Need to draw at right
                        g.setClip(vdrx,culy,cdrx-vdrx,cdry-culy);
                        if (zoom == 1f) { // No scaling
                            g.drawImage(img,ox,oy,this);
                        }
                        else { // Draw the image using on the fly scaling
                            g.drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,
                                        this);
                        }
                    }
                    if (culy < vuly) { // Need to draw at top
                        g.setClip(vulx,culy,vdrx-vulx,vuly-culy);
                        if (zoom == 1f) { // No scaling
                            g.drawImage(img,ox,oy,this);
                        }
                        else { // Draw the image using on the fly scaling
                            g.drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,
                                        this);
                        }
                    }
                    if (vdry < cdry) { // Need to draw at bottom
                        g.setClip(vulx,vdry,vdrx-vulx,cdry-vdry);
                        if (zoom == 1f) { // No scaling
                            g.drawImage(img,ox,oy,this);
                        }
                        else { // Draw the image using on the fly scaling
                            g.drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,
                                        this);
                        }
                    }
                }
                else {
                    // New valid area is empty, we need to draw everything
                    if (zoom == 1f) { // No scaling
                        g.drawImage(img,ox,oy,this);
                    }
                    else { // Draw the image using on the fly scaling
                        g.drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,this);
                    }
                }
            }
            else {
                // We are not translating, so we can't copy
                if (zoom == 1f) { // No scaling
                    g.drawImage(img,ox,oy,this);
                }
                else { // Draw the image using on the fly scaling
                    g.drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,this);
                }
            }
        }

        /**
         * Calculates the image display dimensions according to the zoom and
         * image size. The dimensions are stored in 'dim'.
         *
         * @return True if the dimensions could be calculated, false if not
         * (i.e. not enough info is available).
         *
         * */
        boolean calcDim() {
            // We need the image dimensions
            if (dimFlags != (ImageObserver.WIDTH|ImageObserver.HEIGHT)) {
                // Image dims not yet available we can't do anything
                return false;
            }
            // Calculate dims
            if (zoom == 1f) {
                // Natural image dimension
                dim.width = imgDim.width;
                dim.height = imgDim.height;
            }
            else {
                // Apply zoom
                dim.width = (int)(zoom*imgDim.width);
                dim.height = (int)(zoom*imgDim.height);
            }
            return true;
        }
    }

    /**
     * Scrollbars for the ImgScrollPane container. They are normal AWT
     * Scrollbars, but with a thickness of
     * ImgScrollPane.SCROLLBAR_THICKNESS. Also many of the set method of the
     * Adjustable interface are overriden and throw IllegalArgumentException
     * since they are not to be used externally.
     *
     * */
    class ISPScrollbar extends Scrollbar {

        /**
         * Constructs a new scroll bar with the specified orientation and
         * values.
         *
         * <P>The orientation argument must take one of the two values
         * Scrollbar.HORIZONTAL, or Scrollbar.VERTICAL, indicating a
         * horizontal or vertical scroll bar, respectively.
         *
         * @param orientation indicates the orientation of the scroll bar
         *
         * @param value the initial value of the scroll bar.
         *
         * @param visible the size of the scroll bar's bubble, representing
         * the visible portion; the scroll bar uses this value when paging up
         * or down by a page.
         *
         * @param min the minimum value of the scroll bar.
         *
         * @param max  the maximum value of the scroll bar.
         *
         * @param svt The scrollbar visible type
         **/
        ISPScrollbar(int orientation,
                     int value, int visible, int min, int max) {
            super(orientation,value,visible,min,max);
        }
    
        /**
         * Returns the preferred size of the scrollbar. It is the same as the
         * preferred size of a normal scrollbar but with a thickness of
         * ImgScrollPane.SCROLLBAR_THICKNESS.
         *
         * @return The Scrollbar preferred size
         * */
        public Dimension getPreferredSize() {
            Dimension psz = super.getPreferredSize();
            if (getOrientation() == HORIZONTAL) {
                psz.height = ImgScrollPane.SCROLLBAR_THICKNESS;
            }
            else {
                psz.width = ImgScrollPane.SCROLLBAR_THICKNESS;
            }
            return psz;
        }

        /**
         * Throws an IllegalArgumentException since the minimum value should
         * never be set externally.
         * */
        public void setMinimum(int min) {
            throw new IllegalArgumentException();
        }
        
        /**
         * Throws an IllegalArgumentException since the maximum value should
         * never be set externally.
         * */
        public void setMaximum(int max) {
            throw new IllegalArgumentException();
        }
        
        /**
         * Throws an IllegalArgumentException since the visible amount should
         * never be set externally.
         * */
        public void setVisibleAmount(int v) {
            throw new IllegalArgumentException();
        }
        
        /**
         * Throws an IllegalArgumentException since the block increment should
         * never be set externally.
         * */
        public void setBlockIncrement(int b) {
            throw new IllegalArgumentException();
        }
        
        /**
         * Sets the block increment for this scroll bar.
         *
         * <P>The block increment is the value that is added (subtracted) when
         * the user activates the block increment area of the scroll bar,
         * generally through a mouse or keyboard gesture that the scroll bar
         * receives as an adjustment event.
         *
         * <P>This is a version to be used by The ImgScrollPane class only.
         *
         * @param v the amount by which to increment or decrement the scroll
         * bar's value.
         * */
        void setBlockIncrementI(int v) {
            super.setBlockIncrement(v);
        }    

        /**
         * Sets the value of this scroll bar to the specified value. 
         *
         * <P>If the value supplied is less than the current minimum or
         * greater than the current maximum, then one of those values is
         * substituted, as appropriate.
         *
         * <P>This is a version to be used by The ImgScrollPane class only.
         *
         * @param newValue he new value of the scroll bar.
         * */
        void setValueI(int newValue) {
            super.setValue(newValue);
        }
        
        /**
         * Sets the value of this scroll bar to the specified value and
         * requests a repaint of the image area.
         *
         * <P>If the value supplied is less than the current minimum or
         * greater than the current maximum, then one of those values is
         * substituted, as appropriate.
         *
         * @param newValue he new value of the scroll bar.
         * */
        public void setValue(int newValue) {
            // Set the value and check if we need to repaint
            synchronized (ImgScrollPane.this) {
                super.setValue(newValue);
                newValue = getValue(); // get the actual value for check
                if (imgDisplay.lastUpdateOffset != null) {
                    if (getOrientation() == HORIZONTAL) {
                        if (imgDisplay.lastUpdateOffset.x == newValue) {
                            return; // No change
                        }
                    }
                    else {
                        if (imgDisplay.lastUpdateOffset.y == newValue) {
                            return; // No change
                        }
                    }
                }
            }
            // New value changes from last drawn => repaint
            imgDisplay.repaint();
        }
    }
}
