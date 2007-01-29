/*
 * $Id: ProjectionPoint.java,v 1.12 2006/11/18 19:03:13 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.unidata.geoloc;



/**
 * Points on the Projective geometry plane.
 *
 * @see ProjectionPointImpl
 * @author John Caron
 * @version $Id: ProjectionPoint.java,v 1.12 2006/11/18 19:03:13 dmurray Exp $
 */
public interface ProjectionPoint {

    /**
     * Get the X coordinate
     * @return the X coordinate
     */
    public double getX();

    /**
     * Get the Y coordinate
     * @return the Y coordinate
     */
    public double getY();

    /**
     * Check for equality with the point in question
     *
     * @param pt     point to check
     * @return true if it represents the same point
     */
    public boolean equals(ProjectionPoint pt);

}

/*
 *  Change History:
 *  $Log: ProjectionPoint.java,v $
 *  Revision 1.12  2006/11/18 19:03:13  dmurray
 *  jindent
 *
 *  Revision 1.11  2005/05/13 18:29:10  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.10  2004/09/22 21:22:59  caron
 *  mremove nc2 dependence
 *
 *  Revision 1.9  2004/07/30 16:24:40  dmurray
 *  Jindent and javadoc
 *
 *  Revision 1.8  2004/02/27 21:21:28  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.7  2004/01/29 17:34:57  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.6  2003/04/08 15:59:06  caron
 *  rework for nc2 framework
 *
 *  Revision 1.1  2002/12/13 00:53:09  caron
 *  pass 2
 *
 *  Revision 1.1.1.1  2002/02/26 17:24:45  caron
 *  import sources
 *
 *  Revision 1.5  2000/08/18 04:15:18  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.4  2000/02/07 17:46:00  caron
 *  add equals() to ProjectionPoint
 *
 *  Revision 1.3  1999/12/16 22:57:22  caron
 *  gridded data viewer checkin
 *
 * #
 */








