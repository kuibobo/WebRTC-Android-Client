package webrtc.module.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import webrtc.module.activity.JoinActivity;

public class WebSocketClient {
    private Socket client;
    private static final WebSocketClient me = new WebSocketClient();

    private WebSocketClient() {
    }

    public static WebSocketClient instance() {
        return me;
    }

    public void connect(String url) {
        try {
            client = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        client.connect();
    }

    public Emitter on(String event, Emitter.Listener fn) {
        return client.on(event, fn);
    }

    public void start(String name) {
        try {
            JSONObject message = new JSONObject();
            message.put("name", name);
            client.emit("readyToStream", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(JSONObject message) throws JSONException {
        client.emit("message", message);
    }

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);
        message.put("payload", payload);
        client.emit("message", message);
    }
}