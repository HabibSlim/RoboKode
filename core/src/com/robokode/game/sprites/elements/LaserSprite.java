package com.robokode.game.sprites.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.robokode.game.mvc.Action;
import com.robokode.game.screens.MainGame;
import com.robokode.game.sprites.GenericSprite;
import com.robokode.utils.Direction;
import com.robokode.utils.Math;
import com.robokode.utils.Ressources;

import static com.robokode.utils.Math.TILE_HEIGHT;
import static com.robokode.utils.Math.TILE_WIDTH;

/**
 *  Sprite d'un projectile lancé par les robots
 */
public class LaserSprite extends GenericSprite {

    // Temps en secondes de déplacement d'une case à lautre
    private static final float MOVE_T = 0.05f;

    // Temps effectif de déplacement du sprite
    private float TOTAL_T;

    // Dimensions du sprite
    private static final int tileW = 27;
    private static final int tileH = 19;

    // Dimensions de l'explosion
    private static final int exploW = 100;
    private static final int exploH = 100;

    // Textures du sprite
    private static boolean loaded = false;
    private static TextureRegion laserHG, laserHD, laserBG, laserBD;
    private TextureRegion laserCurrent;

    // Animation d'explosion
    private static Animation<TextureRegion> explodeAnim;
    private static final int SHEET_SIZE = 8; // 8x8
    private static final float FRAME_DT = 1/64f;

    // Action liée à l'explosion du sprite
    private Action linkedAction;

    // Temps écoulé
    private float startTimer = 0, elapsedTime = 0;

    // Flags d'état
    private boolean stopped = false;

    // Décalages x\y des sprites\animations
    private float[] laserS, exploS;

    // Cordonnées de la destination du projectile
    private Vector3 destPix, destIso;

    private MainGame game;

    public LaserSprite(Vector3 orig, Vector3 dest, Direction dirT, Texture spriteP,
                  MainGame gameR, Action linkedAct, IsometricTiledMapRenderer renderer) {
        // On appelle le constructeur de la classe sprite générique
        super(orig, spriteP, renderer);

        destIso = dest;   game = gameR;     linkedAction = linkedAct;

        // On crée les animation du laser, si elles n'ont pas déjà été chargées précédemment
        if (!loaded)
            loadAnimation();

        // On assigne la texture utilisée dans cette instance à laserCurrent
        switch (dirT) {
            case BD:
                laserCurrent = laserBD;
            break;
            case HD:
                laserCurrent = laserHD;
            break;
            case HG:
                laserCurrent = laserHG;
            break;
            case BG:
                laserCurrent = laserBG;
            break;
        }

        // On initialise la position en pixels du sprite, et sa destination
        // On décale les coordonnées finales pour un effet plus réaliste
        destPix = Math.isoToWorld(dest);
        coordPix =  Math.isoToWorld(coordIso);

        // On calcule le temps total de déplacement du projectile
        TOTAL_T = Math.distance(orig, dest) * MOVE_T;

        // On calcule les décalages x\y du dessin
        laserS = getLaserShift(dirT);
        exploS = getExplosionShift(dirT);

        // Correction des positions du sprite
        Math.correctLaser(coordPix, destPix);
    }

