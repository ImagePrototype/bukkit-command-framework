package org.prototype.commandframework.example;

import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        new TestCommand().injectCommand();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

}
