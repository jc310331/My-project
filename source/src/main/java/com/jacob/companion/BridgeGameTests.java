package com.jacob.companion;
import java.util.UUID;
import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
/** Optional suite: start the Python service for run-gametest, then use -PbridgeTests. */
@GameTestHolder("companion_bridge")
@PrefixGameTestTemplate(false)
public class BridgeGameTests {
    @GameTest(template="floor",templateNamespace="companion",timeoutTicks=100)
    public static void pythonRoundTrip(GameTestHelper h) throws Exception {
        var request=new JsonObject();
        request.addProperty("world",UUID.randomUUID().toString());request.addProperty("owner",UUID.randomUUID().toString());request.addProperty("entity",UUID.randomUUID().toString());
        request.addProperty("text","follow me");
        h.assertTrue(AiBridge.request(request).get("action").getAsString().equals("follow"),"Wire action mismatch");
        request.addProperty("text","remember integration-test-fact");AiBridge.request(request);
        request.addProperty("text","memories");
        h.assertTrue(AiBridge.request(request).get("reply").getAsString().contains("integration-test-fact"),"SQLite fact missing across HTTP requests");
        h.succeed();
    }
}
