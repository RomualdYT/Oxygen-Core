package austeretony.oxygen_core.client.gui.privileges.management.roles.context;

import austeretony.alternateui.screen.core.GUIBaseElement;
import austeretony.oxygen_core.client.api.ClientReference;
import austeretony.oxygen_core.client.gui.elements.OxygenContextMenu.OxygenContextMenuAction;
import austeretony.oxygen_core.client.gui.privileges.management.RolesSection;

public class RemoveRoleFromPlayerContextAction implements OxygenContextMenuAction {

    @Override
    public String getLocalizedName(GUIBaseElement currElement) {
        return ClientReference.localize("oxygen_core.gui.privileges.management.removeRoleFromPlayer");
    }

    @Override
    public boolean isValid(GUIBaseElement currElement) {
        return true;
    }

    @Override
    public void execute(GUIBaseElement currElement) {
        ((RolesSection) currElement.getScreen().getWorkspace().getCurrentSection()).openRemoveRoleFromPlayerCallback();
    }
}
