package org.godotengine.godot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GoogleServices {

    private final String TAG = "GoogleServices";
    private final String RTM = "RealTimeMultiplayer";

    private final int RC_SING_IN = 5001;
    private final int RC_ACHIEVEMENT_UI = 9001;
    private final int RC_SELECT_PLAYERS = 9006;
    private final int RC_WAITING_ROOM = 9007;
    private final int RC_INVITATION_INBOX = 9008;

    private Activity activity;
    private Context context;

    private AchievementsClient achievementsClient;
    private LeaderboardsClient leaderboardsClient;

    private PlayersClient playersClient;
    private RealTimeMultiplayerClient realTimeMultiplayerClient;
    private InvitationsClient invitationsClient;
    private GamesClient gamesClient;
    private GoogleSignInClient signInClient;


    private Invitation invitation;
    private String localPlayerId = null;
    private ArrayList<Participant> mParticipants = null;
    private Room mRoom = null;
    private byte[] mMessageBuffer;
    private RoomConfig mJoinedRoomConfig;

    private BackMessageListener messageListener;
    private OnRealTimeMessageReceivedListener mMessageReceivedHandler = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage rtm) {
            byte[] buf = rtm.getMessageData();
            String sender = rtm.getSenderParticipantId();
            propagate("rt_multiplayer", "message_received");
        }
    };
    private RoomUpdateCallback mRoomUpdateCallback = new RoomUpdateCallback() {
        @Override
        public void onRoomCreated(int code, @Nullable Room room) {
            //From docs: Called when the client attempts to create a real-time room.
            // Update UI and internal state based on room updates.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.d(RTM, "Room " + room.getRoomId() + " created.");
                mRoom = room;
            } else {
                Log.w(RTM, "Error creating room: " + code);
            }
        }

        @Override
        public void onJoinedRoom(int code, @Nullable Room room) {
            // From docs: Called when the client attempts to join a real-time room.
            // Update UI and internal state based on room updates.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.d(RTM, "Room " + room.getRoomId() + " joined.");
                mRoom = room;
            } else {
                Log.w(RTM, "Error joining room: " + code);
            }
        }

        @Override
        public void onLeftRoom(int code, @NonNull String roomId) {
            // From docs: Called when the client attempts to leaves the real-time room.
            Log.d(RTM, "*** onLeftRoom() *** Room id: " + roomId + ".");
            // we have left the room; return to main screen.
            Log.d(RTM, "onLeftRoom, code " + code);
            mParticipants = null;
        }

        @Override
        public void onRoomConnected(int code, @Nullable Room room) {
            // From docs: Called when all the participants in a real-time room are fully connected.
            if (code == GamesCallbackStatusCodes.OK && room != null) {
                Log.d(RTM, "*** onRoomConnected() *** Room " + room.getRoomId() + " connected.");
                for (Participant p : room.getParticipants()) {
                    Log.d(RTM, "\t\tParticipant: "  + p);
                }
            } else {
                Log.w(RTM, "Error connecting to room: " + code);
            }
        }
    };
    private RoomStatusUpdateCallback mRoomStatusCallbackHandler = new RoomStatusUpdateCallback() {
        @Override
        public void onRoomConnecting(@Nullable Room room) {
            //From docs: Called when one or more participants have joined the room and have started the process of establishing peer connections.
            // Update the UI status since we are in the process of connecting to a specific room.
            if (room != null ) {
                Log.d(RTM, "*** onRoomConnecting() *** " + room.getRoomId());
            } else {
                Log.d(RTM, "*** onRoomConnecting() *** " + " Room is null");
            }

        }

        @Override
        public void onRoomAutoMatching(@Nullable Room room) {
            // From docs: Called when the server has started the process of auto-matching.
            // Update the UI status since we are in the process of matching other players.
            if (room != null ) {
                Log.d(RTM, "*** onRoomAutomatching() *** " + room.getRoomId());
            } else {
                Log.d(RTM, "*** onRoomAutomatching() *** " + " Room is null");
            }
        }

        @Override
        public void onPeerInvitedToRoom(@Nullable Room room, @NonNull List<String> participantIds) {
            // From docs: Called when one or more peers are invited to a room.
            // Update the UI status since we are in the process of matching other players.
            if (room != null ) {
                Log.d(RTM, "*** onPeerInvitedToRoom() *** " + " Room id: " + room.getRoomId());
                for (String p : participantIds) {
                    Log.d(RTM, "\t\tParticipant : " + p);
                }
            } else {
                Log.d(RTM, "*** onPeerInvitedToRoom() *** " + " Room is null");
            }
        }

        @Override
        public void onPeerDeclined(@Nullable Room room, @NonNull List<String> participantIds) {
            // From docs: Called when one or more peers decline the invitation to a room.
            if (room != null ) {
                Log.d(RTM, "*** onPeerDeclined() *** " + " Room id: " + room.getRoomId());
                for (String p : participantIds) {
                    Log.d(RTM, "\t\tParticipant : " + p);
                }
            } else {
                Log.d(RTM, "*** onPeerDeclined() *** " + " Room is null");
            }
        }

        @Override
        public void onPeerJoined(@Nullable Room room, @NonNull List<String> participantIds) {
            // From docs: Called when one or more peer participants join a room.
            // Update UI status indicating new players have joined!
            if (room != null ) {
                Log.d(RTM, "*** onPeerJoined() *** " + " Room id: " + room.getRoomId());
                for (String p : participantIds) {
                    Log.d(RTM, "\t\tParticipant : " + p);
                }
            } else {
                Log.d(RTM, "*** onPeerJoined() *** " + " Room is null");
            }
        }

        @Override
        public void onPeerLeft(@Nullable Room room, @NonNull List<String> participantIds) {
            // From docs: Called when one or more peer participant leave a room.
            if (room != null ) {
                Log.d(RTM, "*** onPeerLeft() *** " + "Room id: " + room.getRoomId());
                for (String p : participantIds) {
                    Log.d(RTM, "\t\tParticipant : " + p);
                }
            } else {
                Log.d(RTM, "*** onPeerLeft() *** " + "Room is null");
            }
        }

        @Override
        public void onConnectedToRoom(@Nullable Room room) {
            // From docs: Called when the client is connected to the connected set in a room.
            if (room != null ) {
                Log.d(RTM, "*** onConnectedToRoom() *** " + "Room id: " + room.getRoomId());
            } else {
                Log.d(RTM, "*** onConnectedToRoom() *** " + "Room is null");
            }
        }

        @Override
        public void onDisconnectedFromRoom(@Nullable Room room) {
            // From docs: Called when the client is disconnected from the connected set in a room.
            if (room != null ) {
                Log.d(RTM, "*** onDisconnectedFromRoom() *** " + "Room id: " + room.getRoomId());

            } else {
                Log.d(RTM, "*** onDisconnectedFromRoom() *** " + "Room is null");
            }
        }

        @Override
        public void onPeersConnected(@Nullable Room room, @NonNull List<String> participantIds) {
            // From docs: Called when one or more peer participants are connected to a room.
            if (room != null ) {
                Log.d(RTM, "*** onPeersConnected() *** " + "Room id: " + room.getRoomId());
                for (String p : participantIds) {
                    Log.d(RTM, "\t\tParticipant : " + p);
                }
            } else {
                Log.d(RTM, "*** onPeersConnected() *** " + "Room is null");
            }

        }

        @Override
        public void onPeersDisconnected(@Nullable Room room, @NonNull List<String> participantIds) {
            // From docs: Called when one or more peer participants are disconnected from a room.
            if (room != null ) {
                Log.d(RTM, "*** onPeersDisconnected() *** " + "Room id: " + room.getRoomId());
                for (String p : participantIds) {
                    Log.d(RTM, "\t\tParticipant : " + p);
                }
            } else {
                Log.d(RTM, "*** onPeersDisconnected() *** " + "Room is null");
            }
        }

        @Override
        public void onP2PConnected(@NonNull String participantId) {
            // From docs: Called when the client is successfully connected to a peer participant.
            // Update status due to new peer to peer connection.
            Log.d(RTM, "*** onP2PConnected() ***" + " Participant Id: " + participantId);
        }

        @Override
        public void onP2PDisconnected(@NonNull String participantId) {
            // From docs: Called when client gets disconnected from a peer participant.
            // Update status due to  peer to peer connection being disconnected.
            Log.d(RTM, "*** onP2PDisconnected() ***" + " Participant Id: " + participantId);
        }

    };
    private InvitationCallback invitationCallback = new InvitationCallback() {
        // Called when we get an invitation to play a game. We react by showing that to the user.
        @Override
        public void onInvitationReceived(@NonNull Invitation invitation) {
            // We got an invitation to play a game! So, store it in
            // mIncomingInvitationId
            // and show the popup on the screen.
            String invitationId = invitation.getInvitationId();
            Log.d(TAG, "*** onInvitationReceived() *** " + "Inviter: " + invitation.getInviter().getDisplayName());
            propagate("rt_multiplayer", "invitation_received");
        }

        @Override
        public void onInvitationRemoved(@NonNull String invitationId) {
            propagate("rt_multiplayer", "invitation_removed");
        }
    };

    public GoogleServices(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    private void onConnected() {

        Log.d(TAG, "onConnected() called. Setting invitation listener ...");

        getInvitationsClient().registerInvitationCallback(invitationCallback);
        checkForInvitation();

        Log.d(TAG, "Clients initialized");

        propagate("connected", "yes");
    }

    private void onDisconnected() {
        Log.d(TAG, "onDisconnected() called.");
        achievementsClient = null;
        leaderboardsClient = null;
        playersClient = null;
        realTimeMultiplayerClient = null;
        invitationsClient = null;
        gamesClient = null;
        propagate("connected", "no");
    }

    public void signIn() {
        Intent intent = getSignInClient().getSignInIntent();
        activity.startActivityForResult(intent, RC_SING_IN);
    }

    public void signOut() {
        Log.d(TAG, "signOut()");

        if (!isSignedIn()) {
            Log.w(TAG, "signOut() called, but was not signed in!");
            return;
        }
        getSignInClient().signOut().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                boolean successful = task.isSuccessful();
                Log.d(TAG, "signOut(): " + (successful ? "success" : "failed"));
                onDisconnected();
            }
        });

    }

    public void silentlyIn() {
        Log.d(TAG, "Silently sign in");
        getSignInClient().silentSignIn().addOnCompleteListener(activity,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            onConnected();
                        } else {
                            Log.d(TAG, "Unsuccessful sign in.", task.getException());
                            if (isServicesDenied()) {
                                Log.d(TAG, "Player denied using services.");
                            } else {
                                Log.d(TAG, "Start in sign in activity.");
                                signIn();
                            }
                        }
                    }
                });
    }

    public boolean isSignedIn() {

        return getGoogleAccount() != null;
    }

    public void onResume() {
        silentlyIn();
    }

    public void onPause() {

        if (isSignedIn()) {
            Log.d(TAG, "onPause called. Removing invitation listener.");
            getInvitationsClient().unregisterInvitationCallback(invitationCallback);
        }

    }

    public void onActivityResult(int requestCode, int responseCode, Intent intent) {

        switch (requestCode) {

            case RC_SING_IN:

                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
                if (result.isSuccess()) {
                    Log.d(TAG, "Connected from Sing In Activity");
                    onConnected();
                } else {
                    String message = result.getStatus().getStatusMessage();
                    if (message == null || message.isEmpty()) {
                        message = "Response code: " + responseCode;
                    }
                    if (responseCode == Activity.RESULT_CANCELED) {
                        rememberServiceDenial();
                    }
                    Log.d(TAG, "Login error: " + message);
                }
                break;

            case RC_SELECT_PLAYERS:
                if (responseCode != Activity.RESULT_OK) {
                    Log.w(TAG, "*** select players UI cancelled, " + responseCode);
                    // switchToMainScreen();
                } else {
                    handleSelectPlayers(intent);
                }
                break;

            case RC_INVITATION_INBOX:
                if (responseCode != Activity.RESULT_OK) {
                    Log.w(TAG, "*** invitation inbox UI cancelled, " + responseCode);
                    // switchToMainScreen();
                    return;
                }

                Log.d(TAG, "Invitation inbox UI succeeded.");
                Invitation inv = intent.getExtras().getParcelable(
                        Multiplayer.EXTRA_INVITATION);

                // accept invitation
                acceptInviteToRoom(inv.getInvitationId());
                break;

            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d("MultiplayerManager",
                            "Starting game (waiting room returned OK).");
                    startGame();
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    leaveRoom();
                }
                break;

        }
    }

    private void rememberServiceDenial() {
        activity.getPreferences(Activity.MODE_PRIVATE)
                .edit().putBoolean("services_denied", true).apply();
    }

    private boolean isServicesDenied() {
        return activity.getPreferences(Activity.MODE_PRIVATE).getBoolean("services_denied", false);
    }

    public void showAchievements() {
        if (isSignedIn()) {
            getAchievementsClient().getAchievementsIntent().addOnSuccessListener(new OnSuccessListener<Intent>() {
                @Override
                public void onSuccess(Intent intent) {
                    activity.startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                }
            });
        } else {
            signIn();
        }

    }

    public void unlockAchievement(final String achievementId) {

        if (isSignedIn()) {
            getAchievementsClient().unlock(achievementId);
        } else {
            Log.w(TAG, "Achievements: Client is not connected. Unlock failed.");
        }

    }

    public void incrementAchievement(final String achievementId, final int incrementStep) {

        if (isSignedIn()) {
            getAchievementsClient().increment(achievementId, incrementStep);
        } else {
            Log.w(TAG, "Achievements: Client is not connected. "
                    + "Achievement increment failed.");
        }

    }

    public void submitScore(final String leaderboardId, final long score) {

        if (isSignedIn()) {
            getLeaderboardsClient().submitScore(leaderboardId, score);
        } else {
            Log.w(TAG, "Leaderboards: Client is not connected. Submit score failed.");
        }

    }

    public void showLeaderboard(final String leaderboardId) {

        if (isSignedIn()) {
            getLeaderboardsClient().getLeaderboardIntent(leaderboardId);
        } else {
            Log.w(TAG,
                    "Leaderboards: Client is not connected. Cannot show leaderboard: "
                            + leaderboardId);
            signIn();
        }

    }

    public void invitePlayers() {

        if (isSignedIn()) {
            // launch the player selection screen
            // minimum: 1 other player; maximum: 3 other players
            getRealTimeMultiplayerClient()
                    .getSelectOpponentsIntent(1, 3, true)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            activity.startActivityForResult(intent, RC_SELECT_PLAYERS);
                        }
                    });
        } else {
            signIn();
        }

    }

    public void showInvitations() {

        if (isSignedIn()) {
            getInvitationsClient()
                    .getInvitationInboxIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            activity.startActivityForResult(intent, RC_INVITATION_INBOX);
                        }
                    });

        } else {
            signIn();
        }

    }

    public boolean hasInvitation() {
        if (isSignedIn()) {
            //TODO Add invitiation receiving logic
            return invitation != null;
        }
        return false;
    }

    public void startQuickGame(final int role) {

        Log.d(RTM, "Starting quick game");
        if (isSignedIn()) {
            final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
            Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS, MAX_OPPONENTS, role);

            // build the room config:
            RoomConfig roomConfig =
                    RoomConfig.builder(mRoomUpdateCallback)
                            .setOnMessageReceivedListener(mMessageReceivedHandler)
                            .setRoomStatusUpdateCallback(mRoomStatusCallbackHandler)
                            .setAutoMatchCriteria(autoMatchCriteria)
                            .build();

            // Save the roomConfig so we can use it if we call leave().
            mJoinedRoomConfig = roomConfig;
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getRealTimeMultiplayerClient().create(mJoinedRoomConfig);
            Log.d(RTM, "Creating room");
            propagate("rt_multiplayer", "creating_room");
        } else {
            signIn();
        }

    }

    private void checkForInvitation() {
        getGamesClient().getActivationHint()
                .addOnSuccessListener(
                        new OnSuccessListener<Bundle>() {
                            @Override
                            public void onSuccess(Bundle bundle) {
                                if (bundle != null) {
                                    invitation = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
                                    if (invitation != null) {
                                        acceptInviteToRoom(invitation.getInvitationId());
                                    }
                                }
                            }
                        }
                );
    }

    private void showWaitingRoom(Room room, int maxPlayersToStartGame) {
        getRealTimeMultiplayerClient().getWaitingRoomIntent(room, maxPlayersToStartGame)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        activity.startActivityForResult(intent, RC_WAITING_ROOM);
                    }
                });
    }

    public void handleSelectPlayers(Intent data) {

        Log.d(TAG, "Select players UI succeeded.");

        // Get the invitee list.
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

        // Get Automatch criteria.
        int minAutoPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

        // Create the room configuration.
        RoomConfig.Builder roomBuilder = RoomConfig.builder(mRoomUpdateCallback)
                .setOnMessageReceivedListener(mMessageReceivedHandler)
                .setRoomStatusUpdateCallback(mRoomStatusCallbackHandler)
                .addPlayersToInvite(invitees);
        if (minAutoPlayers > 0) {
            roomBuilder.setAutoMatchCriteria(
                    RoomConfig.createAutoMatchCriteria(minAutoPlayers, maxAutoPlayers, 0));
        }

        // Save the roomConfig so we can use it if we call leave().
        mJoinedRoomConfig = roomBuilder.build();
        getRealTimeMultiplayerClient().create(mJoinedRoomConfig);

        Log.d(TAG, "Room created, waiting for it to be ready...");
        propagate("rt_multiplayer", "creating_room");
    }

    public void acceptInviteToRoom(String invId) {

        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder builder = RoomConfig.builder(mRoomUpdateCallback)
                .setInvitationIdToAccept(invId);
        mJoinedRoomConfig = builder.build();
        getRealTimeMultiplayerClient().join(mJoinedRoomConfig);
        propagate("rt_multiplayer", "joining_room");
    }

    public void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        if (mRoom != null) {
            Log.d(TAG, "Leaving room " + mRoom.getRoomId() + ".");
            getRealTimeMultiplayerClient().leave(mJoinedRoomConfig, mRoom.getRoomId());
            mRoom = null;
            localPlayerId = null;
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }
        propagate("rt_multiplayer", "leaving_room");
    }

    public void startGame() {

        Log.d(TAG, "Starting game...");
        propagate("rt_multiplayer", "start_game");
    }

    private void broadcastUnreliable() {

    }

    private void broadcastReliable() {
        if (mMessageBuffer.length > 0) {
            for (String participantId : mRoom.getParticipantIds()) {
                if (!participantId.equals(localPlayerId)) {
                    Task<Integer> task = getRealTimeMultiplayerClient()
                            .sendReliableMessage(mMessageBuffer,
                                    mRoom.getRoomId(),
                                    participantId,
                                    handleMessageSentCallback)
                            .addOnCompleteListener(new OnCompleteListener<Integer>() {
                                @Override
                                public void onComplete(@NonNull Task<Integer> task) {
                                    // Keep track of which messages are sent, if desired.
                                    recordMessageToken(task.getResult());
                                }
                            });
                }
            }
        }

    }

    HashSet<Integer> pendingMessageSet = new HashSet<>();

    synchronized void recordMessageToken(int tokenId) {
        pendingMessageSet.add(tokenId);
    }

    private RealTimeMultiplayerClient.ReliableMessageSentCallback handleMessageSentCallback =
            new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                @Override
                public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientId) {
                    // handle the message being sent.
                    synchronized (this) {
                        pendingMessageSet.remove(tokenId);
                    }
                }
            };

    private void propagate(String from, String what) {
        messageListener.propagate(from, what);
    }

    public void isConnected() {
        if (isSignedIn()) {
            propagate("connected", "yes");
        } else {
            propagate("connected", "no");
        }
    }

    private GoogleSignInAccount getGoogleAccount() {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    private GoogleSignInClient getSignInClient() {
        if (signInClient == null) {
            signInClient = GoogleSignIn.getClient(activity,
                    GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        }
        return signInClient;
    }

    private AchievementsClient getAchievementsClient() {
        if (achievementsClient == null) {
            achievementsClient = Games.getAchievementsClient(context, getGoogleAccount());
        }
        return achievementsClient;
    }

    private LeaderboardsClient getLeaderboardsClient() {
        if (leaderboardsClient == null) {
            leaderboardsClient = Games.getLeaderboardsClient(context, getGoogleAccount());
        }
        return leaderboardsClient;
    }

    private PlayersClient getPlayersClient() {
        if (playersClient == null) {
            playersClient = Games.getPlayersClient(context, getGoogleAccount());
        }
        return playersClient;
    }

    private RealTimeMultiplayerClient getRealTimeMultiplayerClient() {
        if (realTimeMultiplayerClient == null) {
            realTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(context, getGoogleAccount());
        }
        return realTimeMultiplayerClient;
    }

    private InvitationsClient getInvitationsClient() {
        if (invitationsClient == null) {
            invitationsClient = Games.getInvitationsClient(context, getGoogleAccount());
        }
        return invitationsClient;
    }

    private GamesClient getGamesClient() {
        if (gamesClient == null) {
            gamesClient = Games.getGamesClient(context, getGoogleAccount());
        }
        return gamesClient;
    }

    public void setMessageListener(BackMessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public interface BackMessageListener {
        void propagate(String from, String what);
    }
}