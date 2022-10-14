package webrtc.module.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import pub.devrel.easypermissions.EasyPermissions;
import webrtc.module.R;
import webrtc.module.util.WebSocketClient;

/**
 * 被呼端
 */
public class JoinActivity extends AppCompatActivity {
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
                JSONObject payload = null;
                if (!type.equals("init")) {
                    payload = data.getJSONObject("payload");
                }
                // if (id == null)
                id = data.getString("from");

                if (type.equals("init")) {
                    oInit();
                } else if (type.equals("offer")) {
                    onOffer(payload);
                } else if (type.equals("answer")) {
                    onAnswer(payload);
                } else if (type.equals("candidate")) {
                    onCandidate(payload);
                } else {
                    Log.w(TAG, "the type is invalid: " + type);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void oInit() {
            mPeerConnection.createOffer(mWebRtcObserver, mMediaConstraints);
        }

        private void onOffer(JSONObject payload) throws JSONException {
            String description = payload.getString("sdp");
            mPeerConnection.setRemoteDescription(
                    mWebRtcObserver,
                    new SessionDescription(
                            SessionDescription.Type.OFFER,
                            description));
            mPeerConnection.createAnswer(mWebRtcObserver, mMediaConstraints);
        }

        private void onAnswer(JSONObject payload) throws JSONException {
            String description = payload.getString("sdp");
            mPeerConnection.setRemoteDescription(mWebRtcObserver,
                    new SessionDescription(
                            SessionDescription.Type.ANSWER,
                            description));
        }

        private void onCandidate(JSONObject payload) throws JSONException {
            IceCandidate remoteIceCandidate =
                    new IceCandidate(payload.getString("id"),
                            payload.getInt("label"),
                            payload.getString("candidate"));

            mPeerConnection.addIceCandidate(remoteIceCandidate);
        }
    };

    public class WebRtcObserver implements org.webrtc.SdpObserver, PeerConnection.Observer {

        @Override
        public void onCreateSuccess(SessionDescription sdp) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);
                mWebSocketClient.sendMessage(id, sdp.type.canonicalForm(), payload);
                mPeerConnection.setLocalDescription(this, sdp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {
        }

        // PeerConnection.Observer 实现

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.i(TAG, "onIceCandidate: " + iceCandidate);

            try {
                JSONObject message = new JSONObject();
                //message.put("userId", RTCSignalClient.getInstance().getUserId());
                //message.put("type", "candidate");
                message.put("label", iceCandidate.sdpMLineIndex);
                message.put("id", iceCandidate.sdpMid);
                message.put("candidate", iceCandidate.sdp);
                mWebSocketClient.sendMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            for (int i = 0; i < iceCandidates.length; i++) {
                Log.i(TAG, "onIceCandidatesRemoved: " + iceCandidates[i]);
            }
            mPeerConnection.removeIceCandidates(iceCandidates);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            MediaStreamTrack track = rtpReceiver.track();
            if (track instanceof VideoTrack) {
                Log.i(TAG, "onAddVideoTrack");
                VideoTrack remoteVideoTrack = (VideoTrack) track;
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(mRemoteSurfaceView);
            }
        }
    }

    private WebSocketClient mWebSocketClient;

    private static final int VIDEO_RESOLUTION_WIDTH = 1280;
    private static final int VIDEO_RESOLUTION_HEIGHT = 720;
    private static final int VIDEO_FPS = 30;
    private Context mContext;

    private static final String TAG = "JoinActivity";

    public static final String VIDEO_TRACK_ID = "1";//"ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "2";//"ARDAMSa0";

    //用于数据传输
    private PeerConnection mPeerConnection;
    private PeerConnectionFactory mPeerConnectionFactory;

    //OpenGL ES
    private EglBase mRootEglBase;
    //纹理渲染

    //继承自 surface view
    private SurfaceViewRenderer mLocalSurfaceView;
    private SurfaceViewRenderer mRemoteSurfaceView;

