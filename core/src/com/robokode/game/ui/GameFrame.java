package com.robokode.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.robokode.game.RoboGame;
import com.robokode.model.Controleur;
import com.robokode.utils.ConsoleLog;
import com.robokode.utils.Ressources;
import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Création du JFrame principal du jeu
 */
public class GameFrame extends JFrame {
    private LwjglAWTCanvas isoCanvas;

    /* Widgets principaux */
    private RSyntaxTextArea codePanel;
    private IconList iconList;
    private JFXPanel jfxPanel;
    private LevelInfo levelInfo;

    /* Contrôleur graphique */
    private RoboGame ihm;

    private static final String defaultText = "# Les lignes commençant par un \"#\" sont des commentaires.\n" +
            "# Bon codage !\n";

    public GameFrame() {
        /* Création des objets du jeu
        * ================================= */

        // On crée le contrôleur
        final Controleur ctrl = new Controleur();

        // On crée le contrôleur graphique
        ihm = new RoboGame(ctrl, this);

        // On définit l'observateur du contrôleur
        ctrl.setObservateur(ihm);

        /* Création du JFrame
        * ================================= */
        // Configuration de la fenêtre
        //setResizable(false);
        setTitle("RoboKode");
        setLayout(null);    setBackground(new Color(0x1d1d1d));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // On crée le isoCanvas du jeu
        createIsoCanvas(ihm);

        isoCanvas.getCanvas().setBounds(0, 0, RoboGame.W_WIDTH, RoboGame.W_HEIGHT);
        add(isoCanvas.getCanvas());

        /* Création des objets graphiques
        * ================================= */

        // Création de l'éditeur de code RSyntaxTextArea
        JPanel panelC = new JPanel (new BorderLayout());
        int[] codePaneDims = {RoboGame.W_WIDTH-RoboGame.G_WIDTH, (int)(RoboGame.W_HEIGHT*0.50)+5};
        codePanel = new RSyntaxTextArea(20, 5);

        // Définition du jeu de couleur personnalisé
        Ressources.getCodeEditorTheme().apply(codePanel);

        // Création de l'analyseur syntaxique RKode
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/rKode", "com.robokode.utils.RoboKodeSyntaxMaker");
        codePanel.setSyntaxEditingStyle("text/rKode");

        // Police d'écriture
        codePanel.setFont(StyleContext.getDefaultStyleContext().getFont("Consolas", 0, 15));
        codePanel.setMargin(new Insets(0, 10, 0, 0)); // marge gauche
        codePanel.setVisible(false);

        // Positionnement de l'éditeur de code, création d'une barre de déroulement
        panelC.setBounds(RoboGame.G_WIDTH, 0, codePaneDims[0]-2, codePaneDims[1]+10);
        RTextScrollPane sPane = new RTextScrollPane(codePanel);
        panelC.add(sPane);
        add(panelC);

        // Création du panel FX contenant la liste d'action et les boutons
        jfxPanel = new JFXPanel();

        Pane rootPane = new Pane();
        Scene fxScene = new Scene(rootPane);
        jfxPanel.setScene(fxScene);

        Platform.runLater(() -> {
            /** Définition de la feuille de style pour les éléments JavaFX **/
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
            StyleManager.getInstance().addUserAgentStylesheet(Ressources.getCSS_URL());

            /** Création de la liste d'icônes **/
            iconList = new IconList(rootPane, codePaneDims[0]-3, (int)(RoboGame.W_HEIGHT*0.25)+15);
            initItemList();

            // Création du border pane et de la scène
            HBox btnPanel = new HBox(5);    btnPanel.setId("btnBg");

            // Création des boutons
            btnPanel.setPadding(new javafx.geometry.Insets(20));
            btnPanel.setAlignment(Pos.CENTER);

            Button valider = new Button("Valider");
            valider.setId("valider");

            Button restart = new Button("Recommencer");
            restart.setId("restart");

            Button home = new Button("");
            home.setId("home");
            home.setOnAction(event -> {
                // On retourne à l'écran principal pour sélectionner un niveau
                SwingUtilities.invokeLater(()-> {
                    ihm.returnLevelSelect(false);
                });
            });

            // Tooltips des boutons
            Tooltip tip1 = new Tooltip("Clique sur ce bouton pour exécuter ton code et faire déplacer ton robot"),
                    tip2 = new Tooltip("Repositionne ton robot à la case départ et réinitialise toutes les actions"),
                    tip3 = new Tooltip("Retourner au menu principal");

            Tooltip.install(valider, tip1);     Tooltip.install(restart, tip2);     Tooltip.install(home, tip3);

            // Ajouter les boutons au panel
            btnPanel.getChildren().add(valider);
            btnPanel.getChildren().add(restart);
            btnPanel.getChildren().add(home);

            /** Actions listeners des boutons **/
            // Bouton valider
            valider.setOnAction(event -> {
                // On envoie au contrôleur le code tapé par le joueur
                // Lancé sur un thread parralèle ? A FAIRE
                Gdx.app.postRunnable(() -> ctrl.validerCode(codePanel.getText()));
            });

            // Bouton Reset
            restart.setOnAction(event -> {
                // On réinitialise le contenu du jeu
                // => On utilise Gdx postRunnable pour pouvoir accéder à OpenGL
                // => OpenGL est rattaché à un thread unique, on ne peut pas manipuler du contenu graphique
                // => depuis le thread évenementiel de Swing, par exemple
                Gdx.app.postRunnable(() -> ctrl.reinitJeu());
            });

            // On ajoute le Panel au root panel
            btnPanel.setMinHeight(100); btnPanel.setLayoutY((int)(RoboGame.W_HEIGHT*0.25)+15);
            rootPane.getChildren().add(btnPanel);

            /** Création du panneau d'informations **/
            levelInfo = new LevelInfo(this);
        });

        /** Positionnement du panel JFX **/
        jfxPanel.setBounds(RoboGame.G_WIDTH, codePaneDims[1]+10, codePaneDims[0], RoboGame.G_HEIGHT - codePaneDims[1]+5);
        add(jfxPanel);

        // On centre la fenêtre sur l'écran
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-RoboGame.W_WIDTH/2, dim.height/2-RoboGame.W_HEIGHT/2);

