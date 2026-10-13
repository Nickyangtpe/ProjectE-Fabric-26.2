package moze_intel.projecte.compat.eventbus;

public abstract class Event {
  private boolean canceled;

  public boolean isCancelable() {
    return this instanceof ICancellableEvent;
  }

  public boolean isCanceled() {
    return this.canceled;
  }

  public void setCanceled(boolean cancel) {
    if (!this.isCancelable()) {
      throw new UnsupportedOperationException(
          "Attempted to cancel a non-cancelable event: " + this.getClass().getName());
    }

    this.canceled = cancel;
  }
}
