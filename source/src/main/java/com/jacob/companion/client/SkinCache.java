package com.jacob.companion.client;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Render lookups never block; bounded cache, bounded downloads and one background worker. */
final class SkinCache {
    private static final ResourceLocation STEVE=new ResourceLocation("minecraft","textures/entity/player/wide/steve.png");
    private static final Map<String,ResourceLocation> CACHE=new ConcurrentHashMap<>();
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Socrates skins");t.setDaemon(true);return t;});
    static ResourceLocation get(String key) {
        if(!CACHE.containsKey(key) && CACHE.size()<64) {
            CACHE.put(key,STEVE);
            WORKER.submit(()->load(key));
        }
        return CACHE.getOrDefault(key,STEVE);
    }
    private static void load(String key) {
        try {
            if(key.startsWith("local:")) {
                String file=key.substring(6);
                if(!file.matches("[A-Za-z0-9_-]{1,48}\\.png")) throw new IOException("Invalid skin filename");
                Path root=FMLPaths.CONFIGDIR.get().resolve("socrates/skins"); Files.createDirectories(root);
                Path path=root.resolve(file);
                if(Files.isSymbolicLink(path)||Files.size(path)>1048576) throw new IOException("Invalid skin file");
                // Inspect IHDR before native decoding to avoid oversized-image allocations.
                try(var header=new DataInputStream(Files.newInputStream(path))) {
                    if(header.readLong()!=0x89504e470d0a1a0aL || header.readInt()!=13 || header.readInt()!=0x49484452)
                        throw new IOException("Invalid PNG header");
                    int width=header.readInt(),height=header.readInt();
                    if(width!=64 || (height!=32 && height!=64)) throw new IOException("Skin must be 64x64 or 64x32");
                }
                NativeImage image;
                try(var in=Files.newInputStream(path)) { image=NativeImage.read(in); }
                if(image.getWidth()!=64 || (image.getHeight()!=64 && image.getHeight()!=32)) {image.close();throw new IOException("Skin must be 64x64 or 64x32");}
                if(image.getHeight()==32) image=expandLegacy(image);
                NativeImage texture=image;
                Minecraft.getInstance().execute(()->CACHE.put(key,Minecraft.getInstance().getTextureManager().register("socrates_skin",new DynamicTexture(texture))));
            } else {
                if(!key.matches("[A-Za-z0-9_]{3,16}")) return;
                var connection=(HttpURLConnection)new URL("https://api.mojang.com/users/profiles/minecraft/"+key).openConnection();
                connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
                String json;
                try(var in=connection.getInputStream()) { byte[] bytes=in.readNBytes(16385);if(bytes.length>16384)throw new IOException("Profile too large");json=new String(bytes,StandardCharsets.UTF_8); }
                finally {connection.disconnect();}
                String id=JsonParser.parseString(json).getAsJsonObject().get("id").getAsString();
                UUID uuid=UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})","$1-$2-$3-$4-$5"));
                GameProfile profile=Minecraft.getInstance().getMinecraftSessionService().fillProfileProperties(new GameProfile(uuid,key),true);
                Minecraft.getInstance().execute(()->Minecraft.getInstance().getSkinManager().registerSkins(profile,(type,location,skin)->{
                    if(type==com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN)CACHE.put(key,location);
                },true));
            }
        } catch(Exception e) { LogUtils.getLogger().warn("Socrates skin '{}' unavailable; using Steve: {}",key,e.toString()); }
    }
    private static NativeImage expandLegacy(NativeImage old) {
        NativeImage out=new NativeImage(64,64,true);
        out.fillRect(0,0,64,64,0);
        for(int y=0;y<32;y++)for(int x=0;x<64;x++)out.setPixelRGBA(x,y,old.getPixelRGBA(x,y));
        old.close();
        out.copyRect(4,16,16,32,4,4,true,false);out.copyRect(8,16,16,32,4,4,true,false);
        out.copyRect(0,20,24,32,4,12,true,false);out.copyRect(4,20,16,32,4,12,true,false);
        out.copyRect(8,20,8,32,4,12,true,false);out.copyRect(12,20,16,32,4,12,true,false);
        out.copyRect(44,16,-8,32,4,4,true,false);out.copyRect(48,16,-8,32,4,4,true,false);
        out.copyRect(40,20,0,32,4,12,true,false);out.copyRect(44,20,-8,32,4,12,true,false);
        out.copyRect(48,20,-16,32,4,12,true,false);out.copyRect(52,20,-8,32,4,12,true,false);
        return out;
    }
}
