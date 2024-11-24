package app.uniform_values;

public abstract class UniformValues<T> {
    protected final String name;
    protected final T defaultValue;
    protected final T[] values;

    public UniformValues(String name, T defaultValue, T... values) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.values = values;
    }

    public abstract void applyValue(int programID, int index);
}
