package austeretony.oxygen.client.gui.notifications;

import austeretony.alternateui.screen.core.AbstractGUIScreen;
import austeretony.alternateui.screen.core.AbstractGUISection;
import austeretony.alternateui.screen.core.GUIBaseElement;
import austeretony.alternateui.screen.core.GUIWorkspace;
import austeretony.alternateui.util.EnumGUIAlignment;
import austeretony.oxygen.common.main.OxygenMain;
import net.minecraft.util.ResourceLocation;

public class NotificationsGUIScreen extends AbstractGUIScreen {

    public static final ResourceLocation 
    CHECK_ICONS = new ResourceLocation(OxygenMain.MODID, "textures/gui/check_icons.png"),
    CROSS_ICONS = new ResourceLocation(OxygenMain.MODID, "textures/gui/cross_icons.png");

    protected AbstractGUISection mainSection;

    @Override
    protected GUIWorkspace initWorkspace() {
        return new GUIWorkspace(this, 217, 205).setAlignment(EnumGUIAlignment.RIGHT, - 10, 0);
    }

    @Override
    protected void initSections() {
        this.mainSection = this.getWorkspace().initSection(new NotificationsGUISection(this));        
    }

    @Override
    protected AbstractGUISection getDefaultSection() {
        return this.mainSection;
    }

    @Override
    public void handleElementClick(AbstractGUISection section, GUIBaseElement element) {}

    @Override
    protected boolean doesGUIPauseGame() {
        return false;
    }
}
