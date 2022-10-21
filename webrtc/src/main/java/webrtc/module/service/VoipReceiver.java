package webrtc.module.service;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.tapadoo.alerter.Alerter;

import pub.devrel.easypermissions.EasyPermissions;
import webrtc.module.R;
import webrtc.module.VoipApp;
import webrtc.module.activity.BaseActivity;
import webrtc.module.activity.InviteActivity;
import webrtc.module.util.ActivityStackManager;
import webrtc.module.util.CallForegroundNotification;
import webrtc.module.util.Permissions;

public class VoipReceiver extends BroadcastReceiver {
    public static String ACTION_VOIP_RECEIVER = VoipApp.getInstance().getPackageName() + ".voip.Receiver";


    private static final String TAG = "VoipReceiver";
    private AsyncPlayer ringPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_VOIP_RECEIVER.equals(action)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (VoipApp.isAppRunningForeground()) {
                    onForegroundOrBeforeVersionO(true);
                } else {
                    onBackgroundAfterVersionO();
                }
            } else {
                onForegroundOrBeforeVersionO(VoipApp.isAppRunningForeground());
            }
        }
    }

    private void onBackgroundAfterVersionO() {
        BaseActivity activity = (BaseActivity) ActivityStackManager.getInstance().getTopActivity();
        // 权限检测
        String[] per= new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        boolean hasPermission = EasyPermissions.hasPermissions(activity, per);
        if (hasPermission) {
            onBackgroundHasPermission();
        } else {
            CallForegroundNotification notification = new CallForegroundNotification(VoipApp.getInstance());
            notification.sendRequestIncomingPermissionsNotification(
                    activity
            );
        }
    }

    private void onBackgroundHasPermission() {
        CallForegroundNotification notification = new CallForegroundNotification(VoipApp.getInstance());
        notification.sendIncomingCallNotification(
                VoipApp.getInstance()
        );
    }

    private void onForegroundOrBeforeVersionO(Boolean isForeGround) {
        BaseActivity activity = (BaseActivity) ActivityStackManager.getInstance().getTopActivity();
        // 权限检测
        String[] per = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        boolean hasPermission = EasyPermissions.hasPermissions(activity, per);
        Log.i(TAG, "onForegroundOrBeforeVersionO hasPermission = " + hasPermission + ", isForeGround = " + isForeGround);
        if (hasPermission) {
            onHasPermission(activity);
        } else {

            ringPlayer = new AsyncPlayer(null);
            shouldStartRing(true); //来电先响铃
            if (isForeGround) {
                Alerter.create(activity).setTitle("来电通知")
                        .setText(
                                "您收到权限来通话"
                        )
                        .enableSwipeToDismiss()
                        .setBackgroundColorRes(R.color.colorAccent) // or setBackgroundColorInt(Color.CYAN)
                        .setDuration(60 * 1000)
                        .addButton("确定", R.style.AlertButtonBgWhite, v -> {
                            Permissions.request(activity, per, integer -> {
                                shouldStopRing();
                                Log.d(TAG, "Permissions.request integer = " + integer);
                                if (integer == 0) { //权限同意
                                    onHasPermission(activity);
                                } else {
                                    onPermissionDenied();
                                }
                                Alerter.hide();
                            });
                        })
                        .addButton("取消", R.style.AlertButtonBgWhite, v -> {
                            shouldStopRing();
                            onPermissionDenied();
                            Alerter.hide();
                        }).show();
            } else {
                CallForegroundNotification notification = new CallForegroundNotification(VoipApp.getInstance());
                notification.sendRequestIncomingPermissionsNotification(
                        activity
                );
            }

        }
    }

    private void onHasPermission(Context context) {
        //以视频电话拨打，切换到音频或重走这里，结束掉上一个，防止对方挂断后，下边还有一个通话界面
        if (context instanceof InviteActivity) {
            ((InviteActivity) context).finish();
        }
        InviteActivity.openActivity(context);
    }

    // 权限拒绝
    private void onPermissionDenied() {
        Toast.makeText(VoipApp.getInstance(), "权限被拒绝，无法通话", Toast.LENGTH_SHORT).show();
    }

    private void shouldStartRing(boolean isComing) {
        if (isComing) {
            Uri uri = Uri.parse("android.resource://" + VoipApp.getInstance().getPackageName() + "/" + R.raw.incoming_call_ring);
            ringPlayer.play(VoipApp.getInstance(), uri, true, AudioManager.STREAM_RING);
        } else {
            Uri uri = Uri.parse("android.resource://" + VoipApp.getInstance().getPackageName() + "/" + R.raw.wr_ringback);
            ringPlayer.play(VoipApp.getInstance(), uri, true, AudioManager.STREAM_RING);
        }
    }

    private void shouldStopRing() {
        Log.d(TAG, "shouldStopRing begin");
        ringPlayer.stop();
    }
}
