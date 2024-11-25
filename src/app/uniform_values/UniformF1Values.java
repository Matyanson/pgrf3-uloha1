package app.uniform_values;

import static org.lwjgl.opengl.GL20.*;

public class UniformF1Values extends UniformValues<Float> {

    public UniformF1Values(String name, Float... values) {
        super(name, values);
    }

    @Override
    public void applyValue(int programID, int index) {
        int location = glGetUniformLocation(programID, name);
        Float value = values[index];
        glUniform1f(location, value);
    }
}
