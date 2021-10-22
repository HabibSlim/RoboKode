package com.robokode.game.sprites.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.robokode.game.mvc.Action;
import com.robokode.game.screens.MainGame;
import com.robokode.game.sprites.GenericSprite;
import com.robokode.model.Robot;
import com.robokode.utils.*;
import com.robokode.utils.Math;

import java.util.ArrayList;
import java.util.HashMap;


// Classe servant à gérer les ressources graphiques représentant le joueur
public class PlayerSprite extends GenericSprite {

    // Unité de temps pour chaque action du sprite
    private static final float UNIT_T = 0.7f;

    // Nombre de frames dans les animations
    private static final int NB_FRAMES = 9;

    // Temps entre chaque frame
    // => On veut que toutes les frames s'affichent à chaque déplacement
    // Il y a 9 frames : => FRAME_DT = UNIT_T/(nFrame-1)
    private static final float FRAME_DT = UNIT_T /(NB_FRAMES-1);

    // Temps en secondes de pause entre chaque action\déplacement
    private static final float PAUSE_T = 0.2f;

    // Temps en secondes d'affichage des bulles d'info
    // => peut-être à changer en UNIT_T pour éviter les bugs ?
    private static final float BULLE_T = UNIT_T+1;

    // Dimensions du sprite robot
    private static final int tileW = 142;
    private static final int tileH = 142;

    // Temps écoulé : animations
    private float elapsedTime, pauseTime = 0;

    // Temps de déplacement
    private float startTimer = 0;

    // Temps d'affichage des bulles informatives
    private float bulleTimer = 0;

    // Temps d'attente après chaque tir
    private float shootTimer = 0;

    // Pause statique
    private float waitTimer = 0;

    // Coordonnées de destination du personnage
    // => isométrique :              // => pixels :
    private Vector3 destIso;         private Vector3 destPix;

    // Animations de déplacement (haut-gauche, haut-droit, ...)
    private Animation AnimHG, AnimHD, AnimBG, AnimBD;

    // Textures des positions statiques (haut-gauche, haut-droit, ...)
    private TextureRegion StaticHG, StaticHD, StaticBG, StaticBD;

    // Autres textures
    private HashMap<BulleType, Sprite> spriteMap;

    private BulleType bulle = BulleType.DEFAULT;

    // Liste des mouvements du sprite encore à effectuer
    private ArrayList<Action> actionList;

    // Statut du joueur
    private State player_state;

    // Référence à l'objet parent
    private MainGame mainGame;

    // Type de sprite :
    // 0 : bleu, 1 : rouge
    private int spriteType;

    // Flags d'état du sprite
    private boolean statique = true, busy = false, shooting = false, waiting = false;

    /** Barre de vie du robot **/
    private boolean hasLifeBar = false;

    // Décalage x\y
    private static final int shiftX = 0;
    private static final int shiftY = tileH;

    // Unit scale du dessin
    private static final float scale = 2;

    // Niveau en pixels de la barre de vie
    private static final float MAXBAR_W = 65*scale;
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

    public PlayerSprite(Vector3 coord, int type, MainGame mainG, IsometricTiledMapRenderer renderer) {
        // On appelle le constructeur de la classe sprite générique
        super(coord, Ressources.getPlayerSprite(type), renderer);

        // Initialisation des attributs de classe
        this.destPix = new Vector3();    this.destIso = new Vector3();
        this.mainGame = mainG;           this.spriteType = type;

        // On initialise les collections
        actionList = new ArrayList<Action>();
        spriteMap = new HashMap<BulleType, Sprite>();

        // On crée les animations de déplacement
        loadAnimation();

        // On charge en mémoire les autres sprites
        loadSprites();

        // Le joueur commence la partie dans la position HD
        player_state = State.STATIC_HD;
        setRegion(StaticHD);
    }

