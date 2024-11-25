package app.uniform_values;

public abstract class UniformValues<T> {
    protected final String name;
    protected final T[] values;

    public UniformValues(String name, T... values) {
        this.name = name;
        this.values = values;
    }

    public abstract void applyValue(int programID, int index);
}
