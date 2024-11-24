package app.uniform_values;

import static org.lwjgl.opengl.GL20.*;

public class UniformFValues extends UniformValues<Float[]> {

    public UniformFValues(String name, Float[] defaultValue, Float[]... values) {
        super(name, defaultValue, values);
    }

    @Override
    public void applyValue(int programID, int index) {
        int location = glGetUniformLocation(programID, name);
        Float[] value = values[index];

        switch (value.length){
            case 1: glUniform1f(location, values[index][0]); break;
            case 2: glUniform2f(location, values[index][0], values[index][1]); break;
            case 3: glUniform3f(location, values[index][0], values[index][1], values[index][2]); break;
        }
    }
}
