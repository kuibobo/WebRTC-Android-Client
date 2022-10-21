package webrtc.module.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONException;

import webrtc.module.R;
import webrtc.module.VoipApp;
import webrtc.module.activity.InviteActivity;
import webrtc.module.activity.JoinActivity;
import webrtc.module.util.WebSocketClient;

public class FragmentVideo extends Fragment implements View.OnClickListener {
    TextView nameTextView; // 用户昵称
    TextView descTextView;  // 状态提示用语

    View outgoingActionContainer;
    View incomingActionContainer;
    View connectedActionContainer;

    ImageView outgoingHangupImageView;
    ImageView incomingHangupImageView;
    ImageView acceptImageView;

    InviteActivity inviteActivity;

    int getLayout() {
        return R.layout.fragment_video;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        initView(view);
        init();
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        inviteActivity = (InviteActivity) getActivity();
    }

    public void initView(View view) {
        outgoingActionContainer = view.findViewById(R.id.outgoingActionContainer);
        incomingActionContainer = view.findViewById(R.id.incomingActionContainer);
        connectedActionContainer = view.findViewById(R.id.connectedActionContainer);
        nameTextView = view.findViewById(R.id.nameTextView);
        descTextView = view.findViewById(R.id.descTextView);
        acceptImageView = view.findViewById(R.id.acceptImageView);
        outgoingHangupImageView = view.findViewById(R.id.outgoingHangupImageView);
        incomingHangupImageView = view.findViewById(R.id.incomingHangupImageView);
        acceptImageView = view.findViewById(R.id.acceptImageView);

        outgoingHangupImageView.setOnClickListener(this);
        incomingHangupImageView.setOnClickListener(this);
        acceptImageView.setOnClickListener(this);

        view.findViewById(R.id.audioLayout).setVisibility(View.INVISIBLE);
        incomingActionContainer.setVisibility(View.VISIBLE);
        outgoingActionContainer.setVisibility(View.GONE);
        connectedActionContainer.setVisibility(View.GONE);
        descTextView.setText(R.string.av_video_invite);
    }

    public void init() {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // 接听
        if (id == R.id.acceptImageView) {
            if (inviteActivity != null) {
                Intent intent = new Intent(getActivity(), JoinActivity.class);
                startActivity(intent);
                try {
                    WebSocketClient.instance().sendMessage(VoipApp.getId(), "accept", null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                inviteActivity.finish();
            }
        }

        // 挂断电话
        if (id == R.id.incomingHangupImageView || id == R.id.outgoingHangupImageView || id == R.id.connectedHangupImageView) {
            if (inviteActivity != null) {
                try {
                    WebSocketClient.instance().sendMessage(VoipApp.getId(), "reject", null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                inviteActivity.finish();
            }
        }
    }
}
