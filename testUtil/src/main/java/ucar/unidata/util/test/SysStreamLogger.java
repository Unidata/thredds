/*

Now it's possible to redirect Sytem.out/err by calling
SysStreamsLogger.bindSystemStreams() at the begining of your
application.

    Logging Output looks like this (from OpenEJB startup)

    2012-06-27 13:44:12,792  INFO [main:] SYSOUT:181 - Apache OpenEJB 3.1.4    build: 20101112-03:32
    2012-06-27 13:44:12,793  INFO [main:] SYSOUT:181 - http://openejb.apache.org/
*/

package ucar.unidata.util.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;

public class SysStreamLogger
{
    protected static Logger sysOutLogger = LoggerFactory.getLogger("SYSOUT");
    protected static Logger sysErrLogger = LoggerFactory.getLogger("SYSERR");

    public static final PrintStream sysout = System.out;
    public static final PrintStream syserr = System.err;

    protected static final String LINE_SEPERATOR = System.getProperty("line.separator");

    public static void bindSystemStreams() {
        // Enable autoflush
        System.setOut(new PrintStream(new LoggingOutputStream(sysOutLogger, false), true));
        System.setErr(new PrintStream(new LoggingOutputStream(sysErrLogger, true), true));
    }

    public static void unbindSystemStreams() {
        System.setOut(sysout);
        System.setErr(syserr);
    }

    protected static class LoggingOutputStream extends java.io.OutputStream
    {
        protected Logger log;
        protected boolean isError;

        /**
         * Used to maintain the contract of {@link #close()}.
         */
        protected boolean hasBeenClosed = false;

        /**
         * The internal buffer where data is stored.
         */
        protected byte[] buf;

        /**
         * The number of valid bytes in the buffer. This value is always in the
         * range <tt>0</tt> through <tt>buf.length</tt>; elements
         * <tt>buf[0]</tt> through <tt>buf[count-1]</tt> contain valid byte
         * data.
         */
        protected int count;

        /**
         * Remembers the size of the buffer for speed.
         */
        protected int bufLength;

        /**
         * The default number of bytes in the buffer. =2048
         */
        public static final int DEFAULT_BUFFER_LENGTH = 2048;

        protected LoggingOutputStream() {
            // illegal
        }

        /**
         * Creates the LoggingOutputStream to flush to the given Category.
         *
         * @param log     the Logger to write to
         * @param isError the if true write to error, else info
         * @throws IllegalArgumentException if cat == null or priority == null
         */
        public LoggingOutputStream(Logger log, boolean isError) throws IllegalArgumentException {
            if(log == null) {
                throw new IllegalArgumentException("log == null");
            }

            this.isError = isError;
            this.log = log;
            bufLength = DEFAULT_BUFFER_LENGTH;
            buf = new byte[DEFAULT_BUFFER_LENGTH];
            count = 0;
        }

        /**
         * Closes this output stream and releases any system resources
         * associated with this stream. The general contract of
         * <code>close</code> is that it closes the output stream. A closed
         * stream cannot perform output operations and cannot be reopened.
         */
        @Override
        public void close() {
            flush();
            hasBeenClosed = true;
        }

        /**
         * Writes the specified byte to this output stream. The general contract
         * for <code>write</code> is that one byte is written to the output
         * stream. The byte to be written is the eight low-order bits of the
         * argument <code>b</code>. The 24 high-order bits of <code>b</code> are
         * ignored.
         *
         * @param b the <code>byte</code> to write
         */
        @Override
        public void write(final int b) throws IOException {
            if(hasBeenClosed) {
                throw new IOException("The stream has been closed.");
            }

            // don't log nulls
            if(b == 0) {
                return;
            }

            // would this be writing past the buffer?
            if(count == bufLength) {
                // grow the buffer
                final int newBufLength = bufLength + DEFAULT_BUFFER_LENGTH;
                final byte[] newBuf = new byte[newBufLength];

                System.arraycopy(buf, 0, newBuf, 0, bufLength);

                buf = newBuf;
                bufLength = newBufLength;
            }

            buf[count] = (byte) b;
            count++;
        }

        /**
         * Flushes this output stream and forces any buffered output bytes to be
         * written out. The general contract of <code>flush</code> is that
         * calling it is an indication that, if any bytes previously written
         * have been buffered by the implementation of the output stream, such
         * bytes should immediately be written to their intended destination.
         */
        @Override
        public void flush() {

            if(count == 0) {
                return;
            }

            // don't print out blank lines; flushing from PrintStream puts out
            // these
            if(count == LINE_SEPERATOR.length()) {
                if(((char) buf[0]) == LINE_SEPERATOR.charAt(0) && ((count == 1) || // <-
                        // Unix
                        // &
                        // Mac,
                        // ->
                        // Windows
                        ((count == 2) && ((char) buf[1]) == LINE_SEPERATOR.charAt(1)))) {
                    reset();
                    return;
                }
            }

            final byte[] theBytes = new byte[count];

            System.arraycopy(buf, 0, theBytes, 0, count);

            if(isError) {
                log.error(new String(theBytes));
            } else {
                log.info(new String(theBytes));
            }

            reset();
        }

        protected void reset() {
            // not resetting the buffer -- assuming that if it grew that it
            // will likely grow similarly again
            count = 0;
        }
    }
}
