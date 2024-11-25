package app.uniform_values;

import static org.lwjgl.opengl.GL20.*;

public class UniformInt1Values extends UniformValues<Integer> {

    public UniformInt1Values(String name, Integer... values) {
        super(name, values);
    }

    @Override
    public void applyValue(int programID, int index) {
        int location = glGetUniformLocation(programID, name);
        Integer value = values[index];
        glUniform1i(location, value);
    }
}
