package com.jacob.companion;

import net.minecraft.world.entity.player.Player;

/** Menu requests are scoped to an owned, loaded, nearby companion; never commands. */
public final class InteractionActions {
    public enum Action { TALK, FOLLOW, STAY, HOME, RESIDENT, COLLECT, EXPLORE, UNLOAD, AUTONOMY_ON, AUTONOMY_OFF, SKIN, REFRESH }
    public record Result(boolean accepted, String message) { }

    public static boolean canControl(Player player, SocratesEntity companion) {
        return player != null && companion != null && player.isAlive() && !player.isSpectator()
            && companion.isAlive() && !companion.isRemoved()
            && player.getUUID().equals(companion.getOwner()) && player.level() == companion.level()
            && player.distanceToSqr(companion) <= 36;
    }

    public static Result apply(Player player, SocratesEntity companion, Action action, String text) {
        if (!canControl(player, companion)) return new Result(false, "Move closer to your own Socrates to interact.");
        if (action == null || text == null || text.length() > 1000)
            return new Result(false, "Invalid interaction.");
        if (action != Action.TALK && action != Action.SKIN && !text.isEmpty())
            return new Result(false, "Unexpected interaction text.");
        switch (action) {
            case REFRESH -> {return new Result(true, "");}
            case TALK -> {
                if (text.isBlank()) return new Result(false, "Write a message first.");
                return new Result(true, "Thinking...");
            }
            case FOLLOW -> companion.setMode(SocratesEntity.Mode.ADVENTURE);
            case STAY -> companion.setMode(SocratesEntity.Mode.STAY);
            case HOME -> companion.setHome(companion.blockPosition());
            case RESIDENT -> companion.setMode(SocratesEntity.Mode.RESIDENT);
            case COLLECT, EXPLORE -> {
                if (!companion.setTask(action == Action.COLLECT ? "collect" : "explore"))
                    return new Result(false, "Set home nearby before starting a task.");
            }
            case UNLOAD -> {
                if (player.distanceToSqr(companion) > 16)
                    return new Result(false, "Stand within four blocks to receive items.");
                for (int slot = 0; slot < companion.getInventory().getContainerSize(); slot++) {
                    var stack = companion.getInventory().removeItemNoUpdate(slot);
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                }
            }
            case AUTONOMY_ON -> companion.setAutonomy(true);
            case AUTONOMY_OFF -> companion.setAutonomy(false);
            case SKIN -> {
                if (!text.matches("[A-Za-z0-9_]{3,16}|local:[A-Za-z0-9_-]{1,48}\\.png"))
                    return new Result(false, "Use a username or local:filename.png.");
                companion.setSkinUsername(text);
            }
        }
        return new Result(true, switch (action) {
            case FOLLOW -> "I'll follow you.";
            case STAY -> "I'll stay here. Autonomy is off.";
            case HOME -> "Home is now set to where I'm standing.";
            case RESIDENT -> "I'll stay near home.";
            case COLLECT -> "Looking for nearby dropped items.";
            case EXPLORE -> "Exploring near home.";
            case UNLOAD -> "Transferred the items I could give you.";
            case AUTONOMY_ON -> "Autonomy enabled. Configured cloud providers can incur charges.";
            case AUTONOMY_OFF -> "Autonomy disabled.";
            case SKIN -> "Skin requested: " + text + ". Failed lookups may need a client restart.";
            default -> "Ready.";
        });
    }

    public static String status(SocratesEntity companion) {
        int count = 0;
        for (int slot = 0; slot < companion.getInventory().getContainerSize(); slot++)
            count += companion.getInventory().getItem(slot).getCount();
        return "Mode: " + companion.getMode().name().toLowerCase(java.util.Locale.ROOT)
            + " | Task: " + (companion.getTask().isEmpty() ? "none" : companion.getTask())
            + " | Items: " + count + " | Autonomy: " + (companion.isAutonomy() ? "on" : "off");
    }
}
