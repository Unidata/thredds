package thredds.viewer.image;

import thredds.ui.BAMutil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;

public class ImageViewPanel extends JPanel {
  private static boolean debug = false;

  static {
    if (false) {
      String[] readers = javax.imageio.ImageIO.getReaderFormatNames();
      for (int i = 0; i < readers.length; i++) {
        System.out.println(" reader = "+ readers[i]);
      }
      System.out.println("-------------------------------------------");
      String[] readerMimes = javax.imageio.ImageIO.getReaderMIMETypes();
      for (int i = 0; i < readerMimes.length; i++) {
        System.out.println(" readerMimes = "+ readerMimes[i]);
      }
      System.out.println("-------------------------------------------");
      String[] writers = javax.imageio.ImageIO.getWriterFormatNames();
      for (int i = 0; i < writers.length; i++) {
        System.out.println(" writers = "+ writers[i]);
      }
      System.out.println("-------------------------------------------");
      String[] writerMimes = javax.imageio.ImageIO.getWriterMIMETypes();
      for (int i = 0; i < writerMimes.length; i++) {
        System.out.println(" writerMimes = "+ writerMimes[i]);
      }
      System.out.println("-------------------------------------------");
    }
  }

  private int nelems = 0, nlines = 0;
  private ImageView imageView;
  private BufferedImage currentImage = null;
  private AffineTransform at = new AffineTransform();  // identity transform

  private int dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2;

  public ImageViewPanel() {
    imageView = new ImageView();

    makeActions();

    JToolBar toolbar = new JToolBar();
    BAMutil.addActionToContainer( toolbar, zoomIn);
    BAMutil.addActionToContainer( toolbar, zoomOut);
    BAMutil.addActionToContainer( toolbar, zoomDefault);
    BAMutil.addActionToContainer( toolbar, moveUp);
    BAMutil.addActionToContainer( toolbar, moveDown);
    BAMutil.addActionToContainer( toolbar, moveLeft);
    BAMutil.addActionToContainer( toolbar, moveRight);

    // assemble UI
    // setLayout(new BorderLayout());
    JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    toolPanel.add(toolbar);

    //add(toolPanel, BorderLayout.NORTH);
    add(new ImageView(), BorderLayout.CENTER);
  }

