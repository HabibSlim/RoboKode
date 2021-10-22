package com.robokode.game.sprites;

// RoboKode
import com.robokode.utils.Math;

// LibGDX
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;


/**
 * Classe générique représentant les objets graphiques du jeu (joueurs\projectiles\etc.)
 */
public abstract class GenericSprite extends Sprite implements Comparable<GenericSprite> {

    // Objet de rendu isométrique
    protected IsometricTiledMapRenderer renderer;

    // Coordonnées du sprite
    // => isométrique :                // => pixels :
    protected Vector3 coordIso;        protected Vector3 coordPix;

    public GenericSprite(Vector3 coord, Texture spriteP, IsometricTiledMapRenderer renderer) {
        // On charge en mémoire le sprite d'animation du personnage
        super(spriteP);
        this.renderer = renderer;

        // Initialisation des vecteurs
        this.coordPix = new Vector3();   this.coordIso = new Vector3();

        // Les sprites sont initialisés à leurs coordonnées iso
        this.coordIso.x = coord.x;    this.coordIso.y = coord.y;
    }

    // Méthode de dessin de l'élément graphique
    public abstract void draw();

    // Coordonnées iso du sprite
    public Vector3 getCoordIso() {
        return this.coordIso;
    }

    // Coordonnées iso non arrondies du sprite
    public Vector3 getCoordIsoR() { return Math.worldToIso(this.coordPix); }

    // Les sprites ayant la cordonnée x la plus forte sont dessinés après
    // => donc classés en fin de liste
    public int compareTo(GenericSprite obj) {
        int xA, xB, yA, yB;

        xB = (int)coordIso.x;          yB = (int)coordIso.y;
        xA = (int)obj.getCoordIso().x; yA = (int)obj.getCoordIso().y;

        if ((xB>xA)  &&  (yB<yA))  return -1;
        if ((xB>xA)  && (yB==yA))  return -1;
        if ((xA==xB) && (yB<yA))   return -1;
        if ((xB==xA) && (yB>yA))   return 1;
        if ((xB<xA)  && (yB>yA))   return 1;
        if ((xB<xA)  && (yB==yA))  return 1;

        return -1;
    }
}
