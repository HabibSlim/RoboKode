package com.robokode.game.screens.elements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.robokode.utils.Math;
import com.robokode.utils.Ressources;

public class LifeBar extends Sprite {
    private Texture fullBar, circle, portrait, shade, bar;

    // Unit scale du dessin
    private static final float scale = 2;

    // Niveau en pixels de la barre de vie
    private static final float MAXBAR_W = 252*scale;
    private static final float BAR_H = 23*scale;
    private float currentW = MAXBAR_W;

    // Couleur courante de la barre
    private Color currentCoul;
    private Pixmap pixmap;

    // Intervalles de couleur (vert => rouge)
    private final int[] RGB_MIN = {255, 68, 46};
    private final int[] RGB_MAX = {5,  170, 55};

    public LifeBar(float actorX, float actorY){
        /** Fond graphique **/

        // On initialise les coordonnées du sprite
        setPosition(actorX, actorY);

        // Initialisation des textures
        fullBar  = Ressources.getHUDElement(0);
        circle   = Ressources.getHUDElement(1);
        portrait = Ressources.getHUDElement(2);
        shade    = Ressources.getHUDElement(3);

        /** Barre de vie **/

        // Création de la barre de vie
        pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        // Initialisation de la couleur
        currentCoul = new Color(RGB_MAX[0], RGB_MAX[1], RGB_MAX[2], 1);
        pixmap.setColor(currentCoul); pixmap.fill();

        // Création de la texture associée
        bar = new Texture(pixmap);

        setLifeTotal(100);
    }

    @Override
    public void draw(Batch batch){
        // On dessine chaque calque dans le bon ordre
        batch.draw(fullBar, getX(), getY(), fullBar.getWidth()*scale, fullBar.getHeight()*scale);

        // On dessine la barre de vie colorée en fonction du pourcentage
        batch.draw(bar, getX()+96*scale, getY()+37*scale, currentW, BAR_H);

        batch.draw(circle,  getX()+16, getY()+36, circle.getWidth()*scale, circle.getHeight()*scale);
        batch.draw(portrait,getX()+16, getY()+36, portrait.getWidth()*scale, portrait.getHeight()*scale);
        batch.draw(shade,   getX()+16, getY()+36, shade.getWidth()*scale, shade.getHeight()*scale);
    }

    // Actualise la largeur de la barre de vie et sa couleur
    // en fonction du total en pourcentage passé en paramètres
    public void setLifeTotal (int total) {
        this.currentW = (total*MAXBAR_W)/100;

        // => Interpolation linéaire pour avoir la valeur RGB voulue
        int r = Math.interpolate(0, RGB_MIN[0], 100, RGB_MAX[0], total);
        int g = Math.interpolate(0, RGB_MIN[1], 100, RGB_MAX[1], total);
        int b = Math.interpolate(0, RGB_MIN[2], 100, RGB_MAX[2], total);

        currentCoul.set(r/255f, g/255f, b/255f, 1);
        pixmap.setColor(currentCoul); pixmap.fill();
        bar.dispose();  bar = new Texture(pixmap);
    }
}