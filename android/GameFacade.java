package org.godotengine.godot;

import android.app.Activity;
import android.content.Intent;

import org.godotengine.godot.ServicesHelper;

public class GameFacade {

    private Activity activity;
    private ServicesHelper helper;

    public GameFacade(Activity activity, ServicesHelper.BackMessageListener messageListener) {
        this.activity = activity;
        helper = new ServicesHelper(activity);
        helper.setMessageListener(messageListener);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.signInSilently();
            }
        });
    }

    public void showAchievements() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.showAchievements();
            }
        });

    }

    public void unlockAchievement(final String achievementId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.unlockAchievement(achievementId);
            }
        });

    }

    public void incrementAchievement(final String achievementId, final int incrementStep) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.incrementAchievement(achievementId, incrementStep);
            }
        });

    }

    public void showLeaderboard(final String leaderboardId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.showLeaderboard(leaderboardId);
            }
        });
    }

    public void submitScore(final String leaderboardId, final long score) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.submitScore(leaderboardId, score);
            }
        });
    }

    public boolean hasInvitation() {
        return helper.hasInvitation();
    }

    public void startQuickGame(final int role) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.startQuickGame(role);
            }
        });

    }

    public void invitePlayers() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.invitePlayers();
            }
        });

    }

    public void showInvitations() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.showInvitations();
            }
        });

    }

    public void onActivityResult(final int requestCode, final int responseCode, final Intent intent) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.onActivityResult(requestCode, responseCode, intent);
            }
        });

    }

    public void onResume() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.onResume();
            }
        });
    }

    public void onPause() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.onPause();
            }
        });
    }

    public void signIn() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.signIn();
            }
        });
    }

    public void signOut() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.signOut();
            }
        });
    }

    public void isConnected() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helper.isConnected();
            }
        });
    }
}
