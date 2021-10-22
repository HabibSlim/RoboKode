package com.robokode.game.screens;

/**
 * Ecran de chargement => Non utilisé pour l'instant, à intégrer ultèrieurement
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.robokode.game.RoboGame;
import com.robokode.utils.Ressources;

public class Loading implements Screen {

    /**   Dimensions\positions des éléments   **/
    private static final int LOADING_W = 360, LOADING_H = 280;
    private static final int ROBOICO_W = 175, ROBOICO_H = 140;

    // Attributs privés de classe
    private final RoboGame game;

    // Stage Scene2D
    private Stage stage;
    private Texture loading, robotIco;

    public Loading(final RoboGame game) {
        this.game = game;

        // On crée le stage Scene2D
        stage = new Stage();  Gdx.input.setInputProcessor(stage);

        // On ajoute un acteur pour l'effet blur
        Sprite blurSprite = new Sprite(Ressources.getOtherImg(1));
        blurSprite.setPosition((RoboGame.W_WIDTH - LOADING_W)/2, (RoboGame.W_HEIGHT - LOADING_H)/2);
        stage.addActor(new Actor() {
            float myAlpha = 1;
            boolean down = false;

            @Override
            public void draw(Batch batch, float alpha) {
                blurSprite.draw(batch, myAlpha);
            }

            /** Effet de clignotement simple **/
            @Override
            public void act(float delta){
                if ((myAlpha > 1) || (myAlpha < 0)) down = !down;
                if ((myAlpha == 1) || down) {
                    myAlpha -= 0.025;
                    down = true;
                }
                else if ((myAlpha < 1) || !down) {
                    myAlpha += 0.025;
                    down = false;
                }
            }
        });

        // On charge les autres textures
        loading = Ressources.getOtherImg(0);    robotIco = Ressources.getOtherImg(2);
    }

    @Override
    public void show () {
        /** On définit cet écran comme capteur de l'input utilisateur **/
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render (float delta) {
        // On dessine la couleur de fond
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClearColor(29f/255f, 29f/255f, 29f/255f, 1);

        // On dessine le Stage
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

        // On dessine les autres images
        stage.getBatch().begin();
        // Loading
        stage.getBatch().draw(loading, (RoboGame.W_WIDTH - LOADING_W)/2, (RoboGame.W_HEIGHT - LOADING_H)/2, LOADING_W, LOADING_H);
        // Icône
        stage.getBatch().draw(robotIco, ROBOICO_W, ROBOICO_H, ROBOICO_W, ROBOICO_H);
        stage.getBatch().end();
    }

    @Override
    public void resize (int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
    }
}
