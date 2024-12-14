package app;

import app.solid.*;
import app.uniform_values.UniformF1Values;
import app.uniform_values.UniformFValues;
import app.uniform_values.UniformInt1Values;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static app.EColorMode.DEFAULT;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer extends AbstractRenderer {

    // Solids
    private List<Solid> solids;

    // Shader programs
    private List<ShaderProgram> shaderPrograms;

    // SceneCameras
    private List<SceneWindow> sceneWindowList = new ArrayList<>();

    // Shadow mapping
    private OGLTexture2D.Viewer viewer;

    // Texture
    OGLTexture2D textureNoise;

    // Wall
    Mat4 wallMat;

    // Camera Controls
    boolean mouseButton1 = false;
    int selectedWindowIndex = 0;
    double ox, oy;
    double speed = 0.5;

    // Other Controls
    int polygonMode = GL_FILL;
    EColorMode colorMode = DEFAULT;
    private long time = 0;


    public void init() {
        // Behaviour
        glClearColor(0.1f, 0.1f, 0.1f, 2.0f);
        glEnable(GL_DEPTH_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        time = System.nanoTime();
        glPointSize(1.0f);

        // Default camera
        Camera defCamera = new Camera()
                .withPosition(new Vec3D(0, 0, 2))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-75))
                .withFirstPerson(false)
                .withRadius(3);
        Mat4PerspRH defProjPersp = new Mat4PerspRH(Math.toRadians(45), height / (float) width, 0.1, 100);
        Mat4 defProjOrth = new Mat4OrthoRH(1, 1, 0.1, 100);
        sceneWindowList.add(new SceneWindow(width, height, defCamera, defProjPersp, defProjOrth));

        // Shadow mapping camera
        Vec3D lightPosition = new Vec3D(1, 0, 10);
        Camera cameraLight = new Camera()
                .withPosition(lightPosition)
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-90))
                .withFirstPerson(true);
        Mat4PerspRH projLightPersp = new Mat4PerspRH(Math.toRadians(45), height / (float) width, 1, 2);
        Mat4 projOrthLight = new Mat4OrthoRH(5, 5, 1, 20);
        sceneWindowList.add(new SceneWindow(width, height, cameraLight, projOrthLight, projLightPersp));

        // Solids
        Axis axis = new Axis();
        Grid plane = new Grid(2, 2);
        Flame flame = new Flame(1_000_000);
        Grid wall = new Grid(2, 2);
        wallMat = new Mat4RotX(Math.toRadians(-85)).mul(new Mat4Transl(0.0, 0.5, 0));
        wall.setModel(wallMat);
        solids = Arrays.asList(axis, plane, flame, wall);

        // Shader programs
        ShaderProgram programAxis = new ShaderProgram("/axis", 0);

        ShaderProgram programGrid = new ShaderProgram("/grid", 1, 3);
        programGrid.addUniform(new UniformInt1Values("uAnimateType",
                0, 0
        ));
        programGrid.addUniform(new UniformFValues("uBaseColor",
                new Float[]{0.8f, 0.8f, 0.8f},
                new Float[]{0.8f, 0.8f, 0.5f}
        ));
        programGrid.addUniform(new UniformF1Values( "uSpecStrength",
                0.2f, 0.1f
        ));
        programGrid.addUniform(new UniformF1Values( "uShininess",
                7f, 5f
        ));

        ShaderProgram programFlame = new ShaderProgram("/flame", 2);
        programFlame.addUniform(new UniformF1Values("uLifespan",
                3.0f
        ));
        programFlame.addUniform(new UniformF1Values("uMinRange",
                1.0f
        ));
        programFlame.addUniform(new UniformF1Values("uMaxRange",
                2.0f
        ));

        shaderPrograms = Arrays.asList(programAxis, programGrid, programFlame);

        // Viewer to display other textures
        viewer = new OGLTexture2D.Viewer();

        // Textures
        try {
            textureNoise = new OGLTexture2D("textures/noise_normalized.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void display() {
        // draw the rest into textures
        for (int i = 0; i < sceneWindowList.size(); i++) {
            drawScene(i);
        }

        // Draw the shadowMap in the corner
        SceneWindow lightWindow = sceneWindowList.get(1);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        viewer.view(lightWindow.getRenderTarget().getDepthTexture(), -1, -1, 0.5);

        // update time
        time = System.nanoTime();

        setupTextures();
    }

    private void setupTextures() {
        // setup textures
        textureNoise.bind(shaderPrograms.get(2).getProgramID(), "textureNoise", 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    private void drawScene(int sceneWindowIndex) {
        // Bind buffer / renderTarget
        SceneWindow sceneWindow = sceneWindowList.get(sceneWindowIndex);
        if(sceneWindowIndex == 0)
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        else
            sceneWindow.getRenderTarget().bind();

        // Setup canvas
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


        for (int i = 0; i < shaderPrograms.size(); i++) {
            ShaderProgram program = shaderPrograms.get(i);
            glUseProgram(program.getProgramID());
            setGlobalUniforms(sceneWindow, program.getProgramID());

            List<Integer> solidIndexes = program.getSolidIndexes();
            for (int j : solidIndexes) {
                Solid solid = solids.get(j);
                // setup uniforms
                setModelUniform(program.getProgramID(), solid);
                program.applyUniforms(j);
                // Specific/Complex behaviour
                manualSetupBeforeDraw(sceneWindowIndex, i, j, sceneWindow, program, solid);
                // draw
                solid.draw(program.getProgramID());
            }
        }
    }

    private void manualSetupBeforeDraw(
            int windowIndex,            int programIndex,       int solidIndex,
            SceneWindow sceneWindow,    ShaderProgram program,  Solid solid
    ) {
        if(windowIndex == 0) {
            // save shadowMap to texture
            SceneWindow lightWindow = sceneWindowList.get(1);
            lightWindow.getRenderTarget()
                    .bindDepthTexture(program.getProgramID(), "shadowMap", 0);
            // save light VP matrix to uniform
            glUniformMatrix4fv(
                    glGetUniformLocation(program.getProgramID(), "uVPLight"),
                    false,
                    lightWindow.getView().mul(lightWindow.getProjection()).floatArray()
            );
        }
    }

    private void setGlobalUniforms(SceneWindow sceneWindow, int shaderProgram) {
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, sceneWindow.getView().floatArray());

        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, sceneWindow.getProjection().floatArray());

        // save light position to uniform
        SceneWindow lightWindow = sceneWindowList.get(1);
        Vec3D lightPosition = lightWindow.getCamera().getPosition();
        int locLightPos = glGetUniformLocation(shaderProgram, "uLightPos");
        glUniform3f(locLightPos,
                    (float)lightPosition.getX(), (float)lightPosition.getY(), (float)lightPosition.getZ()
        );

        // save cam position to uniform
        int locUCameraPos = glGetUniformLocation(shaderProgram, "uCameraPos");
        Vec3D camPosition = sceneWindow.getCamera().getPosition();
        glUniform3f(locUCameraPos,
                (float)camPosition.getX(), (float)camPosition.getY(), (float)camPosition.getZ()
        );

        int locUTime = glGetUniformLocation(shaderProgram, "uTime");
        glUniform1f(locUTime, time / 1_000_000_000.0f);

        int locUColorMode = glGetUniformLocation(shaderProgram, "uColorMode");
        glUniform1i(locUColorMode, colorMode.ordinal());

        int locUWallModel = glGetUniformLocation(shaderProgram, "uWallModel");
        glUniformMatrix4fv(locUWallModel, false, wallMat.floatArray());
    }

    private void setModelUniform(int shaderProgram, Solid solid) {
        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        glUniformMatrix4fv(locUModel, false, solid.getModel().floatArray());
    }


    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                mouseButton1 = true;
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                ox = xBuffer.get(0);
                oy = yBuffer.get(0);
            }

            if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE) {
                mouseButton1 = false;
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                double x = xBuffer.get(0);
                double y = yBuffer.get(0);
                SceneWindow selectedSceneWindow = sceneWindowList.get(selectedWindowIndex);
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .addAzimuth((double) Math.PI * (ox - x) / width)
                                .addZenith((double) Math.PI * (oy - y) / width)
                );
                ox = x;
                oy = y;
            }
        }

    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mouseButton1) {
                SceneWindow selectedSceneWindow = sceneWindowList.get(selectedWindowIndex);
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .addAzimuth((double) Math.PI * (ox - x) / width)
                                .addZenith((double) Math.PI * (oy - y) / width)
                );
                ox = x;
                oy = y;
            }
        }
    };


    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            SceneWindow selectedSceneWindow = sceneWindowList.get(selectedWindowIndex);
            if (dy < 0){
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .mulRadius(0.9f)
                );
            }
            else{
                selectedSceneWindow.setCamera(
                        selectedSceneWindow.getCamera()
                                .mulRadius(1.1f)
                );
            }
        }
    };

    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            switch (action) {
                case GLFW_PRESS:
                    SceneWindow selectedSceneWindow = sceneWindowList.get(selectedWindowIndex);
                    Camera camera = selectedSceneWindow.getCamera();
                    Vec3D forward = camera.getViewVector().mul(speed);
                    Optional<Vec3D> rightOpt = forward.cross(new Vec3D(0, 0, 1)).normalized();
                    Vec3D right = rightOpt.map(vec -> vec.mul(speed))
                            .orElse(new Vec3D(0));
                        switch (key) {
                            case GLFW_KEY_M:
                                // Change PolygonMode
                                Integer[] options = {GL_FILL, GL_LINE, GL_POINT};
                                int index = Arrays.asList(options).indexOf(polygonMode);
                                if (index < 0 || index == options.length - 1) {
                                    polygonMode = options[0];
                                } else {
                                    polygonMode = options[index + 1];
                                }
                                glPolygonMode(GL_FRONT_AND_BACK, polygonMode);
                                break;
                            case GLFW_KEY_P:
                                selectedSceneWindow.switchProjection();
                                break;
                            case GLFW_KEY_C:
                                if(selectedWindowIndex >= sceneWindowList.size() - 1){
                                    selectedWindowIndex = 0;
                                } else {
                                    selectedWindowIndex++;
                                }
                                break;
                            case GLFW_KEY_N:
                                if (colorMode.ordinal() >= EColorMode.values().length - 1) {
                                    colorMode = EColorMode.values()[0]; // Set to the first enum value
                                } else {
                                    colorMode = EColorMode.values()[colorMode.ordinal() + 1]; // Increment to the next enum value
                                }
                                break;
                            case GLFW_KEY_W:
                                // Move forward
                                selectedSceneWindow.setCamera(
                                        camera.withPosition(camera.getPosition().add(forward))
                                );
                                break;
                            case GLFW_KEY_A:
                                // Move left
                                selectedSceneWindow.setCamera(
                                        camera.withPosition(camera.getPosition().sub(right))
                                );
                                break;
                            case GLFW_KEY_S:
                                // Move backward
                                selectedSceneWindow.setCamera(
                                        camera.withPosition(camera.getPosition().sub(forward))
                                );
                                break;
                            case GLFW_KEY_D:
                                // Move right
                                selectedSceneWindow.setCamera(
                                        camera.withPosition(camera.getPosition().add(right))
                                );
                                break;
                    }
                    break;
                case GLFW_KEY_DOWN:
                    switch (key) {

                    }
                    break;
            }
        }
    };


    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mbCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallbacknew;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }

    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

}