    // Dessine le sprite d'un joueur aux coordonnées isométriques isoX, isoY
    // Et les sprites associés au joueur (bulles\effets\etc.)
    public void draw() {
        Vector3 coord;

        // Mise à jour du temps écoulé
        float deltaT = Gdx.graphics.getDeltaTime();
        elapsedTime += deltaT;

        // On initialise le batch
        renderer.getBatch().begin();

        /** MISE A JOUR DES ACTIONS **/
        // S'il reste des déplacements à effectuer dans la liste des actions
        // et que le sprite n'est pas occupé, on les applique
        if (actionsPending() && isStatic() & !busy)  {
            pauseTime += deltaT;
            if (pauseTime > PAUSE_T) {
                pauseTime = 0;
                Action act = popNextAction();

                // On effectue l'action selon ce qu'il reste à faire
                switch (act.type) {
                    case BULLE:
                        bulleErreur(act.bulle);
                    break;
                    case MOVE:
                        moveDir(act.dir);
                    break;
                    case SHOOT:
                        shootDir(act.dir, act.coord, act.linkedAction);
                    break;
                    case GAGNE_TUTO:
                        mainGame.getLvlInfo().loadVictoryScreen(act.nbEtoiles);
                    break;
                    case WAIT:
                        waiting = true;     busy = true;
                    break;
                }
            }
        }

        // Si le robot est dans un temps d'attente, on attend qu'il soit écoulé
        if (waiting) {
            if (waitTimer == 0) waitTimer = elapsedTime;

            if ((elapsedTime - waitTimer) > UNIT_T) {
                // On a fini le temps de pause pour tirer des trucs
                waiting = false;   busy = false;   waitTimer = 0;
            }
        }

        // Si le robot est en train de tirer un truc, on utilise un timer pour limiter le temps
        if (shooting) {
            if (shootTimer == 0) shootTimer = elapsedTime;

            if ((elapsedTime - shootTimer) > UNIT_T) {
                // On a fini le temps de pause pour tirer des trucs
                shooting = false;   busy = false;   shootTimer = 0;
            }
        }

        /** RENDU DU SPRITE ROBOT **/
        // Si le joueur est en mouvement, on le déplace progressivement vers la destination
        if (!isStatic() & !busy) {
            if (startTimer == 0) { startTimer = elapsedTime; }

            // On calcule le temps écoulé depuis le début du mouvement
            float ratio = 1 - (elapsedTime - startTimer) / UNIT_T;

            // Si le temps de déplacement est écoulé on s'arrête
            if (ratio < 0.05) {

                // On réinitialise les booléens et mesures de temps
                statique = true;
                startTimer = 0; // elapsedTime = 0;

                // On redéfinit l'état courant du joueur
                player_state = moveToStatic();

                // On redéfinit la position du joueur : il est arrivé
                coordPix.x = destPix.x;
                coordPix.y = destPix.y;
                coordIso.x = destIso.x;
                coordIso.y = destIso.y;

                // On positionne le personnage sur la case finale
                coord = coordPix;

            } else { // Sinon on déplace le personnage vers la destination
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
        TextureRegion textR;
        if (isStatic() || busy)
            textR = getStaticFrame();
        else
            textR = getWalkAnim().getKeyFrame(elapsedTime, true);

        renderer.getBatch().draw(textR, coord.x, coord.y, tileW, tileH);

        /** RENDU DES AUTRES SPRITES **/
        // Si on doit afficher une bulle d'action
        if (bulleErreur()) {
            if (bulleTimer == 0) bulleTimer = elapsedTime;

            // On calcule le temps écoulé depuis l'affichage de la bulle
            float ratio = 1 - (elapsedTime - bulleTimer) / BULLE_T;

            // Si le temps d'affichage de la bulle est écoulé, on arrête
            if (ratio < 0.05) {
                // On réinitialise l'état de la bulle, et le timer
                bulle = BulleType.DEFAULT;  bulleTimer = 0;
                busy = false;

            } else {
            // Sinon on affiche la bulle avec une opacité multiple du ratio, au dessus de la tête du robot
                spriteMap.get(bulle).setAlpha(ratio);
                spriteMap.get(bulle).setPosition(coord.x-tileW, coord.y + tileH);
                spriteMap.get(bulle).setSize(200, 200);
                spriteMap.get(bulle).draw(renderer.getBatch());
            }
        }

        /** RENDU DE LA BARRE DE VIE **/
        if (hasLifeBar) {
            /** Rendu de la barre de vie **/
            // Contour et fond
            renderer.getBatch().draw(lifeContour, coord.x+shiftX-4, coord.y+shiftY-4, MAXBAR_W+8, BAR_H+8);
            renderer.getBatch().draw(lifebg, coord.x+shiftX, coord.y+shiftY, MAXBAR_W, BAR_H);

            // Couleur de la barre :
            renderer.getBatch().draw(lifeB, coord.x+shiftX, coord.y+shiftY, currentW, BAR_H);
        }

        // Fin du dessin
        renderer.getBatch().end();
    }

    // Ajoute une action à la liste des actions du joueur
    public void addAction(Action act) {
        // On ajoute toutes les directions à la liste
        this.actionList.add(act);

    }

    // Définit une bulle erreur à afficher au dessus de la tête du robot
    public void bulleErreur(BulleType bulleT) {
        this.bulle = bulleT;
        busy = true; // Le sprite ne bouge pas pendant l'affichage de la bulle
    }

    // Déplace le sprite du joueur dans la direction dir
    // => définit la case destination
    public void moveDir(Direction dir) {
        // On ne peut déplacer un joueur que si il est statique
        if (this.statique) {
            this.statique = false;
            switch(dir) {
                case HD:
                    player_state = State.MARCHE_HD;
                    this.destIso.x = this.coordIso.x;
                    this.destIso.y = this.coordIso.y +1;
                    break;
                case HG:
                    player_state = State.MARCHE_HG;
                    this.destIso.x = this.coordIso.x - 1;
                    this.destIso.y = this.coordIso.y;
                    break;
                case BD:
                    player_state = State.MARCHE_BD;
                    this.destIso.x = this.coordIso.x + 1;
                    this.destIso.y = this.coordIso.y;
                    break;
                case BG:
                    player_state = State.MARCHE_BG;
                    this.destIso.x = this.coordIso.x;
                    this.destIso.y = this.coordIso.y -1;
                    break;
            }

            // On calcule les coordonnées en pixels de la destination
            destPix = Math.isoToWorldPSP(destIso);
        } else {
            // Si le joueur est déjà en mouvement et qu'on cherche à le déplacer
            // => y'a un gros problème
            ConsoleLog.put("PlayerSprite : Erreur dans le séquencage des mouvements.", true);
        }
    }

    // Fait tirer le joueur dans la direction dir
    // => Vers la cible (x,y)
    public void shootDir(Direction dir, int[] coord, Action linkedAction) {
        // On change l'orientation du joueur correspondant à la direction du tir
        switch (dir) {
            case HG:
                this.player_state = State.STATIC_HG;
                break;
            case HD:
                this.player_state = State.STATIC_HD;
                break;
            case BG:
                this.player_state = State.STATIC_BG;
                break;
            case BD:
                this.player_state = State.STATIC_BD;
                break;
        }

        // On crée le sprite à la bonne place, géré par l'IHM mère
        mainGame.creerLaserSprite((int)coordIso.x, (int)coordIso.y, coord[0], coord[1], spriteType, dir, linkedAction);

        // Le sprite est "occupé" pendant le tir
        busy = true;    shooting = true;
    }

    // Renvoie l'animation de déplacement correspondant à l'action en cours
    private Animation<TextureRegion> getWalkAnim() {
        switch(player_state) {
            case MARCHE_HG:
                return AnimHG;
            case MARCHE_HD:
                return AnimHD;
            case MARCHE_BG:
                return AnimBG;
            case MARCHE_BD:
            default:
                return AnimBD;
        }
    }

    // Renvoie la frame statique correspondant au statut du joueur
    public TextureRegion getStaticFrame() {
        switch(player_state) {
            case STATIC_HG:
                return StaticHG;
            case STATIC_HD:
                return StaticHD;
            case STATIC_BG:
                return StaticBG;
            case STATIC_BD:
            default:
                return StaticBD;
        }
    }

    // Renvoie l'état statique correspondant au mouvement courant
    public State moveToStatic() {
        switch (player_state) {
            case MARCHE_BD:
                return State.STATIC_BD;
            case MARCHE_BG:
                return State.STATIC_BG;
            case MARCHE_HD:
                return State.STATIC_HD;
            case MARCHE_HG:
            default:
                return State.STATIC_HG;
        }
    }

    // Crée une barre de vie pour ce sprite
    public void createLifeBar() {
        hasLifeBar = true;

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

    // Définit l'orientation du sprite robot
    public void setPlayerState(State pState) { this.player_state = pState; }

    // Crée les animations du joueur à partir du texture atlas
    private void loadAnimation() {
        TextureRegion[][] tmpFrames = TextureRegion.split(getTexture(), tileW/2, tileH/2);

        // On parcourt le tableau créé afin de fabriquer toutes les animations à partir du texture région créé
        Array<TextureRegion> frames = new Array<TextureRegion>();   // array temporaire pour stocker les frames

        for(int i = 0; i < NB_FRAMES; i++)
            frames.add(tmpFrames[0][i]);    //param: texture, x, y, width, height
        AnimBG = new Animation(FRAME_DT, frames);
        frames.clear();

        for(int i = 0; i < NB_FRAMES; i++)
            frames.add(tmpFrames[1][i]);    //param: texture, x, y, width, height
        AnimBD = new Animation(FRAME_DT, frames);
        frames.clear();

        for(int i = 0; i < NB_FRAMES; i++)
            frames.add(tmpFrames[2][i]);    //param: texture, x, y, width, height
        AnimHG = new Animation(FRAME_DT, frames);
        frames.clear();

        for(int i = 0; i < NB_FRAMES; i++)
            frames.add(tmpFrames[3][i]);    //param: texture, x, y, width, height
        AnimHD = new Animation(FRAME_DT, frames);
        frames.clear();

        // On crée toutes les textures pour les positions statiques
        StaticBG = tmpFrames[0][0];
        StaticBD = tmpFrames[1][0];
        StaticHG = tmpFrames[2][0];
        StaticHD = tmpFrames[3][0];
    }

    // Crée les autres sprites utiles à la fenêtre
    private void loadSprites() {
        /** Génération des bulles d'action **/
        // Bulle : ne peut pas marcher ici
        spriteMap.put(BulleType.NOWALK, new Sprite(Ressources.getBulle(0)));
        // Bulle : boucle infinie
        spriteMap.put(BulleType.INFINITE, new Sprite(Ressources.getBulle(1)));
        // Bulle : programme terminé
        spriteMap.put(BulleType.FINISH, new Sprite(Ressources.getBulle(2)));
        // Bulle : valeurs invalides
        spriteMap.put(BulleType.QUESTION, new Sprite(Ressources.getBulle(3)));
        // Bulle : recharger
        spriteMap.put(BulleType.WAIT, new Sprite(Ressources.getBulle(4)));
    }

    // Renvoie true si le sprite est en position statique
    private boolean isStatic() {
        return this.statique;
    }

    // Renvoie true si une bulle d'erreur doit être affichée
    private boolean bulleErreur() { return this.bulle != BulleType.DEFAULT; }

    // Renvoie true s'il reste des actions à effectuer pour le sprite
    private boolean actionsPending() { return (this.actionList.size()!=0); }

    // Renvoie la première action de la liste des actions à effectuer, et la retire de la liste
    private Action popNextAction() {
        if (actionsPending()) {
            Action act = actionList.get(0);
            actionList.remove(0);
            return act;
        }
        else
            return null;
    }
}
