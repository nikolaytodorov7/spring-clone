package spring.dic.events;

public abstract class ApplicationEvent {
    private final Object source;
    private final long timestamp;

    public ApplicationEvent(Object source) {
        if (source == null)
            throw new IllegalArgumentException("Source is null!");

        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }

    public Object getSource() {
        return source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        return getClass().getName() + "[source=" + source + "]";
    }
}