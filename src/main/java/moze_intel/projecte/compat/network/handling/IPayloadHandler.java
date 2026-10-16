package moze_intel.projecte.compat.network.handling;

@FunctionalInterface
public interface IPayloadHandler<PAYLOAD> {
  void handle(PAYLOAD var1, IPayloadContext var2);
}
