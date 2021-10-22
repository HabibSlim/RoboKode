package com.robokode.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.robokode.game.camera.MenuCamera;
import com.robokode.game.mvc.Action;
import com.robokode.game.mvc.Message;
import com.robokode.game.mvc.Observateur;
import com.robokode.game.screens.LevelChoose;
import com.robokode.game.screens.MainGame;
import com.robokode.game.screens.MainMenu;
import com.robokode.game.ui.GameFrame;
import com.robokode.game.ui.IconList;
import com.robokode.game.ui.LevelInfo;
import com.robokode.model.Controleur;
import com.robokode.utils.Ressources;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** RoboGame est le contrôleur des vues du jeu **/
public class RoboGame extends Game implements Observateur {
    // Largeur du jeu isométrique
    public static final int G_WIDTH = 731;
    public static final int G_HEIGHT = 653; // + (RoboGame.isWindows() ? -11 : 0); // Ajustement si le programme tourne sous Win

    // Largeur de la fenêtre
    public static final int W_WIDTH = 1250; // + (RoboGame.isWindows() ? 6 : 0); // Ajustement si le programme tourne sous Win;
    public static final int W_HEIGHT = G_HEIGHT;

    private SpriteBatch batch;
    private MenuCamera cam;

    // Contrôleur
    private final Controleur ctrl;

    // Frame du jeu
    private final GameFrame gFrame;

    // Etats du jeu
    public enum EtatJeu {
        MAIN_MENU,
        MAIN_GAME,
        SELECT_LVL
    }
    private EtatJeu etat;

    // Ecrans du jeu
    private HashMap<EtatJeu,Screen> ecrans;

    // Index du niveau courant
    private int mapIndex;

    public RoboGame(Controleur ctrl, GameFrame gFrame) {
        this.ctrl = ctrl;
        this.gFrame = gFrame;
        this.ecrans = new HashMap<EtatJeu, Screen>();
    }

	/* Méthodes de classe
    * =================================== */

    /** public : **/
    // Traitement des messages
    @Override
    public void traiterMessage(Message msg) {
        if (etat == EtatJeu.MAIN_GAME) {
            // Switch géant
            switch (msg.type) {
                // Ajout d'éléments graphiques
                case ADD_ROBOT:
                    // On ajoute un robot sur la map, aux coordonnées demandées, et du type demandé
                    // Si une orientation est précisée, on l'utilise
                    if (msg.orientation != null) {
                        getMainGame().creerPlayerSprite(msg.x, msg.y, msg.num, msg.orientation);
                    } else
                        getMainGame().creerPlayerSprite(msg.x, msg.y, msg.num);
                break;
                case ADD_TOWER:
                    getMainGame().createTowerSprite(msg.x, msg.y);
                break;
                case RESET:
                    // On supprime tous les sprites de la vue
                    // => Pour éviter les pb de concurrence on lance sur le thread GDX
                    getMainGame().deleteAllSprites();
                break;
                case ACTIONS:
                    // On parcourt les actions à effectuer, et on les distribue à chaque robot dans l'ordre
                    for (Action action : msg.actionList) {
                        getMainGame().getRobot(action.indexJ).addAction(action);
                    }
                break;
                case ADD_LIST_MSG:
                    // On ajoute un item à la liste des actions
                    // Si l'opération n'est pas faite sur le thread FX, on l'ajoute en Runnable
                    try {
                        Toolkit.getToolkit().checkFxUserThread();
                        gFrame.addInfoList(msg.iconType, msg.text);
                    } catch(IllegalStateException ex) {
                        Platform.runLater(() -> gFrame.addInfoList(msg.iconType, msg.text));
                    }
                break;
                case ADD_LIST_ERROR:
                    // On ajoute à la liste de statut les erreurs syntaxiques détectées
                    ArrayList<String> tokenList = new ArrayList<>();// Si l'opération n'est pas faite sur le thread FX, on l'ajoute en Runnable
                    Runnable operation = () -> {
                        for (String[] errStr : msg.errorList) {
                            gFrame.addInfoList(IconList.IconType.ERROR, errStr[0], errStr[1]);
                            // Si le token est à sélectionner dans le codePane, on l'ajoute à la liste
                            if (errStr[2].equals("w")) tokenList.add(errStr[1]);
                        }

                        // S'il y a des token erreur à sélectionner, on les envoie à la vue
                        if (tokenList.size()>0)   gFrame.selectErrors(tokenList);
                    };

                    try {
                        Toolkit.getToolkit().checkFxUserThread();
                        operation.run(); // On exécute l'opération dans le thread courant
                    } catch(IllegalStateException ex) {
                        Platform.runLater(operation::run); // On exécute l'opération dans le thread FX
                    }
                break;
            }
        }
    }