  private AbstractAction zoomIn, zoomOut, zoomDefault;
  private AbstractAction moveUp, moveDown, moveLeft, moveRight;
  private void makeActions() {
      // add buttons/actions
    zoomIn = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        double scalex = at.getScaleX();
        double scaley = at.getScaleY();
        at.setToScale( 2 * scalex, 2 * scaley);
        imageView.repaint();
        revalidate();
      }
    };
    BAMutil.setActionProperties( zoomIn, "MagnifyPlus", "zoom In", false, 'I', KeyEvent.VK_ADD);

    zoomOut = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        double scalex = at.getScaleX();
        double scaley = at.getScaleY();
        at.setToScale( .5 * scalex, .5 * scaley);
        imageView.repaint();
        revalidate();
      }
    };
    BAMutil.setActionProperties( zoomOut, "MagnifyMinus", "zoom Out", false, 'O', KeyEvent.VK_SUBTRACT);

    zoomDefault = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setDefault();
        imageView.drawG();
        imageView.repaint();
        revalidate();
      }
    };
    BAMutil.setActionProperties( zoomDefault, "Home", "Home map area", false, 'H', KeyEvent.VK_HOME);

    moveUp = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setDefault();
        imageView.repaint();
        revalidate();
      }
    };
    BAMutil.setActionProperties( moveUp, "Up", "move view Up", false, 'U', KeyEvent.VK_UP);

    moveDown = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setDefault();
        imageView.repaint();
      }
    };
    BAMutil.setActionProperties( moveDown, "Down", "move view Down", false, 'D', KeyEvent.VK_DOWN);

    moveLeft = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setDefault();
        imageView.repaint();
      }
    };
    BAMutil.setActionProperties( moveLeft, "Left", "move view Left", false, 'L', KeyEvent.VK_LEFT);

    moveRight = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setDefault();
        imageView.repaint();
      }
    };
    BAMutil.setActionProperties( moveRight, "Right", "move view Right", false, 'R', KeyEvent.VK_RIGHT);
  }

  public void setImage( BufferedImage im) {
    if (debug) System.out.println("ImageViewPanel setImage ");
    // clear();
    currentImage = im;
    nelems = currentImage.getWidth();
    nlines = currentImage.getHeight();

    setDefault();
    imageView.drawG(); // LOOK why doesnt repaint work GODDAMMIT ??
    imageView.repaint();
    revalidate();
  }

  private void setDefault() {
    dx1 = 0;
    dy1 = 0;
    sx1 = 0;
    sy1 = 0;

    dx2 = nelems;
    dy2 = nlines;
    sx2 = nelems;
    sy2 = nlines;
  }

  private class ImageView extends JPanel {

    public Dimension getPreferredSize() {
      return new Dimension(nelems, nlines);
    }

        /** System-triggered redraw. */
    public void paintComponent(Graphics g) {
      super.paintComponent( g);
      draw((Graphics2D)g);
    }

    public void drawG() {
      Graphics g = getGraphics();    // bypasses double buffering ?
      if (null != g) { // LOOK why is this null ???
        draw( (Graphics2D) g);
        g.dispose();
      }
    }

    private void draw(Graphics2D g) {
      if (currentImage == null) return;
      if (debug) System.out.println("ImageViewPanel paintComponent = "+currentImage.getWidth()+" "+currentImage.getHeight());

      Graphics2D g2 = (Graphics2D)g;
      clear( g2);

      g2.drawImage(currentImage, at, new java.awt.image.ImageObserver() {
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
          return true;
        }
      });

      /*Rectangle bounds = getBounds();

      dx2 = Math.min( dx2, (int) bounds.getX());
      dy2 = Math.min( dy2, (int) bounds.getY());

      g.drawImage( currentImage, dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2, new java.awt.image.ImageObserver() {
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
          return true;
        }
      });

          /* drawImage(Image img,
                                    int dx1,
                                    int dy1,
                                    int dx2,
                                    int dy2,
                                    int sx1,
                                    int sy1,
                                    int sx2,
                                    int sy2,
                                    ImageObserver observer)
              dx1 - the x coordinate of the first corner of the destination rectangle.
      dy1 - the y coordinate of the first corner of the destination rectangle.
      dx2 - the x coordinate of the second corner of the destination rectangle.
      dy2 - the y coordinate of the second corner of the destination rectangle.
      sx1 - the x coordinate of the first corner of the source rectangle.
      sy1 - the y coordinate of the first corner of the source rectangle.
      sx2 - the x coordinate of the second corner of the source rectangle.
      sy2 - the y coordinate of the second corner of the source rectan */
    }


    public void clear(Graphics2D g) {
      //Graphics2D g = (Graphics2D) getGraphics();
      if (g == null) return;
      Rectangle bounds = getBounds();
      if (debug) System.out.println("ImageViewPanel clear = "+bounds.width+" "+bounds.height);
      g.setColor(Color.white);
      g.fillRect(0, 0, bounds.width, bounds.height);
      // g.dispose();
    }
  }

}
/* Change History:
   $Log: ImageViewPanel.java,v $
   Revision 1.7  2005/12/15 00:43:31  caron
   all debug is off

   Revision 1.6  2004/10/08 00:32:07  caron
   add simple image viewer in the NCdump panel (2)

   Revision 1.5  2004/10/07 22:15:12  caron
   add simple image viewer in the NCdump panel

   Revision 1.4  2004/09/30 00:33:39  caron
   *** empty log message ***

   Revision 1.3  2004/09/25 00:09:44  caron
   add images, thredds tab

   Revision 1.2  2004/09/24 03:26:38  caron
   merge nj22

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1  2002/04/29 22:52:27  caron
   ADDE Cataloger verion 1

   Revision 1.1.1.1  2002/02/26 17:24:47  caron
   import sources

*/

