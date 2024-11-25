package app.uniform_values;

import static org.lwjgl.opengl.GL20.*;

public class UniformIntValues extends UniformValues<Integer[]> {

    public UniformIntValues(String name, Integer[]... values) {
        super(name, values);
    }

    @Override
    public void applyValue(int programID, int index) {
        int location = glGetUniformLocation(programID, name);
        Integer[] value = values[index];

        switch (value.length){
            case 1: glUniform1i(location, values[index][0]); break;
            case 2: glUniform2i(location, values[index][0], values[index][1]); break;
            case 3: glUniform3i(location, values[index][0], values[index][1], values[index][2]); break;
        }
    }
}