    private VideoTrack mVideoTrack;
    private AudioTrack mAudioTrack;
    private WebRtcObserver mWebRtcObserver;

    private String id;
    private MediaConstraints mMediaConstraints = new MediaConstraints();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        mContext = this;
        mLocalSurfaceView = findViewById(R.id.LocalSurfaceView);
        mRemoteSurfaceView = findViewById(R.id.RemoteSurfaceView);

        createPeerConnection();

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        mWebSocketClient = WebSocketClient.instance();
        mWebSocketClient.connect(url);
        mWebSocketClient.on("id", JoinActivity.this.onId);
        mWebSocketClient.on("message", JoinActivity.this.onMessage);
    }

    public void createPeerConnection() {
        // 1. 初始化的方法，必须在开始之前调用
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        mRootEglBase = EglBase.create();
        // 2. 设置编解码方式：默认方法
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        encoderFactory = new DefaultVideoEncoderFactory(
                mRootEglBase.getEglBaseContext(),
                true,
                true);
        decoderFactory = new DefaultVideoDecoderFactory(mRootEglBase.getEglBaseContext());

        // 构造Factory
        AudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(mContext).createAudioDeviceModule();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        Log.i(TAG, "Create PeerConnection ..." + Thread.currentThread());
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();

        PeerConnection.IceServer ice_server =
                PeerConnection.IceServer.builder("turn:49.232.155.214:3478")
                        .setPassword("mypasswd")
                        .setUsername("terry")
                        .createIceServer();

        iceServers.add(ice_server);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = true;
        //rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        Log.e(TAG, "createPeerConnection mPeerConnectionFactory:" + mPeerConnectionFactory + Thread.currentThread());
        mWebRtcObserver = new WebRtcObserver();
        mPeerConnection = mPeerConnectionFactory.createPeerConnection(rtcConfig, mWebRtcObserver);
        if (mPeerConnection == null) {
            Log.e(TAG, "Failed to createPeerConnection !");
            return;
        }
        initView();
    }

    private void initView() {
        // NOTE: this _must_ happen while PeerConnectionFactory is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
        mLocalSurfaceView.init(mRootEglBase.getEglBaseContext(), null);
        mLocalSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        mLocalSurfaceView.setMirror(true);
        mLocalSurfaceView.setEnableHardwareScaler(false /* enabled */);

        mRemoteSurfaceView.init(mRootEglBase.getEglBaseContext(), null);
        mRemoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        mRemoteSurfaceView.setMirror(true);
        mRemoteSurfaceView.setEnableHardwareScaler(true /* enabled */);
        mRemoteSurfaceView.setZOrderMediaOverlay(true);
        VideoCapturer mVideoCapturer = createVideoCapturer();

        SurfaceTextureHelper mSurfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), mRootEglBase.getEglBaseContext());
        VideoSource videoSource = mPeerConnectionFactory.createVideoSource(false);
        mVideoCapturer.initialize(mSurfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        mVideoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS);

        mVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        mVideoTrack.setEnabled(true);
        mVideoTrack.addSink(mLocalSurfaceView);
        //语音
        MediaConstraints audioConstraints = new MediaConstraints();
        //回声消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        //自动增益
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        //高音过滤
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        //噪音处理
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        AudioSource audioSource = mPeerConnectionFactory.createAudioSource(audioConstraints);
        mAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        mAudioTrack.setEnabled(true);


        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        mPeerConnection.addTrack(mVideoTrack, mediaStreamLabels);
        mPeerConnection.addTrack(mAudioTrack, mediaStreamLabels);

    }


    private VideoCapturer createVideoCapturer() {
        if (Camera2Enumerator.isSupported(this)) {
            return createCameraCapturer(new Camera2Enumerator(this));
        } else {
            return createCameraCapturer(new Camera1Enumerator(true));
        }
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

}