    // Traitement d'une liste de messages de manière successive
    @Override
    public void traiterMessage(List<Message> listeMsg) {
        for (Message msg : listeMsg)
            // Sinon on traite le message normalement
            traiterMessage(msg);
    }

    // Change l'écran mode du jeu
    public void setScreenState(EtatJeu etat) {
        // On définit l'état
        this.etat = etat;

        // On crée l'écran que l'on veut afficher, s'il n'a pas déjà été créé
        Screen sC = createScreen(etat);

        switch (etat) {
            case MAIN_GAME:
                // => On prépare le popup d'informations sur le niveau
                gFrame.getLevelInfo().loadLevelInfo(mapIndex);

                // On redimensionne le canvas du jeu pour qu'il prenne une partie de la fenêtre
                gFrame.setGameMode();

                // On définit l'écran de jeu
                setScreen(sC);

                // On lance la partie en mode tutoriel
                // => On envoie en paramètres l'index du niveau choisi
                ctrl.lancerPartie(Controleur.GameMode.TUTORIAL, mapIndex);
            break;
            case MAIN_MENU:
            case SELECT_LVL:
            default:
                setScreen(sC);
            break;
        }
    }

    // Retour à la vue en plein écan
    // @param : true si le dernier niveau joué a été terminé
    public void returnLevelSelect(boolean finished) {
        // On supprime l'objet MainGame
        getMainGame().dispose();
        ecrans.remove(EtatJeu.MAIN_GAME);

        // On incrémente le niveau à afficher dans la vue de sélection des niveaux
        // On ne peut pas dépasser le nombre de niveaux maximum
        if (finished) mapIndex++;   if (mapIndex >= Ressources.NB_LEVELS) mapIndex--;

        getLvlSelect().directSetLevel(mapIndex);

        // On réinitialise l'intégralité du modèle
        ctrl.hardResetModele();

        // On redimensionne le canvas du jeu pour qu'il prenne toute la fenêtre
        gFrame.setViewMode();

        // On affiche l'écran de sélection des niveaux
        setScreenState(EtatJeu.SELECT_LVL);
    }

    // Hack : méthode "tunnel" pour accéder à LevelInfo
    public LevelInfo getLvlInfo() { return this.gFrame.getLevelInfo(); }

    // Change l'index de la map courante
    public void setMapIndex(int index) {
        this.mapIndex = index;
    }

    /** privé : **/
    // Crée un écran de jeu, s'il n'existe pas déjà
    private Screen createScreen(EtatJeu etat) {
        if (ecrans.containsKey(etat)) {
            return ecrans.get(etat);
        } else {
            Screen newS;
            switch (etat) {
                case MAIN_GAME:
                    newS = new MainGame(this, mapIndex);
                break;
                case MAIN_MENU:
                    newS = new MainMenu(this);
                break;
                case SELECT_LVL:
                    newS = new LevelChoose(this);
                break;
                default:
                    newS = null; // => Déclenche NullPointerException car ne devrait jamais arriver
            }

            ecrans.put(etat, newS);
            return newS;
        }
    }

    // Renvoie l'objet écran du menu principal du jeu
    private MainGame getMainGame() {
        return (MainGame)(this.ecrans.get(EtatJeu.MAIN_GAME));
    }

    // Renvoie l'objet écran du menu de sélection des niveaux
    private LevelChoose getLvlSelect() {
        return (LevelChoose)(this.ecrans.get(EtatJeu.SELECT_LVL));
    }

	/* Autres méthodes surchargées de Game
    * =================================== */

    @Override
    public void create () {
        batch = new SpriteBatch();

        // On crée la caméra du menu
        cam = new MenuCamera(W_WIDTH, W_HEIGHT);

        // On affiche le menu du jeu :
        setScreenState(EtatJeu.MAIN_MENU);
    }

    @Override
    public void render () {
        batch.setProjectionMatrix(cam.combined());
        super.render();
    }

    @Override
    public void resize(int width, int height) {
        cam.update(width, height);
        super.resize(width, height);
    }

    private static boolean isWindows()
    {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
