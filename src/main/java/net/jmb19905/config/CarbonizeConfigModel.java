package net.jmb19905.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RestartRequired;
import net.jmb19905.Carbonize;

@SuppressWarnings("unused")
@Modmenu(modId = Carbonize.MOD_ID)
@Config(name = Carbonize.MOD_ID, wrapperName = "CarbonizeConfig")
public class CarbonizeConfigModel {
    @RestartRequired
    public boolean moreBurnableBlocks = true;

    @RestartRequired
    public boolean burnableContainers = false;
    public boolean charcoalPile = true;
    public boolean burnCrafting = true;
    public boolean createAsh = true;
    public boolean increasedFireSpreadRange = true;
}
