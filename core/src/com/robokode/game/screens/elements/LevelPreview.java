package com.robokode.game.screens.elements;


import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.robokode.utils.Ressources;

/**
 * Cercle central donnant un aperçu sur le niveau à consulter
 */
public class LevelPreview extends Actor {

    // Textures de l'acteur
    Sprite fullCircle;
    Sprite lvlPreviews[];
    Sprite lastPreview, newPreview;
    Sprite innerShade;

    // Numéro du niveau sélectionné
    int index = 0;

    // Alpha courant
    float alphaI = 0f;

    // Flags d'état
    public boolean started = false;

    // Position du cercle
    private float actorX, actorY;

    public LevelPreview(float actorX, float actorY) {
        this.actorX = actorX;   this.actorY = actorY;

        // Texture du cercle blanc
        fullCircle = new Sprite(Ressources.getImgRsc(4));
        setBounds(actorX, actorY, fullCircle.getWidth(), fullCircle.getHeight());

        // Ombre intérieure
        innerShade = new Sprite(Ressources.getImgRsc(6));
        innerShade.setPosition(actorX, actorY); innerShade.setAlpha(0.5f);

        // Preview des niveaux
        lvlPreviews = Ressources.getLvlPreviews();

        // Positionnement des éléments
        fullCircle.setPosition(actorX, actorY);
        for (Sprite lvlS : lvlPreviews)     lvlS.setPosition(actorX, actorY);
    }

    @Override
    public void draw(Batch batch, float alpha){

        // Dessin du cercle en fond
        fullCircle.draw(batch, alpha);

        // Dessin du preview du niveau
        // calculs de transparence
        if (started) {
            if (alphaI < 1) {
                alphaI += 0.05f;
            } else {
                alphaI = 0;
                started = false;
            }
        }

        // rendu des aperçus
        if (started) {
            lastPreview.setAlpha(1-alphaI);
            newPreview.setAlpha(alphaI);

            lastPreview.draw(batch);
            newPreview.draw(batch);
        } else {
            // Dessin de l'aperçu du niveau correspondant à l'index actif
            lvlPreviews[index].draw(batch, alpha);
        }

        // Dessin de l'ombre intérieure
        innerShade.draw(batch, alpha);
    }

    public void setIndex(int newIndex) {
        lastPreview = new Sprite(lvlPreviews[index]);
        newPreview  = new Sprite(lvlPreviews[newIndex]);

        // On positionne les sprites
        lastPreview.setPosition(actorX, actorY); newPreview.setPosition(actorX, actorY);

        // On démarre l'animation
        started = true;

        // On actualise l'index
        this.index = newIndex;
    }
}