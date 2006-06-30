package ucar.nc2.ui.image;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.URL;
import java.net.MalformedURLException;
import javax.swing.*;
import javax.swing.event.*;

import ucar.nc2.dt.image.ImageDatasetFactory;
import ucar.nc2.dataset.grid.GeoGrid;
import thredds.ui.BAMutil;

public class ImageViewPanel extends JPanel {
  private static boolean debug = false;

  private ImageDatasetFactory imageFactory = new ImageDatasetFactory();
  private boolean movieIsPlaying = false;
  private javax.swing.Timer timer;
  private int delay = 4000; // millisescs
  private JSpinner spinner;

  private boolean fullscreenMode = false;

  private PicturePane pixPane;

  public ImageViewPanel(Container buttPanel) {
    pixPane = new PicturePane();
    setLayout(new BorderLayout());
    add( pixPane, BorderLayout.CENTER);

    if (buttPanel == null) {
      buttPanel = new JPanel();
      add( buttPanel, BorderLayout.NORTH);
    }

    AbstractAction prevAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setImage( imageFactory.getNextImage(false));
      }
    };
    BAMutil.setActionProperties( prevAction, "VCRPrevFrame", "previous", false, 'P', -1);
    BAMutil.addActionToContainer(buttPanel, prevAction);

    AbstractAction nextAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
       setImage( imageFactory.getNextImage(true));
      }
    };
    BAMutil.setActionProperties( nextAction, "VCRNextFrame", "next", false, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, nextAction);

    AbstractAction playAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (movieIsPlaying) {
          if (timer != null) timer.stop();
          movieIsPlaying = false;
        } else {
          movieIsPlaying = true;
          timer = new Timer(delay, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
              setImage(imageFactory.getNextImage(true));
            }
          });
          timer.start();
        }
      }
    };
    BAMutil.setActionProperties( playAction, "MovieLoop", "loop", true, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, playAction);

    spinner = new JSpinner( new SpinnerNumberModel(5000, 10, 20000, 1000));
    spinner.addChangeListener( new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Integer value = (Integer) spinner.getModel().getValue();
        delay = value.intValue();
        if (timer != null) timer.setDelay( delay);
      }
    });
    buttPanel.add( spinner);

    AbstractAction fullscreenAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        System.out.println("isFullScreenSupported= "+device.isFullScreenSupported());

        fullFrame = new JFrame();
        fullFrame.setUndecorated( true);
        fullFrame.getContentPane().add( pixPane);

        fullscreenMode = true;
        device.setFullScreenWindow(fullFrame);

        // look for any input - get out of full screen mode
        KeyEventDispatcher dispatcher = new MyKeyEventDispatcher();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( dispatcher);
      }
    };
    BAMutil.setActionProperties(fullscreenAction, "Export", "fullscreen", true, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, fullscreenAction);
  }

  private JFrame fullFrame;

  private class MyKeyEventDispatcher implements java.awt.KeyEventDispatcher {
    public boolean dispatchKeyEvent(KeyEvent e) {
      if (fullscreenMode) {
        System.out.println(" dispatchKeyEvent="+e);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        device.setFullScreenWindow(null);
        fullscreenMode = false;
      }

      resetPane();
      if (fullFrame != null) fullFrame.dispose();

      // deregister yourself
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher( this);
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }

  private void resetPane() {
    add( pixPane, BorderLayout.CENTER);
    revalidate();
  }

  public void setImageFromGrid( GeoGrid grid) {
      try {
        BufferedImage image = imageFactory.openDataset( grid);
        setImage( image);

      } catch (Exception e2) {
        javax.swing.JOptionPane.showMessageDialog(null, "Error on dataset\n"+imageFactory.getErrorMessages());
        e2.printStackTrace();
      }
  }


   public boolean setImageFromUrl( String location) {

     if (location.startsWith("http")) {
       URL url = null;
       try {
         url = new URL( location);
       } catch (MalformedURLException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       }

       // uses ImageIO.createImageInputStream()
       pixPane.setPicture( url, "legendParam", 0.0);

     } else {

      try {
        BufferedImage image = imageFactory.open( location);
        setImage( image);

      } catch (Exception e2) {
        javax.swing.JOptionPane.showMessageDialog(null, "Error on dataset = "+location+"\n"+imageFactory.getErrorMessages());
        e2.printStackTrace();
        return false;
      }
    }

    return true;
  }

  public void setImage( BufferedImage im) {
    if (im == null) return;
    if (debug) System.out.println("ImageViewPanel setImage ");
    pixPane.setBufferedImage( im, "setImage");
    pixPane.repaint();
  }

  static public void main( String[] args) {
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
/* Change History:
   $Log: ImageViewPanel.java,v $
   Revision 1.9  2005/12/15 00:29:13  caron
   *** empty log message ***

   Revision 1.8  2005/12/02 00:15:37  caron
   NcML 
   Dimension.isVariableLength()

   Revision 1.7  2005/11/28 16:41:42  caron
   geogrid subset deal with 2D lat/lon
   movie loop on image panel

   Revision 1.6  2005/03/07 20:48:33  caron
   no message

   Revision 1.5  2005/02/23 20:10:21  caron
   no message

   Revision 1.4  2004/11/10 17:00:29  caron
   no message

   Revision 1.3  2004/11/07 02:55:12  caron
   no message

   Revision 1.2  2004/10/29 00:14:11  caron
   no message

   Revision 1.1  2004/10/23 21:55:41  caron
   new image drawing

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

