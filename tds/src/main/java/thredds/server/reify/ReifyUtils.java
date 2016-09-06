/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.reify;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

class ReifyUtils
{
    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Type Decls

    static /*package*/ enum Command
    {
        DOWNLOAD, INQUIRE;

        static public Command parse(String cmd)
        {
            if(cmd == null) return null;
            for(Command x : Command.values()) {
                if(cmd.equalsIgnoreCase(x.toString())) return x;
            }
            return null;
        }
    }

    static /*package*/ enum Inquiry
    {
	DOWNLOADDIR("downloaddir"),
	USERNAME("username");

	private String key;
	private Inquiry(String key) {this.key = key;}

	public String getKey() {return this.key;}

        static public Inquiry parse(String key)
        {
            if(key == null) return null;
            for(Inquiry x : Inquiry.values()) {
                if(key.equalsIgnoreCase(x.toString())) return x;
            }
            return null;
        }
    }

    static /*package*/ enum FileFormat
    {
        NETCDF3("nc3"),
        NETCDF4("nc4"),;

        private String extension;

        public final String getName()
        {
            return this.toString().toLowerCase();
        }

        public final String getExtension()
        {
            return this.extension;
        }

        FileFormat(String ext)
        {
            this.extension = ext;
        }

        static public FileFormat getformat(String fmt)
        {
            if(fmt == null) return null;
            for(FileFormat rf : FileFormat.values()) {
                if(fmt.equalsIgnoreCase(rf.toString())) return rf;
            }
            return null;
        }
    }

    static public class SendError extends RuntimeException
    {
        public int httpcode = 0;
        public String msg = null;

        /**
         * Generate an error based on the parameters
         *
         * @param httpcode 0=>no code specified
         */

        public SendError(int httpcode)
        {
            this(httpcode, (String) null);
        }

        /**
         * Generate an error based on the parameters
         *
         * @param httpcode 0=>no code specified
         * @param t        exception that caused the error; may not be null
         */

        public SendError(int httpcode, Throwable t)
        {
            this(httpcode, t.getMessage());
        }

        /**
         * Generate an error based on the parameters
         *
         * @param httpcode 0=>no code specified
         * @param msg      additional info; may be null
         */
        ;

        public SendError(int httpcode, String msg)
        {
            if(httpcode == 0) httpcode = HttpServletResponse.SC_BAD_REQUEST;
            if(msg == null)
                msg = "";
            this.httpcode = httpcode;
            this.msg = msg;
        }


    }

    //////////////////////////////////////////////////
    /*
    static public File
    createTarget(URI uri, File downloadroot, String extension)
            throws IOException
    {
        assert uri != null && downloadroot != null && extension != null;
        if(extension.charAt(0) != '.')
            extension = '.' + extension;
        String path = uri.getPath();
        int index;
        // Strip directory part
        index = path.lastIndexOf('/');
        if(index >= 0)
            path = path.substring(index + 1, path.length());
        // Strip all trailing extensions
        for(; ; ) {
            index = path.lastIndexOf('.');
            if(index < 0) break;
            path = path.substring(0, index);
        }
        File result;
        if(ReifyController.testinfo) {
            result = new File(downloadroot,path+extension);
            result.delete();
            result.createNewFile();
        }   else {
            // Create a temp file
            result = File.createTempFile(path, extension, downloadroot);
        }
        if(!result.canWrite())
            throw new IOException("Cannot create writeable temporary target file: " + result);
        return result;
    }
    */

    static File
    findSystemTempDir(String[] candidates)
    {
        for(String candidate : candidates) {
            File f = new File(candidate);
            if(f.exists() && f.canRead() && f.canWrite())
                return f;
            if(f.mkdirs()) // Try to create the path
                return f;
        }
        // As a last resort use the java temp file mechanism
        try {
            File tempfile = File.createTempFile("tmp", "tmp");
            File tempdir = tempfile.getParentFile();
            if(!tempdir.canWrite() || !tempdir.canRead())
                return null;
            return tempdir;
        } catch (IOException e) {
            return null;
        }
    }

    static public String
    toString(Map<String, String> map, boolean encode, String... order)
    {
        List<String> orderlist;
        if(order == null)
            orderlist = new ArrayList<String>(map.keySet());
        else
            orderlist = Arrays.asList(order);
        StringBuilder b = new StringBuilder();
        // Make two passes: one from order, and one from remainder
        boolean first = true;
        for(int i = 0; i < orderlist.size(); i++) {
            String key = orderlist.get(i);
            String value = map.get(key);
            if(value == null) continue; // ignore
            if(!first) b.append("&");
            b.append(key);
            b.append("=");
            b.append(encode ? urlEncode(value) : value);
            first = false;
        }
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(orderlist.contains(entry.getKey())) continue;
            if(!first) b.append("&");
            b.append(entry.getKey());
            b.append("=");
            b.append(encode ? urlEncode(entry.getValue()) : entry.getValue());
            first = false;
        }
        return b.toString();
    }

    static public Map<String, String>
    parseMap(String params, char sep, boolean decode)
    {
        Map<String, String> map = new HashMap<>();
        if(params == null || params.length() == 0)
            return map;
        String[] pieces = params.split("[&]");
        for(int i = 0; i < pieces.length; i++) {
            String piece = pieces[i].trim();
            String[] pair = piece.split("[=]");
            String key = pair[0].trim();
            if(pair.length >= 2) {
                String v = pair[1].trim();
                if(decode) v = urlDecode(v);
                map.put(key, v);
            } else if(pair.length == 1) {
                map.put(key, "");
            } else
                assert false : "split() failed";
        }
        return map;
    }

    static public List<String>
    parseList(String params, char sep, boolean decode)
    {
        List<String> list = new ArrayList<>();
        if(params == null || params.length() == 0)
            return list;
        String regex = "[ ]*[" + sep + "][ ]*";
        String[] pieces = params.split(regex);
        for(int i = 0; i < pieces.length; i++) {
            String piece = pieces[i];
            if(decode) piece = urlDecode(piece);
            list.add(piece);
        }
        return list;
    }

    static public String
    urlEncode(String s)
    {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
        return s;
    }

    static public String
    urlDecode(String s)
    {
        try {
            s = URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
        return s;
    }

    static public String
    getStackTrace(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        try {
            sw.close();
        } catch (IOException ioe) {
            return "close failure";
        }
        return sw.toString();
    }

}
