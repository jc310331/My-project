package com.jacob.companion.client;
import com.jacob.companion.SocratesEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;
public class SocratesRenderer extends MobRenderer<SocratesEntity,PlayerModel<SocratesEntity>> {
    public SocratesRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER),false),0.5f);
        addLayer(new ItemInHandLayer<>(this,ctx.getItemInHandRenderer()));
        addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),ctx.getModelManager()));
    }
    @Override public ResourceLocation getTextureLocation(SocratesEntity entity) { return SkinCache.get(entity.getSkinUsername()); }
}
