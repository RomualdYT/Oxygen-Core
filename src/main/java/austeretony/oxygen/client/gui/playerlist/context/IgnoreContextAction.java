package austeretony.oxygen.client.gui.playerlist.context;

import java.util.UUID;

import austeretony.alternateui.screen.contextmenu.AbstractContextAction;
import austeretony.alternateui.screen.core.GUIBaseElement;
import austeretony.oxygen.client.OxygenManagerClient;
import austeretony.oxygen.client.api.OxygenHelperClient;
import austeretony.oxygen.client.gui.PlayerGUIButton;
import net.minecraft.client.resources.I18n;

public class IgnoreContextAction extends AbstractContextAction {

    @Override
    protected String getName(GUIBaseElement currElement) {
        return I18n.format("oxygen.gui.action.ignore");
    }

    @Override
    protected boolean isValid(GUIBaseElement currElement) {
        UUID targetUUID = ((PlayerGUIButton) currElement).playerUUID;
        return !targetUUID.equals(OxygenHelperClient.getPlayerUUID()) 
                && OxygenHelperClient.isOnline(targetUUID)
                && !OxygenHelperClient.getPlayerData().haveFriendListEntryForUUID(targetUUID);
    }

    @Override
    protected void execute(GUIBaseElement currElement) {
        UUID targetUUID = ((PlayerGUIButton) currElement).playerUUID;
        OxygenManagerClient.instance().getFriendListManager().addToIgnoredSynced(targetUUID);
    }
}
