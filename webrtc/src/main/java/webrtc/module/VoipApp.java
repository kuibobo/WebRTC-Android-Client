package webrtc.module;

import android.app.ActivityManager;
import android.app.Application;

import java.util.List;

public class VoipApp extends Application {

    private static String id;
    private static VoipApp app;

    public static VoipApp getInstance() {
        return app;
    }

    public static void setId(String id) {
        VoipApp.id = id;
    }

    public static String getId() {
        return VoipApp.id;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static boolean isAppRunningForeground() {
        ActivityManager activityManager =
                (ActivityManager) app.getSystemService(Application.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : runningAppProcesses) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(app.getApplicationInfo().processName))
                    return true;
            }
        }
        return false;
    }
}
