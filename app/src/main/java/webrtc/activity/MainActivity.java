package webrtc.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.web2android.R;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;
import pub.devrel.easypermissions.EasyPermissions;
import webrtc.module.VoipApp;
import webrtc.module.activity.BaseActivity;
import webrtc.module.activity.JoinActivity;
import webrtc.module.service.VoipReceiver;
import webrtc.module.util.WebSocketClient;

public class MainActivity extends BaseActivity {

    private Emitter.Listener onId = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mWebSocketClient.start("bourne3");
        }
    };

    public Emitter.Listener onMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            try {
                String type = data.getString("type");
                VoipApp.setId(data.getString("from"));
                if (type.equals("invite")) {
                    Intent intent = new Intent();
                    intent.setAction(VoipReceiver.ACTION_VOIP_RECEIVER);
                    intent.setComponent(new ComponentName(VoipApp.getInstance().getPackageName(), VoipReceiver.class.getName()));
                    // 发送广播
                    VoipApp.getInstance().sendBroadcast(intent);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private WebSocketClient mWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
            }
        });

        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Need permissions for camera & microphone", 0, perms);
        }

        mWebSocketClient = WebSocketClient.instance();
        mWebSocketClient.connect(getResources().getString(R.string.web_rtc_server));
        mWebSocketClient.on("id", onId);
        mWebSocketClient.on("message", onMessage);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}