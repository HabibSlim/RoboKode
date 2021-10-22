package com.robokode.game.screens.elements;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.robokode.utils.Ressources;

public class LevelBouton extends Actor {
    Sprite iconeN;

    // Numéro du niveau
    int index;

    // Alpha courant sur l'icône
    float alphaI = 0.5f;

    // Flags d'état
    public boolean started = false;
    private boolean active = false;

    public LevelBouton(float actorX, float actorY, int index) {
        this.index = index;

        // Icône du bouton
        iconeN = new Sprite(Ressources.getImgRsc(5));

        setBounds(actorX, actorY, iconeN.getWidth(), iconeN.getHeight());
        iconeN.setPosition(actorX, actorY);

        final Actor moi = this;
        addListener(new InputListener(){
            public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
                ((LevelBouton)moi).started = true;
            }
            public void exit (InputEvent event, float x, float y, int pointer, Actor fromActor) {
                ((LevelBouton)moi).started = false;
            }
        });
    }

    @Override
    public void draw(Batch batch, float alpha){
        if (alpha != 1) return; // Si l'acteur parent fait changer l'alpha, on ne fait pas le rendu

        if (!active) {
            if (started) {
                if (alphaI < 1) {
                    alphaI += 0.1f;
                    if (alphaI > 1) alphaI = 1;
                }
            } else if (alphaI > 0.5f) {
                alphaI -= 0.1f;
                if (alphaI <= 0.5f) alphaI = 0.5f;
            }
        }

        iconeN.setAlpha(alphaI);
        iconeN.draw(batch);
    }

    // Modifie l'état du bouton
    public void setActive(boolean state) {
        if (state) {
            this.active = true;
            this.alphaI = 1;
        } else {
            this.active = false;
            this.alphaI = 0.5f;
        }
    }
}