    // Dessine le sprite d'une tour aux coordonnées isométriques isoX, isoY
    public void draw() {
        // Mise à jour du temps écoulé
        float deltaT = Gdx.graphics.getDeltaTime();
        elapsedTime += deltaT;

        /** RENDU DU SPRITE **/
        Vector3 coord;

        // Si le sprite est en mouvement, on le déplace progressivement vers la destination
        if (!isStopped()) {
            if (startTimer == 0) startTimer = elapsedTime;

            // On calcule le temps écoulé depuis le début du mouvement
            float ratio = 1 - (elapsedTime - startTimer) / TOTAL_T;

            // Si le temps de déplacement est écoulé on s'arrête
            if (ratio < 0.05) {
                // On réinitialise les booléens et mesures de temps
                stopped = true;
                startTimer = 0; elapsedTime = 0;

                // On redéfinit la position du sprite : il est arrivé
                coordPix.x = destPix.x;
                coordPix.y = destPix.y;
                coordIso.x = destIso.x;
                coordIso.y = destIso.y;

                // On positionne le sprite sur la case finale
                coord = coordPix;

                // Si une action liée a été ajoutée, on l'exécute :
                if (linkedAction != null) {
                    if (linkedAction.type == Action.ActionType.SET_HP_TOUR)
                        game.getTourelle(linkedAction.indexT).setLifeTotal(linkedAction.HP);
                    else if (linkedAction.type == Action.ActionType.SET_HP_ROBOT)
                        game.getRobot(linkedAction.indexJ).setLifeTotal(linkedAction.HP);

                    // On remet à zéro l'action liée
                    linkedAction = null;
                }

            } else { // Sinon on déplace le personnage vers la destination de manière progressive
                coord = new Vector3();

                coord.x = destPix.x - ratio * (destPix.x - coordPix.x);
                coord.y = destPix.y - ratio * (destPix.y - coordPix.y);
            }
        } else {
            // On positionne directement le sprite en coordonnée cartésiennes isoX, isoY
            coordPix = Math.isoToWorldPSP(coordIso);
            coord = coordPix;
        }

        // On exécute le rendu : on dessine la frame active
        // => si le joueur est immobile on renvoie une frame statique, sinon on renvoie la frame animée correspondante
        renderer.getBatch().begin();
        TextureRegion textR;
        if (!isStopped()) {
            textR = laserCurrent;
            renderer.getBatch().draw(textR, coord.x+laserS[0], coord.y+laserS[1], tileW, tileH);
        }
        else {
            // => On récupère la texture de l'animation de l'explosion cool
            if (explodeAnim.isAnimationFinished(elapsedTime)) {
                // => Y'a rien à voir, l'objet doit être détruit.
                // => Les textures chargées statiquement ne sont pas déréférencées : magique Java
                game.removeLaserSprite(this);
            } else {
                textR = explodeAnim.getKeyFrame(elapsedTime, false);
                renderer.getBatch().draw(textR, coord.x+exploS[0], coord.y+exploS[1], exploW*2, exploH*2);
            }
        }

        renderer.getBatch().end();
    }

    // Renvoie vrai si le projectile est entré en collision avec un objet
    public boolean isStopped() { return this.stopped; }

    // Crée les animations du sprite à partir du texture atlas
    private void loadAnimation() {
        loaded = true;

        /** Chargement du sprite du laser **/
        TextureRegion[][] tmpFrames = TextureRegion.split(getTexture(), tileW, tileH);

        laserHD = tmpFrames[0][0];      laserHG = tmpFrames[0][1];
        laserBD = tmpFrames[1][0];      laserBG = tmpFrames[1][1];

        /** Chargement de l'animation d'explosion **/
        tmpFrames = TextureRegion.split(Ressources.getOtherSprite(2), exploW, exploH);

        // On parcourt le tableau créé afin de fabriquer toutes les animations à partir du texture région créé
        Array<TextureRegion> frames = new Array<TextureRegion>();   // array temporaire pour stocker les frames

        for(int j = 0; j < SHEET_SIZE; j++)
            for(int i = 0; i < SHEET_SIZE; i++)
                frames.add(tmpFrames[j][i]);
        explodeAnim = new Animation(FRAME_DT, frames);
        frames.clear();
    }

    // Renvoie le décalage x/y du sprite laser en fonction de la direction du tir
    private float[] getLaserShift(Direction dir) {
        switch (dir) {
            case HG:
                return new float[] {(int)((-1/12f)*TILE_WIDTH), (int)((0.8+1/15f)*TILE_HEIGHT)};
            case HD:
                return new float[] {TILE_WIDTH, (int)((0.9)*TILE_HEIGHT)};
            case BG:
                return new float[] {(int)((0.35)*TILE_WIDTH), (int)((1/6f)*TILE_HEIGHT)};
            case BD:
            default:
                return new float[] {(int)((0.6)*TILE_WIDTH), (int)((1/6f)*TILE_HEIGHT)};
        }
    }

    // Renvoie le décalage x/y de l'animation d'explosion en fonction de la direction du tir
    private float[] getExplosionShift(Direction dir) {
        switch (dir) {
            case HG:
                return new float[] {0, (int)((-0.1)*TILE_HEIGHT)};
            case HD:
                return new float[] {(int)((0.4)*TILE_WIDTH), 0};
            case BG:
                return new float[] {(int)((0.8)*TILE_WIDTH), 0};
            case BD:
            default:
                return new float[] {0, (int)((-0.4)*TILE_HEIGHT)};
        }
    }

    // Si le sprite est en phase d'explosion, il est dessiné en priorité sur les autres sprites
    public int compareTo(GenericSprite obj) {
        if (isStopped())
            return 1;
        else
            return super.compareTo(obj);
    }
}
