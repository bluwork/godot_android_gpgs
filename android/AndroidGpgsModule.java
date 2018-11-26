package org.godotengine.godot;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.godotengine.godot.ServicesFacade;

public class AndroidGpgsModule extends Godot.SingletonBase implements org.godotengine.godot.Services.BackMessageListener {

    private static final String TAG = "Android_Gpgs_Module";
    private int mInstanceId;
    private ServicesFacade facade;

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

        facade = new ServicesFacade(activity, this);
    }

    static public Godot.SingletonBase initialize(Activity activity) {
        return new AndroidGpgsModule(activity);
    }

    public void init(final int pInstanceId) {

        mInstanceId = pInstanceId;
        //Log.d(TAG, "Script instance id: " + mInstanceId);
    }

    private void signIn() {
        facade.signIn();
    }

    private void signOut() {
        facade.signOut();
    }

    protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        facade.onActivityResult(requestCode, resultCode, data);
    }

    protected void onMainResume() {
        if (facade == null) {
            Log.d(TAG, "Facade is not initialized yet");
            return;
        }
        facade.onResume();
    }

    protected void onMainPause() {
        if (facade == null) {
            return;
        }
        facade.onPause();
    }

    private void showAchievements() {
        facade.showAchievements();
    }

    private void unlockAchievement(String achievementId) {
        facade.unlockAchievement(achievementId);
    }

    private void incrementAchievement(String achievementId, int increment_step) {
        facade.incrementAchievement(achievementId, increment_step);
    }

    private void showLeaderboard(String leaderboardId) {
        facade.showLeaderboard(leaderboardId);
    }

    private void submitScore(String leaderboardId, int score) {
        facade.submitScore(leaderboardId, score);
    }

    private void startQuickGame(int role) {

        facade.startQuickGame(role);
    }

    private void invitePlayers() {

        facade.invitePlayers();
    }

    private void leaveRoom() {
        facade.leaveRoom();
    }

    private void showInvitations() {
        facade.showInvitations();
    }

    private void isConnected() {
        facade.isConnected();
    }

    @Override
    public void propagate(String from, String what) {
        GodotLib.calldeferred(mInstanceId, "_from_services", new Object[]{from, what});
        Log.d(TAG, "Back to Godot - instance_id: " + mInstanceId + " from: " + from + ": " + what + ".");
    }
}