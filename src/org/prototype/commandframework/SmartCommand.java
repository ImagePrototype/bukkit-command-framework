package org.prototype.commandframework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

public final class SmartCommand implements CommandExecutor {

    private static CommandMap commandMap;

    private final String commandLabel, description, usage, plugin, permission, permissionMessage;
    private final Predicate<CommandSender> predicate;
    private final CommandExecutor executor;
    private final List<String> aliases;
    private final int minArgs, maxArgs;
    private final boolean onlyPlayer;
    private final Map<Set<String>, SmartCommand> childCommandMap;

    private SmartCommand(String commandLabel, String description, String usage, List<String> aliases, String plugin, String permission, String permissionMessage, Predicate<CommandSender> predicate, CommandExecutor executor, int minArgs, int maxArgs, boolean onlyPlayer, Map<Set<String>, SmartCommand> childCommandMap) {
        this.commandLabel = commandLabel;
        this.description = description;
        this.usage = usage;
        this.aliases = aliases;
        this.plugin = plugin;
        this.permission = permission;
        //System.out.println("Permission: " + this.permission);
        //System.out.println("Usage: " + this.usage);
        this.permissionMessage = permissionMessage;
        this.predicate = predicate;
        this.executor = executor;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.onlyPlayer = onlyPlayer;
        this.childCommandMap = childCommandMap;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args) {

        if ((StringUtils.isNotEmpty(this.permission)) && (!sender.hasPermission(this.permission))) {
            sender.sendMessage(this.permissionMessage);
            return true;
        }

        if ((this.predicate != null) && (!this.predicate.test(sender))) {
            sender.sendMessage(this.permissionMessage);
            return true;
        }

        if (this.onlyPlayer) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Sorry, but you aren't a player.");
                return true;
            }
        }

        if ((this.minArgs >= 0) && (args.length < this.minArgs)) {
            sender.sendMessage(this.usage.replace("<command>", commandLabel));
            return true;
        }

        if ((this.maxArgs >= 0) && (args.length > this.maxArgs)) {
            sender.sendMessage(this.usage.replace("<command>", commandLabel));
            return true;
        }

        if ((args.length != 0) && (this.childCommandMap != null) && (!this.childCommandMap.isEmpty())) {
            final SmartCommand subCommand = getSubCommand(args[0]);
            if (subCommand != null) {
                return subCommand.onCommand(sender, command, commandLabel, fixArgs(args));
            }
        }

        return this.executor.onCommand(sender, command, commandLabel, args);
    }

    private SmartCommand getSubCommand(final String subcommand) {
        return this.childCommandMap.entrySet().stream()
                .filter(entry -> entry.getKey().stream().anyMatch(alias -> alias.equalsIgnoreCase(subcommand)))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    //Arrays.stream(args).skip(1).toArray(String[]::new))
    private String[] fixArgs(String[] args) {
        //final String[] subArgs = new String[args.length - 1];
        //System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        return Arrays.stream(args).skip(1).toArray(String[]::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    public final static class Builder {

        private String commandLabel, description = "", usage = "/<command>", plugin = "", permission, permissionMessage = ChatColor.RED + "You do not have permission to use this command.";
        private Predicate<CommandSender> predicate;
        private CommandExecutor executor;
        private List<String> aliases;
        private int minArgs = -1, maxArgs = -1;
        private boolean subcommand, onlyPlayer;

        private Map<Set<String>, SmartCommand> childCommandMap;

        Builder() {
        }

        public Builder subcommand() {
            this.subcommand = true;
            return this;
        }

        //

        public Builder name(String commandLabel) {
            this.commandLabel = commandLabel;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder permissionMessage(String permissionMessage) {
            this.permissionMessage = permissionMessage;
            return this;
        }

        public Builder predicate(Predicate<CommandSender> predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder executor(CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        public Builder minArgs(int minArgs) {
            this.minArgs = minArgs;
            return this;
        }

        public Builder maxArgs(int maxArgs) {
            this.maxArgs = maxArgs;
            return this;
        }

        public Builder aliases(String... aliases) {
            this.aliases = ImmutableList.copyOf(aliases);
            return this;
        }

        public Builder plugin(Plugin plugin) {
            return this.plugin(plugin.getDescription().getName());
        }

        public Builder plugin(String plugin) {
            this.plugin = plugin;
            return this;
        }

        public Builder onlyPlayer() {
            this.onlyPlayer = true;
            return this;
        }

        public Builder child(SmartCommand child, String... aliases) {
            if (this.childCommandMap == null) {
                this.childCommandMap = new HashMap<>();
            }

            final ImmutableSet<String> set = ImmutableSet.copyOf(ImmutableSet.copyOf(aliases));

            if (set.isEmpty()) {
                throw new IllegalArgumentException("<aliases> cannot cannot be empty for subcommand.");
            }

            this.childCommandMap.put(set, child);
            return this;
        }

        public SmartCommand build() throws IllegalArgumentException {
            if (this.subcommand) {

                if (!StringUtils.isEmpty(this.commandLabel)) {
                    throw new IllegalArgumentException("<name> not allowed for subcommand.");
                } else if ((this.aliases != null) && (!this.aliases.isEmpty())) {
                    throw new IllegalArgumentException("<aliases> not allowed for subcommand.");
                } else if (!StringUtils.isEmpty(this.plugin)) {
                    throw new IllegalArgumentException("<plugin> not allowed for subcommand.");
                }

                return new SmartCommand(this.commandLabel, this.description, this.usage, this.aliases, this.plugin, this.permission, this.permissionMessage, this.predicate, this.executor, this.minArgs, this.maxArgs, this.onlyPlayer, this.childCommandMap);
            } else {
                SmartCommand smartCommand = new SmartCommand(this.commandLabel, this.description, this.usage, this.aliases, this.plugin, this.permission, this.permissionMessage, this.predicate, this.executor, this.minArgs, this.maxArgs, this.onlyPlayer, this.childCommandMap);
                register(smartCommand);
                return smartCommand;
            }
        }

    }

    private static void register(SmartCommand smartCommand) throws IllegalArgumentException {
        ReflectCommand command;

        if (StringUtils.isNotEmpty(smartCommand.commandLabel)) {
            command = new ReflectCommand(smartCommand);
        } else {
            throw new IllegalArgumentException("Command does not have a commandLabel.");
        }

        if (smartCommand.executor == null) {
            throw new IllegalArgumentException(smartCommand.commandLabel + " does not have an executor.");
        }

        getCommandMap().register(smartCommand.plugin, command);
        System.out.println(String.format("Successfully injected command '%s'.", command.getName()));
    }

    private static CommandMap getCommandMap() {
        if (commandMap == null) {
            try {
                final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                commandMap = (CommandMap) f.get(Bukkit.getServer());
                return commandMap;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return commandMap;
    }

    private final static class ReflectCommand extends Command {

        private final SmartCommand smartCommand;

        private ReflectCommand(final SmartCommand smartCommand) {
            super(smartCommand.commandLabel, smartCommand.description, smartCommand.usage, smartCommand.aliases);
            this.smartCommand = smartCommand;
        }

        @Override
        public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
            return this.smartCommand.onCommand(sender, this, commandLabel, args);
        }

    }


}

