package com.robokode.game.screens;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.robokode.game.RoboGame;
import com.robokode.game.camera.GameCamera;
import com.robokode.game.mvc.Action;
import com.robokode.game.screens.elements.LifeBar;
import com.robokode.game.sprites.GenericSprite;
import com.robokode.game.sprites.characters.PlayerSprite;
import com.robokode.game.sprites.characters.State;
import com.robokode.game.sprites.elements.LaserSprite;
import com.robokode.game.sprites.elements.TowerSprite;
import com.robokode.game.ui.LevelInfo;
import com.robokode.utils.Direction;
import com.robokode.utils.Math;
import com.robokode.utils.Ressources;
import com.robokode.utils.maps.MapInfo;
import com.robokode.utils.maps.MapLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainGame implements Screen {

    // Rendu TMX
    private IsometricTiledMapRenderer tiledMapRenderer;
    private TiledMap tiledMap;

    /*   Attributs RKode   */
    // Caméra de jeu
    private GameCamera cam;

    // Contrôleur de jeu
    private RoboGame game; // => à supprimer peut-être

    // Informations de la carte
    private MapInfo mapI;

    /*   Sprites et textures   */
    private HashMap<Integer, PlayerSprite> player_sp; // liste des objets de type joueur
    private ArrayList<TowerSprite>  tower_sp;  // tourelles à abattre
    private ArrayList<LaserSprite>  laser_sp;  // projectiles lancés par les joueurs

    /*   HUD   */
    private HashMap<PlayerSprite,LifeBar> lifeBars;
    private static final int LIFEBAR_H = 135;

    /*   Constantes   */
    private static final float[] BGCOLOR = {97/255f, 205/255f, 219/255f};


    public MainGame (RoboGame game, int mapID) {
        this.game = game;

        // Initialisation des collections

        this.player_sp = new HashMap<>();
        this.tower_sp  = new ArrayList<>();
        this.laser_sp  = new ArrayList<>();

        /* Chargement de la map TMX
        ====================================*/

        // Nom du fichier TMX
        mapI = MapLoader.Instance().getInfo(mapID);
        String tmxMap = mapI.tmxSrc;

        // Création de la caméra
        int[] dim = Math.getTmxDim(tmxMap);
        cam = new GameCamera(dim[0], (int)(dim[1]*1.5));

        // Chargement de la carte
        tiledMap = new TmxMapLoader().load(tmxMap);
        tiledMapRenderer = new IsometricTiledMapRenderer(tiledMap);

        // Initialisation du HashMap reliant chaque joueur à sa map
        lifeBars = new HashMap<>();
    }

    /* Méthodes publiques pour la Vue
    * =================================== */

    // Crée un sprite de joueur aux coordonnées (x,y)
    //  de couleur type :  0:"bleu" ou 1:"rouge"
    public void creerPlayerSprite(int x, int y, int type) {
        // Création d'un personnage aux coordonnées x,y
        Vector3 coordA = new Vector3 (x, y, 0);

        // On ajoute le joueur à l'ArrayList des joueurs
        PlayerSprite nSp = new PlayerSprite(coordA, type, this, tiledMapRenderer);

        // On ajoute le sprite à l'index type
        player_sp.put(type, nSp);

        // On crée une barre de vie au sprite s'il est de type 1
        if (type==1) nSp.createLifeBar();
    }

    // Crée un sprite avec une orientation particulière
    public void creerPlayerSprite(int x, int y, int type, State dir) {
        creerPlayerSprite(x, y, type);

        // On lui donne une orientation particulière au sprite avant de l'ajouter
        player_sp.get(type).setPlayerState(dir);
    }

    // Crée une tourelle cible (map TUTO_SHOOT) aux coordonnées (x,y)
    public void createTowerSprite(int x, int y) {
        // Création d'une tourelle aux coordonnées x,y
        Vector3 coord = new Vector3 (x, y, 0);

        // On crée la tourelle
        TowerSprite tSp = new TowerSprite(coord, Ressources.getTowerSprite(0), tiledMapRenderer);

        // On ajoute le sprite à l'index
        tower_sp.add(tSp);
    }

    // Crée un sprite laser de la coordonnée (x,y) à la destination (xD,yD)
    // dans la direction passée en paramètres. Le type est définit comme suit :
    //                      0 => bleu    |    1 => rouge
    public void creerLaserSprite (int x, int y, int xD, int yD, int type, Direction dir, Action linkedAction) {
        LaserSprite laserSprite = new LaserSprite(new Vector3(x, y, 0), new Vector3(xD, yD, 0),
                dir, Ressources.getOtherSprite(type), this, linkedAction, tiledMapRenderer);
        laser_sp.add(laserSprite);
    }


    // Supprime tous les sprites de la vue
    public void deleteAllSprites() {
        player_sp.clear();  laser_sp.clear();   tower_sp.clear();
    }

    /** Getters sur les objets de la vue **/

    // Renvoie le robot[idRobot] à la vue pour qu'elle puisse les manipuler
    public PlayerSprite getRobot(int idRobot) {
        return player_sp.get(idRobot);
    }

    // Renvoie la tourelle [idTourelle] à la vue pour qu'elle puisse les manipuler
    public TowerSprite getTourelle(int idTourelle) {
        return tower_sp.get(idTourelle);
    }

    /* Méthodes privées de la classe
    * =================================== */

    // Ordonne les sprites en fonction de leur position
    private void orderSprites(ArrayList<GenericSprite> allSprites) {
        // On ordonne les sprites dans la liste en fonction de leurs coordonnées
        // pour gérer la superposition
        Collections.sort(allSprites);
    }

    @Override
    public void render (float delta) {

        /** Rendu du jeu ici **/

        // Couleur de fond => images après
        Gdx.gl.glClearColor(BGCOLOR[0], BGCOLOR[1], BGCOLOR[2], 1);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Dimensions voulues du jeu
        cam.update(RoboGame.G_WIDTH, RoboGame.G_HEIGHT);

        // Rendu de la map
        tiledMapRenderer.setView((OrthographicCamera)cam.viewport.getCamera());

        // On a toujours deux couches background et relief
        int[] background = {0};  int[] relief = {1};

        // Rendu des tiles de fond : toujours en premier
        tiledMapRenderer.render(background);

        // On dessine tous les sprites dans l'ordre
        // Si la map possède un relief, on calcule l'ordre de render
        // Sinon on les dessine simplement 1 par 1

        // => on crée une liste de tous les sprites
        ArrayList<GenericSprite> allSp = new ArrayList<GenericSprite>();

        allSp.addAll(player_sp.values());   allSp.addAll(tower_sp);
        allSp.addAll(laser_sp);

        // On ordonne les sprites selon leurs coordonnées
        orderSprites(allSp);

        // => Ce bout est à simplifier
        boolean renderStatus = false;
        ArrayList<GenericSprite> rendered = new ArrayList<>();
        for (GenericSprite sprite : allSp) {
            // Si le sprite n'a pas déjà été rendu
            if (!rendered.contains(sprite)) {
                if (mapI.hasRelief() && !renderStatus) {
                    // Si l'un des objets en relief doit être dessiné avant, on le fait
                    for (int[] coord : mapI.reliefs) {
                        if (drawFirst(coord[0], coord[1], sprite)) {
                            // On dessine tous les sprites qui doivent être dessinés avant la couche de relief
                            for (GenericSprite beforeSp : allSp) {
                                if (!(beforeSp instanceof  LaserSprite)) {
                                    if (!drawFirst(coord[0], coord[1], beforeSp)) {
                                        beforeSp.draw();
                                        rendered.add(beforeSp);
                                    }
                                }
                            }
                            tiledMapRenderer.render(relief); renderStatus = true;
                            break;
                        }
                    }
                }

                // On dessine le sprite
                sprite.draw();
            }
        }

        // Si le relief n'a pas encore été dessiné, on le fait maintenant
        if (!renderStatus && mapI.hasRelief()) {
            tiledMapRenderer.render(relief);
        }

        /** Rendu du HUD **/

        // On dessine les barres de vie associées à chaque joueur (s'il y en a)
        for (LifeBar lifeB : lifeBars.values()) {
            tiledMapRenderer.getBatch().begin();
            //tiledMapRenderer.getBatch().setProjectionMatrix(new Matrix4());
            lifeB.draw(tiledMapRenderer.getBatch());
            tiledMapRenderer.getBatch().end();
        }
    }

    // Renvoie vrai si l'objet de coordonnées (x,y) doit être dessiné avant le sprite sp
    public boolean drawFirst(int x, int y, GenericSprite sp) {
        // Si l'objet est un laser en phase d'explosion, on fait toujours le rendu du sprite en dernier
        if ((sp instanceof LaserSprite) && (((LaserSprite) sp).isStopped())) return true;

        int xA, xB, yA, yB;

        xB = x;                       yB = y;
        xA = (int)sp.getCoordIso().x; yA = (int)sp.getCoordIso().y;

        if ((xB>xA)  &&  (yB<yA))  return false;
        if ((xB>xA)  && (yB==yA))  return false;
        if ((xA==xB) && (yB<yA))   return false;
        if ((xB==xA) && (yB>yA))   return true;
        if ((xB<xA)  && (yB>yA))   return true;
        if ((xB<xA)  && (yB==yA))  return true;

        return true;
    }

    // Supprime un sprite de la collection lasersprite
    public void removeLaserSprite(LaserSprite lSp) { this.laser_sp.remove(lSp); }

    // Hack : méthode "tunnel" pour accéder à LevelInfo
    public LevelInfo getLvlInfo() {
        return this.game.getLvlInfo();
    }

    @Override
    public void resize (int width, int height) {
        //
    }

    @Override
    public void pause () { }

    @Override
    public void resume () { }

    @Override
    public void hide () { }

    @Override
    public void dispose () { }

    @Override
    public void show () { }
}
