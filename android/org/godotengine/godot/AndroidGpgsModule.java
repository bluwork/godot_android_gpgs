package org.godotengine.godot;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class AndroidGpgsModule extends Godot.SingletonBase implements GoogleServices.BackMessageListener {

    private static final String TAG = "Android_Gpgs_Module";
    private int mInstanceId;
    private ServicesBroker broker;

    public AndroidGpgsModule(Activity activity) {
        registerClass("AndroidGpgsModule", new String[]{

                "init",

                "isConnected",

                "signIn", "signOut",

                "showAchievements", "unlockAchievement", "incrementAchievement",

                "showLeaderboard", "submitScore",

                "hasInvitation",

                "startQuickGame", "invitePlayers", "showInvitations", "leaveRoom"

        });

        broker = new ServicesBroker(activity, this);
    }

    static public Godot.SingletonBase initialize(Activity activity) {
        return new AndroidGpgsModule(activity);
    }

    public void init(final int pInstanceId) {

        mInstanceId = pInstanceId;
        //Log.d(TAG, "Script instance id: " + mInstanceId);
    }

    private void signIn() {
        broker.signIn();
    }

    private void signOut() {
        broker.signOut();
    }

    protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        broker.onActivityResult(requestCode, resultCode, data);
    }

    protected void onMainResume() {
        if (broker == null) {
            Log.d(TAG, "Facade is not initialized yet");
            return;
        }
        broker.onResume();
    }

    protected void onMainPause() {
        if (broker == null) {
            return;
        }
        broker.onPause();
    }

    private void showAchievements() {
        broker.showAchievements();
    }

    private void unlockAchievement(String achievementId) {
        broker.unlockAchievement(achievementId);
    }

    private void incrementAchievement(String achievementId, int increment_step) {
        broker.incrementAchievement(achievementId, increment_step);
    }

    private void showLeaderboard(String leaderboardId) {
        broker.showLeaderboard(leaderboardId);
    }

    private void submitScore(String leaderboardId, int score) {
        broker.submitScore(leaderboardId, score);
    }

    private void startQuickGame(int role) {

        broker.startQuickGame(role);
    }

    private void invitePlayers() {

        broker.invitePlayers();
    }

    private void leaveRoom() {
        broker.leaveRoom();
    }

    private void showInvitations() {
        broker.showInvitations();
    }

    private void isConnected() {
        broker.isConnected();
    }

    @Override
    public void propagate(String from, String what) {
        GodotLib.calldeferred(mInstanceId, "_from_services", new Object[]{from, what});
        Log.d(TAG, "Back to Godot - instance_id: " + mInstanceId + " from: " + from + ": " + what + ".");
    }
}