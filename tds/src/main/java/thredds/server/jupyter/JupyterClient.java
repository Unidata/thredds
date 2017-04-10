package thredds.server.jupyter;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import javax.json.Json;
import javax.json.JsonObject;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import ucar.nc2.constants.CDM;

public class JupyterClient {

    private class JupyterSocket {
        final private String DELIM = "<IDS|MSG>";

        ZMQ.Socket socket;
        JupyterSocket(ZMQ.Socket socket, String URI) {
            this.socket = socket;
            socket.connect(URI);
            socket.setReceiveTimeOut(timeout);
            socket.setSendTimeOut(timeout);
        }

        void close() {
            socket.close();
        }

        private String readSocket() {
            return new String(socket.recv(), CDM.utf8Charset);
        }

        private JsonObject readSocketJson() {
            String resp = readSocket();
            if (resp.isEmpty())
                return null;
            return Json.createReader(new StringReader(resp)).readObject();
        }

        private boolean sendJson(JsonObject json) {
            if (json != null) {
                return socket.sendMore(json.toString());
            } else {
                return socket.sendMore("{}");
            }
        }

        private boolean sendJson(JupyterMessage msg) {
            if (msg != null) {
                return sendJson(msg.toJson());
            } else {
                return sendJson((JsonObject)null);
            }
        }

        JupyterMessage sendMessage(JupyterMessage msg) {
            socket.sendMore(DELIM);
            socket.sendMore(msg.digest(connection.key.getBytes()));
            sendJson(msg);
            sendJson(msg.parent);
            sendJson(msg.metadata);
            sendJson(msg.content);
            socket.send("");

            return receiveMessage();
        }

        JupyterMessage receiveMessage() {
            String delim = "";
            while (!delim.equals(DELIM)) {
                delim = readSocket();
            }

            String signature = readSocket();
            JupyterMessage resp = JupyterMessage.fromJson(readSocketJson());
            resp.addParent(JupyterMessage.fromJson(readSocketJson()));
            resp.addMetadata(readSocketJson());
            resp.addContent(readSocketJson());
            // TODO: Need to do the signature checking with the actual bytes
            // that were sent (since our JSON processing could change the order--
            // it doesn't round-trip). This will necessitate a refactor so that the
            // JupyterMessage is much more self-contained.
//            if (signature.equals(resp.digest(connection.key.getBytes()))) {
//                return resp;
//            } else {
//                System.err.println("Jupyter message failed signature check.");
//                return null;
//            }
            return resp;
        }
    }

    private class JupyterConnection {
        public String session;

        ZContext ctx;
        JupyterSocket cmdSocket, shellSocket, resultSocket;
        Process kernel;
        String key = UUID.randomUUID().toString();

        final int controlPort = 5000;

        public JupyterConnection() {
            ctx = new ZContext();
            cmdSocket = new JupyterSocket(ctx.createSocket(ZMQ.REQ),
                    "tcp://localhost:" + String.valueOf(controlPort));
            session = UUID.randomUUID().toString();
        }

        public void connect() throws IOException {
            ProcessBuilder builder = new ProcessBuilder(pythonPath.toString(),
                    "-m", "ipykernel", "--Session.key=b'" + key + "'", "--control", String.valueOf(controlPort));
            builder.redirectErrorStream(true);
            kernel = builder.start();

            if (kernel.isAlive()) {
                InputStream inp = kernel.getInputStream();
                byte[] out = new byte[4096];
                int bytesRead = inp.read(out);
                System.out.println(new String(out, 0, bytesRead, CDM.utf8Charset));

                JupyterMessage control = new JupyterMessage(connection.session, "thredds",
                        JupyterMessage.MessageType.connect_request);
                JupyterMessage result = cmdSocket.sendMessage(control);
                shellSocket = new JupyterSocket(ctx.createSocket(ZMQ.DEALER),
                        "tcp://localhost:" + String.valueOf(result.content.getInt("shell")));

                // Must subscribe to a topic (even an empty one) to get messages
                ZMQ.Socket subSocket = ctx.createSocket(ZMQ.SUB);
                subSocket.subscribe("".getBytes());
                resultSocket = new JupyterSocket(subSocket,
                        "tcp://localhost:" + String.valueOf(result.content.getInt("iopub")));
            } else {
                throw new RuntimeException("Error starting: " + pythonPath.toString());
            }
        }

