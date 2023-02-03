package spring.container.events;

import java.util.HashSet;
import java.util.Set;

public class ApplicationEventPublisher {
    private final Set<Listener> listeners = new HashSet<>();

    public void addListener(Listener listener) {
        if (listener == null)
            throw new IllegalArgumentException("Listener instance is null!");

        listeners.add(listener);
    }

    public void publishEvent(ApplicationEvent event) {
        publishEvent((Object) event);
    }

    public void publishEvent(Object event) {
        for (Listener listener : listeners) {
            Class<?> listenerClass = listener.type;
            Class<?> eventClass = event.getClass();
            if (!listenerClass.isAssignableFrom(eventClass))
                continue;

            Object result = listener.invoke(event);
            if (result != null)
                publishEvent(result);
        }
    }
}