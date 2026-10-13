package moze_intel.projecte.compat.eventbus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import moze_intel.projecte.compat.event.FabricEventBootstrap;

public class IEventBus {
  private final List<IEventBus.ListenerEntry<?>> typedListeners = new ArrayList<>();
  private final List<IEventBus.ListenerEntry<Event>> inferredListeners = new ArrayList<>();
  private long sequence;

  public IEventBus() {
    FabricEventBootstrap.initialize();
  }

  public synchronized <T extends Event> void addListener(Class<T> eventType, Consumer<T> listener) {
    this.typedListeners.add(
        new IEventBus.ListenerEntry<>(eventType, listener, EventPriority.NORMAL, this.sequence++));
  }

  public synchronized <T extends Event> void addListener(Consumer<T> listener) {
    this.inferredListeners.add(
        new IEventBus.ListenerEntry<Event>(
            Event.class,
            (Consumer<Event>) (Consumer<?>) listener,
            EventPriority.NORMAL,
            this.sequence++));
  }

  public synchronized <T extends Event> void addListener(
      EventPriority priority, Consumer<T> listener) {
    this.inferredListeners.add(
        new IEventBus.ListenerEntry<Event>(
            Event.class, (Consumer<Event>) (Consumer<?>) listener, priority, this.sequence++));
  }

  public synchronized <T extends Event> void addListener(
      EventPriority priority, Class<T> eventType, Consumer<T> listener) {
    this.typedListeners.add(
        new IEventBus.ListenerEntry<>(eventType, listener, priority, this.sequence++));
  }

  public <T extends Event> T post(T event) {
    List<IEventBus.ListenerEntry<?>> listeners;
    synchronized (this) {
      listeners = new ArrayList<>(this.typedListeners.size() + this.inferredListeners.size());
      listeners.addAll(this.typedListeners);
      listeners.addAll(this.inferredListeners);
    }

    listeners.sort(
        Comparator.<IEventBus.ListenerEntry<?>, EventPriority>comparing(entryx -> entryx.priority())
            .thenComparingLong(entryx -> entryx.sequence()));

    for (IEventBus.ListenerEntry<?> entry : listeners) {
      if (event.isCanceled()) {
        break;
      }

      if (entry.eventType() == Event.class || entry.eventType().isInstance(event)) {
        try {
          ((Consumer<T>) entry.listener()).accept(event);
        } catch (ClassCastException var6) {
        }
      }
    }

    return event;
  }

  private record ListenerEntry<T extends Event>(
      Class<T> eventType, Consumer<T> listener, EventPriority priority, long sequence) {}
}