        /** Création des écrans de chargement */

        // On affiche la fenêtre
        pack();
        setSize(RoboGame.W_WIDTH, RoboGame.W_HEIGHT);
        setResizable(false);
        setVisible(true);
    }

    /* Communication avec le contrôleur graphique
    * ============================================= */

    // Passe le JFrame en mode jeu :
    // affiche l'éditeur de code, les boutons d'action
    public void setGameMode() {
        // On initialise le texte contenu par l'éditeur de texte
        codePanel.setText(defaultText);

        // On réinitialise le contenu de la liste d'items
        Platform.runLater(this::initItemList);

        // On redimensionne le isoCanvas du jeu
        isoCanvas.getCanvas().setBounds(0, 0, RoboGame.G_WIDTH, RoboGame.G_HEIGHT);

        // On affiche les éléments masqués
        codePanel.setVisible(true);
        jfxPanel.setVisible(true);
    }

    // Passe le JFrame en mode jeu :
    // affiche l'éditeur de code, les boutons d'action
    public void setViewMode() {
        // On redimensionne le isoCanvas du jeu
        isoCanvas.getCanvas().setVisible(false);
        isoCanvas.getCanvas().setBounds(0, 0, RoboGame.W_WIDTH, RoboGame.W_HEIGHT);
        isoCanvas.getCanvas().setVisible(true);
    }

    // Création du canvas du jeu
    private void createIsoCanvas(RoboGame ihm) {
        // Définition de la configuration
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.foregroundFPS = 0;

        config.width = RoboGame.W_WIDTH;
        config.height = RoboGame.W_HEIGHT;
        config.resizable = false;
        config.vSyncEnabled = true;

        // On crée une application LWJGL à partir de notre classe RoboGame
        isoCanvas = new LwjglAWTCanvas(ihm, config);
    }

    // Sélectionne la liste des termes erronés passés en paramètre dans le codePane
    public void selectErrors(ArrayList<String> errToken) {
        // Ne fonctionne pour l'instant que pour les erreurs grossières
        String text = codePanel.getText();
        if (text.length() == 0) {
            return;
        }

        /** S'il y a plus d'un token erreur */
        String regExp;
        if (errToken.size() > 1) {
            // On génère une expression régulière à partir des tokens erreur passés en paramètre
            regExp = "(";

            for (int i = 0; i < errToken.size(); i++) {
                // On ne sélectionne que les tokens d'au moins trois caractères de longueur
                if (errToken.get(i).length() > 2) {
                    System.out.println ("token : {"+errToken.get(i)+"} :: length : "+errToken.get(i).length());
                    String token = errToken.get(i);
                    regExp += Pattern.quote(token) + ((i<errToken.size()-1) ? "|" : ")");
                }
            }
        } else {
            // L'expression régulière est simplement le token à rechercher entre parenthèses
            // On quote pour pouvoir faire des recherches sur des caractères spéciaux
            if (errToken.get(0).length() > 2) {
                regExp = "(" + Pattern.quote(errToken.get(0)) + ")";
            } else {
                return; // On ne sélectionne pas les tokens de moins de trois caractères de longueur
            }
        }

        ConsoleLog.put("RegExp : "+regExp);

        /** Configuration du contexte de recherche **/
        SearchContext context = new SearchContext();
        context.setSearchFor(regExp);
        context.setMatchCase(true);
        context.setRegularExpression(true);
        context.setWholeWord(false);

        /** Petit hack : pour éviter de repositionner le caret
         * On fait deux recherches successives avant\arrière **/
        try {
            context.setSearchForward(true);
            SearchEngine.find(codePanel, context).wasFound();
            context.setSearchForward(false);
            SearchEngine.find(codePanel, context).wasFound();
        } catch (Exception ex) {
            // ...
        }
    }

    // Ajout d'un élément à la liste d'infos
    public void addInfoList(IconList.IconType iconType, String text) {
        iconList.addItem(iconType, text);
    }

    // Ajout d'un message d'erreur syntaxique à la liste d'infos
    public void addInfoList(IconList.IconType iconType, String text, String errToken) {
        iconList.addItem(iconType, text, errToken);
    }

    // Réinitialise la liste d'items
    public void initItemList() {
        iconList.clearList();
        iconList.addItem(IconList.IconType.CHECK, "À vos marques, prêt, codez !");
    }

    // Renvoie le panneau d'informations
    public LevelInfo getLevelInfo() {
        return levelInfo;
    }

    // Renvoie une référence au contrôleur graphique
    public RoboGame getRoboGame() { return this.ihm; }
}
