package com.robokode.game.ui;

import com.robokode.game.RoboGame;
import com.robokode.utils.Ressources;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static javafx.stage.StageStyle.TRANSPARENT;

/**
 * Popup d'information présentant le niveau à venir
 **/
public class LevelInfo extends Stage {

    /** Dimensions des éléments graphiques de l'écran d'introduction */
    public final static int DIALOG_W = 900;
    public final static int DIALOG_H = 570;
    private final static int HEADER_W = 900;
    private final static int HEADER_H = 110;
    private final static int BROWSER_H = 400;
    private final static int BOTTOM_H = 60;
    private final static int VALIDER_BTNW = 200;

    /** Dimensions des éléments graphiques de l'écran de fin de niveau */
    public final static int FINISH_W = 820;
    public final static int FINISH_H = 110;
    public final static int LEVEL_W = 430;
    public final static int LEVEL_H = 190;
    public final static int NEXBTN_S = 130;

    /** Contenu graphique : écran de chargement **/
    private ImageView imgV, imgB;
    private WebEngine webEngine;
    private WebView browser;
    private Button start;

    private JFrame parentFrame;
    private ComponentAdapter currentAdapter;

    /** Contenu graphique : écran de victoire **/
    private ImageView finish, starIcon;
    private Button nextBtn;

    // Etat de la fenêtre
    public boolean levelMode = false;

