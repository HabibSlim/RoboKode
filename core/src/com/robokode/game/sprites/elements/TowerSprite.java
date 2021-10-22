package com.robokode.game.sprites.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.robokode.game.sprites.GenericSprite;
import com.robokode.model.Robot;
import com.robokode.utils.Math;

/**
 *  Classe servant à gérer les ressources graphiques représentant le joueur
 */
public class TowerSprite extends GenericSprite {
    // Temps de rafraîchissement des animations
    private static float FRAME_FPS = 1f/10f;

    // Dimensions du sprite
    private static final int tileW = 85;
    private static final int tileH = 135;
    private static final int realW = 165;
    private static final int realH = 262;

    // Décalage x\y
    private static final int shiftX = -15;
    private static final int shiftY = 0;

    // Temps écoulé
    private float elapsedTime = 0;

    // Objet animation
    Animation<TextureRegion> towerAnim;

    // Unit scale du dessin
    private static final float scale = 2;

    // Niveau en pixels de la barre de vie
    private static final float MAXBAR_W = 85*scale;
    private static final float BAR_H = 8*scale;
    private float currentW = MAXBAR_W;

    // Vie maximale de la tourelle
    private static final int MAX_HP = Robot.PVMAX_T;

    // Couleur courante de la barre
    private Color currentCoul;
    private Pixmap pixmap, pixcontour, pixbg;
    private Texture lifeB, lifeContour, lifebg;

    // Intervalles de couleur (vert => rouge)
    private final int[] RGB_MIN = {255, 68, 46};
    private final int[] RGB_MAX = {5,  170, 55};

    public TowerSprite(Vector3 coord, String spriteP, IsometricTiledMapRenderer renderer) {
        // On appelle le constructeur de la classe sprite générique
        super(coord, new Texture(spriteP), renderer);

        // On crée les animations de déplacement
        loadAnimation();

        // On initialise la position en pixels de la tour
        coordPix =  Math.isoToWorld(coordIso);

        /** Création de la barre de vie **/
        pixmap     = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixcontour = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixbg      = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        // Initialisation des couleurs
        pixcontour.setColor(new Color(0, 0, 0, 0.3f)); pixcontour.fill();
        pixbg.setColor(Color.WHITE); pixbg.fill();

        currentCoul = new Color(RGB_MAX[0], RGB_MAX[1], RGB_MAX[2], 1);
        pixmap.setColor(currentCoul); pixmap.fill();

        // Création des textures associées
        lifeB  = new Texture(pixmap);    lifeContour = new Texture(pixcontour);
        lifebg = new Texture(pixbg);

        setLifeTotal(MAX_HP);
    }

    // Dessine le sprite d'une tour aux coordonnées isométriques isoX, isoY
    public void draw() {
        // Mise à jour du temps écoulé
        float deltaT = Gdx.graphics.getDeltaTime();
        elapsedTime += deltaT;

        // On initialise le batch
        renderer.getBatch().begin();

        /** RENDU DU SPRITE DE LA TOUR **/
        TextureRegion textR;
        textR = towerAnim.getKeyFrame(elapsedTime, true);

        renderer.getBatch().draw(textR, coordPix.x+shiftX, coordPix.y+shiftY, realW, realH);

        /** Rendu de la barre de vie **/
        // Contour et fond
        renderer.getBatch().draw(lifeContour, coordPix.x+shiftX-4, coordPix.y+shiftY-4, MAXBAR_W+8, BAR_H+8);
        renderer.getBatch().draw(lifebg, coordPix.x+shiftX, coordPix.y+shiftY, MAXBAR_W, BAR_H);

        // Couleur de la barre :
        renderer.getBatch().draw(lifeB, coordPix.x+shiftX, coordPix.y+shiftY, currentW, BAR_H);

        // Fin du dessin
        renderer.getBatch().end();
    }

    // Crée les animations du joueur à partir du texture atlas
    private void loadAnimation() {
        TextureRegion[][] tmpFrames = TextureRegion.split(getTexture(), tileW, tileH);

        // On parcourt le tableau créé afin de fabriquer toutes les animations à partir du texture région créé
        Array<TextureRegion> frames = new Array<TextureRegion>();   // array temporaire pour stocker les frames
        // On charge toutes les frames
        for(int i = 0; i < 5; i++)
            frames.add(tmpFrames[0][i]);
        towerAnim = new Animation(FRAME_FPS, frames);
        frames.clear();
    }

    // Actualise la largeur de la barre de vie et sa couleur
    // en fonction du total en pourcentage passé en paramètres
    public void setLifeTotal (int total) {
        this.currentW = (total*MAXBAR_W)/MAX_HP;

        // => Interpolation linéaire pour avoir la valeur RGB voulue
        int r = Math.interpolate(0, RGB_MIN[0], MAX_HP, RGB_MAX[0], total);
        int g = Math.interpolate(0, RGB_MIN[1], MAX_HP, RGB_MAX[1], total);
        int b = Math.interpolate(0, RGB_MIN[2], MAX_HP, RGB_MAX[2], total);

        currentCoul.set(r/255f, g/255f, b/255f, 1);
        pixmap.setColor(currentCoul); pixmap.fill();
        lifeB.dispose();  lifeB = new Texture(pixmap);
    }
}
