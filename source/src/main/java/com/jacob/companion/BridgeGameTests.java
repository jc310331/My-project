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
        var observation=new JsonObject();observation.addProperty("health",20);observation.addProperty("mode","STAY");
        observation.add("items",new JsonObject());observation.addProperty("has_axe",true);observation.addProperty("has_pickaxe",false);
        request.add("observations",observation);request.addProperty("text","go get me 15 oak logs");
        h.assertTrue(AiBridge.request(request).get("action").getAsString().equals("oak_logs:15"),"Quantity request failed across authenticated bridge");
        request.addProperty("text","[autonomy]");
        h.assertTrue(AiBridge.request(request).get("action").getAsString().equals("oak_logs:2"),"Personal materials goal did not use observed tools");
        h.succeed();
    }
}
