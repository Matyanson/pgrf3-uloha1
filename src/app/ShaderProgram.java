package app;

import app.uniform_values.UniformValues;
import lwjglutils.ShaderUtils;

import java.util.ArrayList;
import java.util.List;

public class ShaderProgram {
    private final String shaderFileName;
    private final int programID;
    private List<Integer> solidIndexes;
    private List<UniformValues<?>> uniforms;

    public ShaderProgram(String shaderFileName) {
        this.shaderFileName = shaderFileName;
        this.solidIndexes = new ArrayList<Integer>();
        this.uniforms = new ArrayList<>();

        this.programID = ShaderUtils.loadProgram(shaderFileName);
    }

    public ShaderProgram(String shaderFileName, Integer ...solidIndexes) {
        this.shaderFileName = shaderFileName;
        this.solidIndexes = List.of(solidIndexes);
        this.uniforms = new ArrayList<>();

        this.programID = ShaderUtils.loadProgram(shaderFileName);
    }

    public void applyUniforms(int solidIndex) {
        int index = solidIndexes.indexOf(solidIndex);
        if (index == -1) return;

        for (UniformValues<?> uniformValues : uniforms) {
            uniformValues.applyValue(this.programID, index);
        }
    }

    public void addUniform(UniformValues<?> uniformValues) {
        this.uniforms.add(uniformValues);
    }

    public List<UniformValues<?>> getUniforms() {
        return uniforms;
    }

    public void setUniforms(List<UniformValues<?>> uniforms) {
        this.uniforms = uniforms;
    }

    public String getShaderFileName() {
        return shaderFileName;
    }

    public int getProgramID() {
        return programID;
    }

    public List<Integer> getSolidIndexes() {
        return solidIndexes;
    }

    public void setSolidIndexes(List<Integer> solidIndexes) {
        this.solidIndexes = solidIndexes;
    }

    public void addSolidIndexes(Integer ...solidIndexes) {
        this.solidIndexes.addAll(List.of(solidIndexes));
    }
}
