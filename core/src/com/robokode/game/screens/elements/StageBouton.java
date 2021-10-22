package com.robokode.game.screens.elements;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.robokode.utils.Ressources;

public class StageBouton extends Actor {
    Sprite textureN;
    Sprite iconeN;

    // Coordonnées du bouton :
    float actorX, actorY;

    // Alpha courant sur l'icône
    float alphaI = 0;
    public boolean started = false;

    public StageBouton(int index, float actorX, float actorY, float offsetX, float offsetY){
        // On initialise les coordonnées du sprite
        this.actorX = actorX;
        this.actorY = actorY;

        // Bouton de base
        textureN = new Sprite(new Texture(Ressources.getMenuBtn(index)));

        // Icône on hover
        iconeN = new Sprite(Ressources.getIconBtn(index));

        setBounds(actorX, actorY, textureN.getWidth(), textureN.getHeight());

        // On positionne les sprites
        iconeN.setPosition(getX()+offsetX, getY()+offsetY);
        textureN.setPosition(getX(), getY());

        final Actor moi = this;
        addListener(new InputListener(){
            public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
                ((StageBouton)moi).started = true;
            }
            public void exit (InputEvent event, float x, float y, int pointer, Actor fromActor) {
                ((StageBouton)moi).started = false;
            }
        });
    }

    @Override
    public void draw(Batch batch, float alpha){
        // On ne dessine les icônes qu'en dehors des animations de fondu
        if (alpha == 1) {
            if (started) {
                if (alphaI < 1) {
                    alphaI += 0.1; iconeN.setAlpha(alphaI);

                    if (alphaI > 1) alphaI = 1;
                }
                iconeN.draw(batch);
            } else if (alphaI > 0) {
                alphaI -= 0.1;
                if (alphaI <= 0) alphaI = 0;

                iconeN.setAlpha(alphaI);
                iconeN.draw(batch);
            }
        }
        textureN.draw(batch, alpha);
    }

}