package com.jacob.companion;
import com.google.gson.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Fixed loopback endpoint. A model never supplies commands, paths, coordinates or URLs. */
public final class AiBridge {
    private static final Set<UUID> PENDING=ConcurrentHashMap.newKeySet();
    private static final Map<UUID,Long> LAST=new ConcurrentHashMap<>();
    private static final ExecutorService WORKER=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),r->{var t=new Thread(r,"Socrates planner");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    public static void ask(ServerPlayer player,SocratesEntity s,String text) {
        if(!player.getServer().isSingleplayer()) { player.sendSystemMessage(Component.literal("Socrates AI bridge is single-player only."));return; }
        if(text.isBlank() || text.length()>1000) {player.sendSystemMessage(Component.literal("Use 1–1000 characters."));return;}
        long now=System.nanoTime();UUID owner=player.getUUID();
        if(PENDING.contains(owner) || now-LAST.getOrDefault(owner,0L)<TimeUnit.SECONDS.toNanos(3)) {player.sendSystemMessage(Component.literal("Socrates is still thinking; try again shortly."));return;}
        var server=player.getServer();var level=s.level();UUID entity=s.getUUID();int revision=s.getPlanRevision();
        var registry=server.overworld().getDataStorage().computeIfAbsent(CompanionRegistry::load,CompanionRegistry::new,"socrates_registry");
        var body=new JsonObject();body.addProperty("world",registry.worldId().toString());body.addProperty("owner",owner.toString());body.addProperty("entity",entity.toString());body.addProperty("text",text);
        PENDING.add(owner);LAST.put(owner,now);
        try { WORKER.submit(()->{
            JsonObject result=null;String failure=null;
            try { result=request(body); } catch(Exception ex) {failure="Planner unavailable or invalid reply. Start ai/service.py for this test instance.";}
            final JsonObject response=result;final String error=failure;
            PENDING.remove(owner);
            server.execute(()->{
                var current=server.getPlayerList().getPlayer(owner);
                if(current==null)return;
                if(error!=null) {current.sendSystemMessage(Component.literal(error));return;}
                if(!s.isAlive() || s.isRemoved() || !owner.equals(s.getOwner()) || s.level()!=level || current.level()!=level || s.getPlanRevision()!=revision || current.distanceToSqr(s)>4096) {
                    current.sendSystemMessage(Component.literal("Discarded stale Socrates plan; state changed or companion is out of range."));return;
                }
                String action=response.get("action").getAsString();
                switch(action) {
                    case "follow" -> s.setMode(SocratesEntity.Mode.ADVENTURE);
                    case "stay" -> s.setMode(SocratesEntity.Mode.STAY);
                    case "resident" -> s.setMode(SocratesEntity.Mode.RESIDENT);
                    case "home" -> s.setHome(s.blockPosition());
                    case "collect", "explore" -> { if(!s.setTask(action)) {current.sendSystemMessage(Component.literal("Task rejected: set home nearby first."));return;} }
                    default -> { }
                }
                current.sendSystemMessage(Component.literal("Socrates: "+response.get("reply").getAsString()+" [accepted: "+action+"; mode: "+s.getMode()+"]"));
            });
        }); } catch(RejectedExecutionException ex) { PENDING.remove(owner);player.sendSystemMessage(Component.literal("Planner queue busy.")); }
    }
    static JsonObject request(JsonObject body) throws Exception {
        Path file=FMLPaths.CONFIGDIR.get().resolve("socrates/bridge.json");
        if(Files.size(file)>4096)throw new IllegalArgumentException("Invalid config");
        String token=JsonParser.parseString(Files.readString(file)).getAsJsonObject().get("token").getAsString();
        if(!token.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid token");
        var c=(HttpURLConnection)new URL("http://127.0.0.1:8765/v1/plan").openConnection(Proxy.NO_PROXY);
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(2000);c.setReadTimeout(25000);
        c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+token);
        byte[] payload=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(payload.length);
        try {
            try(var out=c.getOutputStream()) {out.write(payload);}
            if(c.getResponseCode()!=200)throw new IllegalArgumentException("Planner failed");
            try(var in=c.getInputStream()) {
                byte[] data=in.readNBytes(8193);if(data.length>8192)throw new IllegalArgumentException("Reply too big");
                return validate(JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } finally {c.disconnect();}
    }
    static JsonObject validate(JsonObject data) {
        if(data.size()!=2 || !data.has("action") || !data.has("reply") || !data.get("action").isJsonPrimitive() || !data.get("reply").isJsonPrimitive() || !data.getAsJsonPrimitive("action").isString() || !data.getAsJsonPrimitive("reply").isString())throw new IllegalArgumentException("Invalid reply");
        if(!Set.of("none","follow","stay","resident","home","status","collect","explore").contains(data.get("action").getAsString()) || data.get("reply").getAsString().length()>600)throw new IllegalArgumentException("Unsupported plan");
        return data;
    }
}
