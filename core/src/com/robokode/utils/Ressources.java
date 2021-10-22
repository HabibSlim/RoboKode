/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.robokode.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;


/**
 *
 * Classe utilisant le pattern singleton pour charger les ressources de jeu progressivement
 */
public class Ressources {
    // Constantes du jeu
    public static final int NB_LEVELS = 5;

    // Classe statique servant à distribuer les fichiers ressources parmi les différentes classes
    private static String workDir = "res/";

    // Noms des fichiers associés aux tutoriels
    private static String[] maps = {"tuto0.", "tuto1.", "tuto2.", "tuto3.", "tuto4."};

    // Ressources chargées statiquement
    private HashMap<String,Texture> textureMap;

    private static Ressources instance;

    private Ressources() {
        this.textureMap = new HashMap<>();
    }

    private static Ressources getInstance() {
        if (instance == null) {
            instance = new Ressources();
        }
        return instance;
    }

    /**   Méthodes d'accès aux fichiers
     * ================================= **/

    /**   UI   **/

    // Renvoie le chemin vers les boutons des menus
    public static String getMenuBtn(int index) {
        String[] btnAdr = {"jouer.png", "param.png", "quitter.png",
                "tutoriel.png", "multijoueur.png", "menu_principal.png",
                "leftSlide.png", "rightSlide.png", };

        return workDir + "UI/menu/" + btnAdr[index];
    }

    // Renvoie l'icône correspondant à une icône
    public static Texture getIconBtn(int index) {
        String[] btnAdr = {"jouer_ico.png", "param_ico.png", "quitter_ico.png",
                "barA.png", "barB.png", "barC.png",
                "leftHover.png", "rightHover.png"};

        return actualizeMap(btnAdr[index], workDir + "UI/menu/");
    }

    /**   Maps   **/

    // Renvoie le chemin vers le fichier TMX de la map à partir de son index
    public static String getMap(int index) {
        return workDir + "Maps/" + maps[index] + "tmx";
    }

    // Renvoie le chemin vers le fichier RKMP de l'info de la map
    public static String getMapInfo(int index) {
        return workDir + "Maps/info/" + maps[index] + "rkmp";
    }

    /**   Sprites   **/

    // Renvoie le sprite d'un robot
    //    type = 0 : bleu
    //    type = 1 : rouge
    public static Texture getPlayerSprite(int type) {
        // Initialisation du tableau des sprites
        String[] sprites = {"fullSheet_blue.png", "fullSheet_red.png"};
        //String[] sprites = {"spritesheet_small.png"};

        return actualizeMap(sprites[type], workDir + "Sprites/");
    }

    // Renvoie le sprite d'une tour
    public static String getTowerSprite(int index) {
        // Initialisation du tableau des sprites
        String[] sprites = {"anim_allume4.png"};

        return workDir + "Sprites/" + sprites[index];
    }

    // Autres sprites utiles
    public static Texture getOtherSprite(int index) {
        // Initialisation du tableau des sprites
        String[] sprites = {"projectiles/blueLaserS.png", "projectiles/redLaserS.png", "projectiles/explo1.png"};

        return actualizeMap(sprites[index], workDir + "Sprites/");
    }

    // Renvoie les sprites des icônes d'action
    public static Texture getBulle(int index) {
        // Initialisation du tableau des sprites
        String[] sprites = {"bulle_obst.png", "bulle_infinite.png", "bulle_finish.png", "bulle_question.png", "bulle_wait.png"};

        return actualizeMap(sprites[index], workDir + "Sprites/icons/");
    }

    // Charge et renvoie les aperçus des niveaux
    public static Sprite[] getLvlPreviews() {
        Sprite[] allPreviews = new Sprite[NB_LEVELS];

        for (int i=0; i<Ressources.NB_LEVELS; i++) {
            allPreviews[i] = new Sprite(actualizeMap(i+".png", workDir + "UI/menu/levelPreview/"));
        }

        return allPreviews;
    }

    // Charge et envoie les éléments du HUD
    public static Texture getHUDElement(int index) {
        // Initialisation du tableau des sprites
        String[] sprites = {"lfullBar.png", "lcircle.png", "portrait/robot_blue.png", "lshade.png"};

        return actualizeMap(sprites[index], workDir + "UI/HUD/");
    }

    // Autres éléments graphiques
    public static Texture getImgRsc(int index) {
        // Initialisation du tableau des sprites
        String[] sprites = {"fond.png", "roue_big.png", "roue_sml.png",
                "logo.png", "fullCircle.png", "levelbar.png", "innerShade.png"};

        return actualizeMap(sprites[index], workDir + "UI/menu/");
    }

    public static Texture getOtherImg(int index) {
        String[] allImg = {"loading.png", "loading_blur.png", "robot_ico.png"};

        return actualizeMap(allImg[index], workDir + "UI/other/");
    }

    /**   Autres ressources   **/

    public static Theme getCodeEditorTheme() {
        Theme theme = null; // petit hack
        String addr = workDir + "UI/editor.xml";

        try {
            theme = Theme.load(new FileInputStream(addr));
        } catch (IOException e) {
            ConsoleLog.put("Ressources : impossible de lire : "+addr, true);
        }

        return theme;
    }

    public static String getCSS_URL() {
        String addr = "file:" + workDir + "UI/uistyle.css";

        return addr;
    }

    public static String getLvlURI(int index) {
        String path = workDir + "UI/html/niveau" + index + ".html";

        return new File(path).toURI().toString();
    }

    public static String getLvlHeaderURL(int index) {
        String path =  "file:"+workDir+"UI/headers/header" + index + ".png";

        return path;
    }

    public static String getIcon_URL() {
        String addr = "file:" + workDir + "UI/icons/listIcons/";

        return addr;
    }

    public static String getLvlPaneImg(int index) {
        String[] allImg = {"finish.png", "level1.png", "level2.png", "level3.png"};

        return "file:"+ workDir + "UI/other/" + allImg[index];
    }

    /**  Utilitaire  **/

    // Met en mémoire les textures sollicitées
    // Amélioration => Cache de type LRU avec libération des ressources
    private static Texture actualizeMap(String index, String path) {
        Ressources res = getInstance();

        // Si l'image à charger n'est pas déjà en mémoire, on l'ajoute à la textureMap
        if (!res.textureMap.containsKey(index)) {
            res.textureMap.put(index, new Texture(path + index));
        }

        return res.textureMap.get(index);
    }
}