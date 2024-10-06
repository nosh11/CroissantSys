package com.github.sheauoian.croissantsys.command;

import com.github.sheauoian.croissantsys.CroissantSys;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public abstract class CMD implements CommandExecutor {
    private static CroissantSys plugin;
    public CMD(CroissantSys plugin) {
        if (this.getPlugin() == null) this.setPlugin(plugin);
        if (this.getInstance() == null) throw new NullPointerException("Instance is null");
        if (this.getCommandName() == null) throw new NullPointerException("Command name is null");
        this.register();
    }
    final CroissantSys getPlugin() {
        return CMD.plugin;
    }
    final void setPlugin(CroissantSys instance) {
        if (instance == null)
            throw new IllegalArgumentException("Command gg");
        CMD.plugin = instance;
    }
    public void register() {
        PluginCommand c = this.getPlugin().getCommand(this.getCommandName());
        if (c != null) {
            c.setExecutor(this.getInstance());
            if (this.getInstance() instanceof TabCompleter)
                c.setTabCompleter((TabCompleter) this.getInstance());
        }
    }
    abstract CMD getInstance();
    public abstract String getCommandName();
}