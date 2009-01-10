/*
 * Copyright (c) 2008 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Writes PNG images using the ImageIO class.  Only one instance of this class
 * will ever be created, so this class contains no member variables to ensure
 * thread safety.
 * @author jdb
 */
public class JpegFormat extends SimpleFormat
{
    /**
     * Protected default constructor to prevent direct instantiation.
     */
    protected JpegFormat() {}
    
    @Override
    public String getMimeType()
    {
        return "image/jpeg";
    }

    @Override
    public boolean supportsMultipleFrames()
    {
        return false;
    }

    @Override
    public boolean supportsFullyTransparentPixels()
    {
        return false;
    }

    @Override
    public boolean supportsPartiallyTransparentPixels()
    {
        return false;
    }

    @Override
    public void writeImage(List<BufferedImage> frames, OutputStream out) throws IOException
    {
        if (frames.size() > 1)
        {
            throw new IllegalArgumentException("Cannot render animations in JPEG format");
        }
        ImageIO.write(frames.get(0), "jpeg", out);
    }
}
