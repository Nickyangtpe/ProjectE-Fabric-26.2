package moze_intel.projecte.compat.network.registration;

import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.compat.network.client.ClientPayloadBridge;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.compat.network.handling.IPayloadHandler;
import moze_intel.projecte.compat.network.handling.ServerPayloadContext;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.NotNull;

public class PayloadRegistrar {
  private final String version;
  private final boolean optional;

  public PayloadRegistrar(@NotNull String version) {
    this(version, false);
  }

  private PayloadRegistrar(String version, boolean optional) {
    this.version = version;
    this.optional = optional;
  }

  @NotNull
  public String version() {
    return this.version;
  }

  public boolean isOptional() {
    return this.optional;
  }

  public PayloadRegistrar versioned(@NotNull String version) {
    return new PayloadRegistrar(version, this.optional);
  }

  public PayloadRegistrar optional() {
    return new PayloadRegistrar(this.version, true);
  }

  public <PAYLOAD extends CustomPacketPayload> void playToServer(
      @NotNull Type<PAYLOAD> type,
      @NotNull StreamCodec<? super RegistryFriendlyByteBuf, PAYLOAD> codec,
      @NotNull IPayloadHandler<PAYLOAD> handler) {
    PayloadTypeRegistry.serverboundPlay().register(type, codec);
    ServerPlayNetworking.registerGlobalReceiver(
        type,
        (payload, context) -> {
          IPayloadContext payloadContext = new ServerPayloadContext(context);
          payloadContext.enqueueWork(() -> handler.handle((PAYLOAD) payload, payloadContext));
        });
  }

  public <PAYLOAD extends CustomPacketPayload> void playToClient(
      @NotNull Type<PAYLOAD> type,
      @NotNull StreamCodec<? super RegistryFriendlyByteBuf, PAYLOAD> codec,
      @NotNull IPayloadHandler<PAYLOAD> handler) {
    PayloadTypeRegistry.clientboundPlay().register(type, codec);
    if (FMLEnvironment.dist.isClient()) {
      ClientPayloadBridge.registerReceiver(type, handler);
    }
  }

  public <PAYLOAD extends CustomPacketPayload> void playBidirectional(
      @NotNull Type<PAYLOAD> type,
      @NotNull StreamCodec<? super RegistryFriendlyByteBuf, PAYLOAD> codec,
      @NotNull IPayloadHandler<PAYLOAD> handler) {
    this.playToClient(type, codec, handler);
    this.playToServer(type, codec, handler);
  }
}
