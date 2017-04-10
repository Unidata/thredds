package thredds.server.jupyter;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

class JupyterMessage {
    public enum MessageType {
        connect_request, connect_reply, status, stream,
        execute_request, execute_reply, execute_result, execute_input;
    };

    String session, user, msg_id, version;
    JsonObject metadata, content;
    LocalDateTime dateTime;
    MessageType type;
    JupyterMessage parent;

    public JupyterMessage(String session, String user, MessageType type) {
        this.user = user;
        this.type = type;
        this.session = session;

        this.metadata = null;
        this.content = null;
        this.parent = null;
        this.msg_id = UUID.randomUUID().toString();
        this.dateTime = LocalDateTime.now();
        this.version = "5.0";
    }

    public void addParent(JupyterMessage parent) {
        this.parent = parent;
    }

    public void addMetadata(JsonObject metadata) {
        this.metadata = metadata;
    }

    public void addContent(JsonObject content) {
        this.content = content;
    }

    public JsonObject toJson() {
       return Json.createObjectBuilder()
                .add("msg_id", msg_id)
                .add("username", user)
                .add("session", session)
                .add("msg_type", type.toString())
                .add("date", dateTime.toString())
                .add("version", version).build();
    }

    static public JupyterMessage fromJson(JsonObject json) {
        JupyterMessage ret = new JupyterMessage(json.getString("session"),
                json.getString("username"),
                MessageType.valueOf(json.getString("msg_type")));
        ret.msg_id = json.getString("msg_id");
        ret.dateTime = LocalDateTime.parse(json.getString("date"));
        ret.version = json.getString("version");
        return ret;
    }

    public String digest(byte[] key) {
        try {
            final Mac sha256_mac = Mac.getInstance("HmacSHA256");
            final SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
            sha256_mac.init(secret_key);
            sha256_mac.update(toJson().toString().getBytes());

            if (parent != null) {
                sha256_mac.update(parent.toJson().toString().getBytes());
            } else {
                sha256_mac.update("{}".getBytes());
            }

            if (metadata != null) {
                sha256_mac.update(metadata.toString().getBytes());
            } else {
                sha256_mac.update("{}".getBytes());
            }

            if (content != null) {
                sha256_mac.update(content.toString().getBytes());
            } else {
                sha256_mac.update("{}".getBytes());
            }
            return Hex.encodeHexString(sha256_mac.doFinal());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "";
        }
    }
}