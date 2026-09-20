package UI;

public interface PageLifecycle {
    default void onShown() {}
    default void onHidden() {}
    default void disposePage() {}
}
