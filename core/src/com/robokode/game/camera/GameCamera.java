package com.robokode.game.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameCamera {

    private OrthographicCamera cam;
    public FitViewport viewport;

    public GameCamera(int width, int height) {
        cam = new OrthographicCamera();
        viewport = new FitViewport(width, height, cam);

        viewport.getCamera().position.set(width/2f - width*0.061f , 0, 0); // On centre la caméra au milieu de l'écran
        viewport.apply();
    }

    public void update (int width, int height) {
        viewport.update(width, height);
    }

    public OrthographicCamera getCam() {
        return this.cam;
    }
    public Viewport getPort() {
        return this.viewport;
    }
}