    public LevelInfo(JFrame mainFrame) {
        // On enregistre la référence au frame parent
        parentFrame = mainFrame;

        // Calcul de la taille de la bordure de la fenêtre
        int borderWindow = RoboGame.W_HEIGHT - parentFrame.getContentPane().getHeight();

        /** Pane principal pour le popup **/
        Pane p = new Pane();

        // Définition de la couleur de fond (gris transparent)
        p.setStyle("-fx-background-color: rgba(29,29, 29, 0.5);");

        /** Header de la fenêtre de dialogue **/
        imgV = new ImageView();
        imgV.setLayoutX((RoboGame.W_WIDTH-DIALOG_W)/2);
        imgV.setLayoutY((RoboGame.W_HEIGHT-DIALOG_H+borderWindow)/2);

        p.getChildren().add(imgV);

        Scene scene = new Scene(p);
        setScene(scene);

        /** Contenu de la description du niveau **/
        browser = new WebView();
        webEngine = browser.getEngine();

        // Quand le contenu HTML est créé est chargé, on veut en notifier la fenêtre parente
        // On initialise le countdown

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // => On affiche le panneau du niveau
                showPane();
            }
        });
        browser.setMaxWidth(DIALOG_W);      browser.setMaxHeight(BROWSER_H);
        browser.setMinWidth(DIALOG_W);      browser.setMinHeight(BROWSER_H);
        browser.setLayoutX((RoboGame.W_WIDTH-DIALOG_W)/2);
        browser.setLayoutY((RoboGame.W_HEIGHT-DIALOG_H-borderWindow)/2+borderWindow+HEADER_H);
        p.getChildren().add(browser);

        /** Image du bas de la fenêtre **/
        imgB = new ImageView();
        imgB.setImage(new Image("file:res/UI/headers/bottomW.png"));
        imgB.setLayoutX((RoboGame.W_WIDTH-DIALOG_W)/2);
        imgB.setLayoutY((RoboGame.W_HEIGHT-DIALOG_H-borderWindow)/2+borderWindow+HEADER_H+BROWSER_H);

        p.getChildren().add(imgB);

        /** Contenu de la description du niveau **/
        start = new Button("Commencer");
        start.setLayoutX((RoboGame.W_WIDTH-VALIDER_BTNW)/2);
        start.setLayoutY((RoboGame.W_HEIGHT-DIALOG_H-borderWindow)/2+borderWindow+HEADER_H+BROWSER_H+5);
        start.setId("commencer");
        p.getChildren().add(start);

        // Action listener sur le bouton
        start.setOnAction(event -> {
            // On cache le panneau JFX
            hidePane();
        });

        /** Création de l'en-tête "Finish" **/
        finish = new ImageView();
        finish.setImage(new Image(Ressources.getLvlPaneImg(0)));
        finish.setLayoutX((RoboGame.W_WIDTH-FINISH_W)/2);        finish.setLayoutY(70);

        starIcon = new ImageView();
        starIcon.setLayoutX((RoboGame.W_WIDTH-LEVEL_W)/2);       starIcon.setLayoutY((RoboGame.W_HEIGHT-LEVEL_H)/2);

        nextBtn = new Button ("");
        nextBtn.setLayoutX((RoboGame.W_WIDTH-NEXBTN_S)/2);       nextBtn.setLayoutY(RoboGame.W_HEIGHT-NEXBTN_S-75);
        nextBtn.setId("nextBtn");

        // Logique du bouton
        nextBtn.setOnAction(event -> {
            // On cache le panneau JFX
            hidePane();
            // On envoie un message pour repasser en mode sélection des niveaux
            SwingUtilities.invokeLater(()-> {
                ((GameFrame)mainFrame).getRoboGame().returnLevelSelect(true);
            });
        });

        // Ajout des éléments au panneau principal
        p.getChildren().add(finish);        p.getChildren().add(starIcon);        p.getChildren().add(nextBtn);
        finish.setVisible(false);           starIcon.setVisible(false);           nextBtn.setVisible(false);

        /** Configuration de la scène et du stage **/
        scene.setFill(null);
        initStyle(TRANSPARENT);

        // On bloque la fermeture manuelle de la fenêtre
        setOnCloseRequest(event -> event.consume());
        setWidth(RoboGame.W_WIDTH);    setHeight(RoboGame.W_HEIGHT);
        setAlwaysOnTop(true);

        centerFrame();
    }

    // Méthode chargeant le panneau d'introduction d'un niveau
    public void loadLevelInfo(int index) {
        Platform.runLater(() -> {
            if (!levelMode) {
                levelMode = true;
                // On masque les éléments relatifs à l'écran fin de niveau
                finish.setVisible(false);           starIcon.setVisible(false);           nextBtn.setVisible(false);

                // On affiche les éléments relatifs à l'écran d'intro des niveaux
                imgV.setVisible(true);                 imgB.setVisible(true);
                browser.setVisible(true);              start.setVisible(true);
            }
            // MapInfo.getHTML() => : index
            webEngine.load(Ressources.getLvlURI(index));
            // Ressources.getHeaderLvl(index) => :
            imgV.setImage(new Image(Ressources.getLvlHeaderURL(index)));
        });
    }

    // Méthode chargeant le panneau de victoire à la fin d'un niveau
    // starW  :  victoire du niveau soit 1\2\3 étoiles
    public void loadVictoryScreen(int starW) {
        Platform.runLater(() -> {
            starIcon.setImage(new Image(Ressources.getLvlPaneImg(starW)));
            if (levelMode) {
                levelMode = false;
                // On masque les éléments relatifs à l'écran d'intro des niveaux
                imgV.setVisible(false);                 imgB.setVisible(false);
                browser.setVisible(false);              start.setVisible(false);

                // On affiche les éléments de cet écran
                finish.setVisible(true);           starIcon.setVisible(true);           nextBtn.setVisible(true);
            }
            showPane();
        });
    }

    // On affiche le panneau
    public void showPane() {
        // On désactive l'input sur la fenêtre principale
        parentFrame.setEnabled(false);

        // On ajoute un listener sur le mouvement de la fenêtre parente
        currentAdapter = new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                centerFrame();      // Si la fenêtre parente est déplacée, on veut que le popup suive
            }
        };
        parentFrame.addComponentListener(currentAdapter);

        // On actualise la position du Stage et on l'affiche
        Platform.runLater(() -> {
            centerFrame();
            show();
        });
    }

    public void hidePane() {
        // On affiche le panneau
        parentFrame.setEnabled(true);
        Platform.runLater(() -> {
            hide();
        });

        // On supprime le listener
        parentFrame.removeComponentListener(currentAdapter);
    }

    // On actualise la position du frame par rapport au parent
    public void centerFrame() {
        setX(parentFrame.getLocation().x);            setY(parentFrame.getLocation().y);
    }
}