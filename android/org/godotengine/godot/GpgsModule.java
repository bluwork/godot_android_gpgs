package org.godotengine.godot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import javax.microedition.khronos.opengles.GL10;

public class GpgsModule extends Godot.SingletonBase {

    private static final String TAG = "PlayServices";

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_ACHIEVEMENT_UI = 9002;
    private static final int RC_LEADERBOARD_UI = 9003;


    protected Activity appActivity;
    protected Context appContext;

    private Godot activity = null;
    private int instanceId = 0;

    private LeaderboardsClient leaderboardsClient;
    private AchievementsClient achievementsClient;


    public GpgsModule(Activity pActivity) {
        registerClass("PlayServices", new String[]{

                "init",

                "isSignedIn",

                "signIn", "signOut",

                "showAchievements", "unlockAchievement", "incrementAchievement",

                "showLeaderboard", "submitScore",

        });

        this.appActivity = pActivity;
        this.appContext = appActivity.getApplicationContext();
        this.activity = (Godot) pActivity;

    }

    static public Godot.SingletonBase initialize(Activity activity) {
        return new GpgsModule(activity);
    }

    public void init(final int pInstanceId) {
        instanceId = pInstanceId;
        //Log.d(TAG, String.format("Instance id: %s", instanceId));
        signInSilently();
    }

    private void signIn() {
        startSignInIntent();
    }

    private void signOut() {
        saveLoginState(false);
        GoogleSignInClient signInClient = GoogleSignIn.getClient(appContext,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        signInClient.signOut().addOnCompleteListener(appActivity,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        onDisconnected();
                    }
                });
    }


    private void showAchievements() {
        if (achievementsClient != null) {
            achievementsClient.getAchievementsIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            appActivity.startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                        }
                    });
        } else {
            startSignInIntent();
        }

    }

    private void unlockAchievement(String achievementId) {
        if (achievementsClient != null) {
            achievementsClient.unlock(achievementId);
        } else {
            startSignInIntent();
        }
    }

    private void incrementAchievement(String achievementId, int increment_step) {
        if (achievementsClient != null) {
            achievementsClient.increment(achievementId, increment_step);
        } else {
            startSignInIntent();
        }
    }


    private void showLeaderboard(String leaderboardId) {
        if (leaderboardsClient != null) {
            leaderboardsClient.getLeaderboardIntent(leaderboardId)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            activity.startActivityForResult(intent, RC_LEADERBOARD_UI);
                        }
                    });
        } else {
            startSignInIntent();
        }

    }

    private void submitScore(String leaderboardId, int score) {
        if (leaderboardsClient != null) {
            leaderboardsClient.submitScore(leaderboardId, score);
        } else {
            startSignInIntent();
        }

    }

    private void signInSilently() {

        GoogleSignInOptions signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN;
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(appContext);

        if (GoogleSignIn.hasPermissions(account, signInOptions.getScopeArray())) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            onConnected(account);
            //Log.d(TAG, "Signed in.");
        } else {
            // Haven't been signed-in before. Try the silent sign-in first.
            GoogleSignInClient signInClient = GoogleSignIn.getClient(appActivity, signInOptions);
            signInClient
                    .silentSignIn()
                    .addOnCompleteListener(
                            appActivity,
                            new OnCompleteListener<GoogleSignInAccount>() {
                                @Override
                                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                                    if (task.isSuccessful()) {
                                        // The signed in account is stored in the task's result.
                                        onConnected(task.getResult());
                                    } else {
                                        //Log.d(TAG, "Not Signed in.");
                                    }
                                }
                            })
                    .addOnFailureListener(appActivity, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Log.d(TAG, "Sign in  failure. E: " + e.toString());
                        }
                    });
        }
    }

    private void startSignInIntent() {
        saveLoginState(true);
        GoogleSignInClient signInClient = GoogleSignIn.getClient(appActivity,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        Intent intent = signInClient.getSignInIntent();
        appActivity.startActivityForResult(intent, RC_SIGN_IN);

    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(appContext) != null;
    }

    private void onConnected(GoogleSignInAccount account) {
        //Log.d(TAG, "Client connected.");
        achievementsClient = Games.getAchievementsClient(appContext, account);
        leaderboardsClient = Games.getLeaderboardsClient(appContext, account);
        propagate("connected", "yes");
    }

    private void onDisconnected() {
        //Log.d(TAG, "Client disconnected.");
        achievementsClient = null;
        leaderboardsClient = null;
        propagate("connected", "no");
    }


    private void saveLoginState(boolean allowed) {
        appActivity.getSharedPreferences("gpgs", Context.MODE_PRIVATE)
                .edit().putBoolean("silent_login", allowed).apply();
    }

    private boolean canLogin() {
        return appActivity.getSharedPreferences("gpgs", Context.MODE_PRIVATE).getBoolean("silent_login", true);
    }
    // Forwarded callbacks you can reimplement, as SDKs often need them.

    protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // The signed in account is stored in the result.
                onConnected(result.getSignInAccount());
                //Log.d(TAG, "Signed after explicit call.");
            } else {

                //Log.d(TAG, "Not signed in after explicit call.");
            }
        }
        if (requestCode == RC_ACHIEVEMENT_UI || requestCode == RC_LEADERBOARD_UI) {
            if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                //Log.d(TAG, "User explicitly logged out from GPGS. Saving decision...");
                saveLoginState(false);
                onDisconnected();
            }
        }

    }

    protected void onMainRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    }

    protected void onMainPause() {

    }

    protected void onMainResume() {
        //Log.d(TAG, "On main resume.");
        if (canLogin()) {
            signInSilently();
        }

    }

    protected void onMainDestroy() {

    }

    protected void onGLDrawFrame(GL10 gl) {

    }

    protected void onGLSurfaceChanged(GL10 gl, int width, int height) {
    } // Singletons will always miss first 'onGLSurfaceChanged' call.

    public void propagate(String from, String what) {
        GodotLib.calldeferred(instanceId, "_from_services", new Object[]{from, what});
        //Log.d(TAG, "Back to Godot - instance_id: " + instanceId + " from: " + from + ": " + what + ".");
    }

}
