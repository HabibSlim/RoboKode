package com.robokode.game.ui;

import com.robokode.utils.Ressources;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;


/** Liste d'icônes utile pour signaler des infos au joueur
 *  Utilise JavaFX **/

public class IconList {

    /** Types des icônes **/
    public enum IconType {
        BLOCK ("block", "Le programme que tu as écrit a tenté de déplacer ton robot sur un obstacle, ce qui est impossible.\n" +
                "Peut-être as-tu mal compté les cases ?"),
        INFINITE ("infinite", "Ton programme a l'air de ne pas savoir comment s'arrêter...\n" +
                "Es-tu sûr d'avoir écrit une condition pour laquelle tes boucles s'interrompent ?"),
        FINISH ("finish", "Ton programme s'est terminé et il n'y a pas eu d'erreur, mais tu n'es pas parvenu à ton objectif.\n" +
                "Peu importe si tu es loin où près du but, c'est en expérimentant qu'on finit par réussir !"),
        ERROR ("error", "Il y a une erreur de syntaxe dans ton programme.\n" +
                "Une parenthèse qui manque ou une virgule en trop, ton robot est très strict et ne te laissera pas faire d'erreurs..."),
        QUESTION("qmark", "Une des variables de ton programme n'a pas de valeur valide.\n"+
                "As-tu pensé à vérifier que la direction ne valait pas zéro, ou que tu as bien donné une valeur à ta variable ?"),
        CHECK ("check", "C'est parti !");

        private String fileT, extended;

        IconType(String fileT, String extended) { this.fileT = fileT; this.extended = extended; }

        @Override
        public String toString() {
            return Ressources.getIcon_URL()+fileT+".png";
        }
    }

    /** Classe définissant une paire <icone,texte> **/
    private class ListData {
        public Image icone;
        public String texte;
        public String tooltip;
        public String errToken;

        ListData(IconType type, String texte) { this.icone = new Image(type+""); this.texte = texte; this.tooltip = type.extended; }
        ListData(IconType type, String texte, String errToken) { this(type, texte); this.errToken = errToken; }
    }

    private ObservableList<ListData> elements =  FXCollections.observableArrayList();

    public IconList(Pane jfxPane, int wWidth, int wHeight) {

        /** Création du Pane et de la liste des icônes **/
        Pane listPane = new Pane();
        ListView<ListData> listObj = new ListView<>(elements);
        listObj.setId("listView");

        listObj.setMinWidth(wWidth);         listObj.setMaxWidth(wWidth);
        listObj.setMinHeight(wHeight);       listObj.setMaxHeight(wHeight);
        listObj.setCellFactory(param -> new IconCell());

        /** Création du bouton Clear **/
        Button clear = new Button("");
        clear.setId("clear");

        // Positionnement
        clear.setLayoutX(wWidth - 65);
        clear.setLayoutY(wHeight - 65);

        // Action reliée au bouton
        clear.setOnAction(event -> clearList());

        // Tooltip du bouton
        Tooltip tip = new Tooltip("Vider la liste d'infos");
        Tooltip.install(clear, tip);

        /** Ajout des éléments créés **/
        listPane.getChildren().add(listObj);    listPane.getChildren().add(clear);

        jfxPane.getChildren().add(listPane);
    }

    /** Méthodes permettant de modifier la liste **/
    public void addItem(IconType iconType, String texte) {
        elements.add(0, new ListData(iconType, texte)); // On ajoute les éléments en haut de la liste
    }
    public void addItem(IconType iconType, String texte, String errToken) {
        elements.add(0, new ListData(iconType, texte, errToken)); // On ajoute les éléments en haut de la liste
    }

    public void clearList() {
        elements.clear();
    }

    /** Redéfinition d'une cellule de la liste **/
    private class IconCell extends ListCell<ListData> {
        private ImageView imageView = new ImageView();
        private Tooltip tT = new Tooltip();
        private FlowPane pane = new FlowPane();

        @Override
        protected void updateItem(ListData item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                imageView.setImage(null);
                pane.getChildren().clear();     getChildren().clear();
                setGraphic(pane);
            } else {
                if (!getChildren().contains(pane))  getChildren().add(pane);

                // On installe le tooltip
                tT.setText(item.tooltip);
                Tooltip.install(this, tT);

                // On définit l'image
                imageView.setImage(item.icone);

                // On construit la chaîne de caractères
                ArrayList<Text> textBlock = new ArrayList<>();
                String spacer = "  ";
                // Si on doit mettre en forme des tokens, on les fait apparaître
                if (item.errToken != null) {
                    textBlock.add(formatText(spacer+item.texte + " : ", false));
                    textBlock.add(formatText(item.errToken, true));
                } else {
                    textBlock.add(formatText(spacer+item.texte, false));
                }

                pane.getChildren().clear();
                pane.getChildren().add(imageView);      pane.getChildren().addAll(textBlock);
                setGraphic(pane);
            }
        }
    }

    // Formate une chaîne de caractères en un texte JavaFx
    private Text formatText(String word, boolean error) {
        Text txt = new Text(word);
        if (error) {
            txt.setFill(Paint.valueOf("red"));
            txt.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        }
        // txt.setStyle()

        return txt;
    }
}