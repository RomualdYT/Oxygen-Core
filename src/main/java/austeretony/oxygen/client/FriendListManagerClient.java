package austeretony.oxygen.client;

import java.util.UUID;

import austeretony.oxygen.client.gui.friends.FriendsListGUIScreen;
import austeretony.oxygen.common.core.api.ClientReference;
import austeretony.oxygen.common.main.FriendListEntry;
import austeretony.oxygen.common.main.OxygenMain;
import austeretony.oxygen.common.main.OxygenPlayerData;
import austeretony.oxygen.common.network.server.SPChangeStatus;
import austeretony.oxygen.common.network.server.SPEditFriendListEntryNote;
import austeretony.oxygen.common.network.server.SPManageFriendList;
import austeretony.oxygen.common.network.server.SPRequest;

public class FriendListManagerClient {

    private final OxygenManagerClient manager;

    public FriendListManagerClient(OxygenManagerClient manager) {
        this.manager = manager;
    }

    public void openFriendsListSynced() {
        OxygenMain.network().sendToServer(new SPRequest(SPRequest.EnumRequest.OPEN_FRIENDS_LIST));
    }

    public void openFriendsListDelegated() {
        ClientReference.getMinecraft().addScheduledTask(new Runnable() {

            @Override
            public void run() {
                openFriendsList();
            }
        });
    }

    private void openFriendsList() {
        ClientReference.displayGuiScreen(new FriendsListGUIScreen());
        this.manager.getLoader().savePlayerDataDelegated();
    }

    public void changeStatusSynced(OxygenPlayerData.EnumStatus status) {
        OxygenMain.network().sendToServer(new SPChangeStatus(status));
    }

    public void downloadFriendsListDataSynced() {
        this.manager.getPlayerData().getFriendListEntries().clear();
        this.openFriendsListSynced();
    }

    public void sendFriendRequestSynced(UUID playerUUID) {
        OxygenMain.network().sendToServer(new SPManageFriendList(SPManageFriendList.EnumAction.ADD_FRIEND, playerUUID));
    }

    public void removeFriendSynced(UUID playerUUID) {
        this.manager.getPlayerData().removeFriendListEntry(playerUUID);
        OxygenMain.network().sendToServer(new SPManageFriendList(SPManageFriendList.EnumAction.REMOVE_FRIEND, playerUUID));
        this.manager.getLoader().savePlayerDataDelegated();
    }

    public void editFriendListEntryNoteSynced(UUID playerUUID, String note) {
        this.manager.getPlayerData().getFriendListEntryByUUID(playerUUID).setNote(note);
        OxygenMain.network().sendToServer(new SPEditFriendListEntryNote(playerUUID, note));
        this.manager.getLoader().savePlayerDataDelegated();
    }

    public void addToIgnoredSynced(UUID playerUUID, String username) {
        this.manager.getPlayerData().addFriendListEntry(new FriendListEntry(playerUUID, username, true).createId());
        OxygenMain.network().sendToServer(new SPManageFriendList(SPManageFriendList.EnumAction.ADD_IGNORED, playerUUID));
        this.manager.getLoader().savePlayerDataDelegated();
    }

    public void removeIgnoredSynced(UUID playerUUID) {
        this.manager.getPlayerData().removeFriendListEntry(playerUUID);
        OxygenMain.network().sendToServer(new SPManageFriendList(SPManageFriendList.EnumAction.REMOVE_IGNORED, playerUUID));
        this.manager.getLoader().savePlayerDataDelegated();
    }
}