        public boolean execute(String code) {
            JupyterMessage exec = new JupyterMessage(connection.session, "thredds",
                    JupyterMessage.MessageType.execute_request);
            exec.content = Json.createObjectBuilder()
                    .add("code", code)
                    .add("silent", false)
                    .add("store_history", false)
                    .add("user_expressions", "")
                    .add("allow_stdin", false)
                    .add("stop_on_error", false)
                    .build();
            JupyterMessage result = shellSocket.sendMessage(exec);
            return result.content.getString("status").equals("ok");
        }

        public String getResult() {
            JupyterMessage msg;
            while ((msg = resultSocket.receiveMessage()) != null) {
                if (msg.type == JupyterMessage.MessageType.execute_result) {
                    return msg.content.getJsonObject("data").getString("text/plain");
                }
//                } else if (msg.type == JupyterMessage.MessageType.stream) {
//                    return msg.content.getString("text");
//                }
            }
            return "";
        }

        public void close() {
            if (resultSocket != null)
                resultSocket.close();
            if (shellSocket != null)
                shellSocket.close();
            cmdSocket.close();
            ctx.close();
            try {
                kernel.getOutputStream().write(0x5c);
            } catch (IOException e) {
                // We're leaving, so just ignore
                System.out.println("Error sending quit to kernel.");
            }
            kernel.destroy();
        }
    }

    JupyterConnection connection;
    Path pythonPath;
    int timeout;

    public JupyterClient(Path pythonPath, int timeout) {
        this.pythonPath = pythonPath;
        this.timeout = timeout;
    }

    static public List<String> methods(Path configPath) throws IOException {
        File scriptDir = configPath.resolve("jupyter/").toFile();
        List<String> ret = new ArrayList<>();
        FilenameFilter filter = (dir, name) -> name.endsWith(".py");

        for (File f: scriptDir.listFiles(filter)) {
            String methodName = f.getName();
            ret.add(methodName.substring(0, methodName.length() - 3));
        }
        return ret;
    }

    boolean connect(Path configPath, String method) throws IOException {
        connection = new JupyterConnection();
        connection.connect();

        File codeFile = configPath.resolve("jupyter/" + method + ".py").toFile();
        if (codeFile == null) {
            throw new RuntimeException("Unknown method " + method);
        }
        FileInputStream fis = new FileInputStream(codeFile);
        byte[] codeBuff = new byte[(int) codeFile.length()];
        fis.read(codeBuff);
        if (connection.execute(new String(codeBuff, CDM.UTF8))) {
            return true;
        } else {
            throw new RuntimeException("Error initializing Python code.");
        }
    }

    String processFile(String filePath, Map<String, String[]> parameters) {
        StringBuilder code = new StringBuilder("entrypoint('").append(filePath).append("'");
        for (Map.Entry<String, String[]> entry: parameters.entrySet()) {
            code.append(", ").append(entry.getKey()).append('=');
            String[] vals = entry.getValue();
            if (vals.length == 1) {
                code.append("'").append(vals[0]).append("'");
            } else {
                StringJoiner joiner = new StringJoiner("','", "['", "']");
                for (String item: vals) {
                    joiner.add(item);
                }
                code.append(joiner.toString());
            }
        }
        code.append(")");
        if (connection.execute(code.toString())) {
            String result = connection.getResult();
            return result.substring(1, result.length() - 1);
        } else {
            throw new RuntimeException("Error processing file with Python.");
        }
    }

    public void close() {
        connection.close();
    }
}
