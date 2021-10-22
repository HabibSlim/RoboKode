package com.robokode.game.screens.elements;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.robokode.utils.Ressources;

public class WheelSprite extends Actor {
    Sprite textureN;
    float actorX, actorY;
    float width, height;

    float angle = 0;
    float inC;

    public WheelSprite(int index, float actorX, float actorY, float inC){
        this.inC = inC;

        // On initialise les coordonnées du sprite
        this.actorX = actorX;
        this.actorY = actorY;

        Texture obj = Ressources.getImgRsc(index);
        textureN = new Sprite(obj);
        textureN.setPosition(actorX, actorY);

        // On initialise les dimensions du sprite
        width = obj.getWidth();  height = obj.getHeight();
    }

    @Override
    public void draw(Batch batch, float alpha) {
        //textureN.setOriginCenter();
        textureN.setRotation(angle);
        textureN.draw(batch, alpha);
        //batch.draw(textureN, actorX, actorY);
    }

    @Override
    public void act(float delta){
        this.angle+=inC;
        if (angle == 360)
            angle=0;
    }
}