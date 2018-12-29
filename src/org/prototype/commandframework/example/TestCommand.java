package org.prototype.commandframework.example;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.prototype.commandframework.SmartCommand;

public class TestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("this is a command executor");
        return true;
    }

    public void injectCommand() {

        SmartCommand subCommand = SmartCommand.builder()
                .subcommand()
                .usage("/ex subcommand")
                .permission("plugin.command.example.subcommand")
                .executor(new TestSubCommandExecutor())
                .build();


        SmartCommand.builder()
                .name("example")
                .aliases("ex", "e")
                .usage("/<command>")
                .permission("plugin.command.example")
                .description("example command")
                .executor(this)
                .child(subCommand, "subcommand", "sc")
                .build();

    }

}
