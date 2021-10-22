package com.robokode.game.mvc;

import com.robokode.game.sprites.characters.State;
import com.robokode.game.ui.IconList;

import java.util.ArrayList;

/**
 * Objet envoyé par le contrôleur vers la vue contenant diverses informations
 */
public class Message {

    /* Enuméré pour qualifier le type de message envoyé */
    public static enum TypeMessage {
        /* Messages Controleur => Vue */
        // Ajout d'éléments graphiques
        ADD_ROBOT, // on ajoute un robot aux coordonnées x,y sur la map
        ADD_TOWER, // on ajoute une tourelle en x,y

        // Feedback de jeu
        ADD_LIST_MSG, // on ajoute un message dans la liste de statut de l'IHM
        ADD_LIST_ERROR, // on ajoute une liste d'erreurs syntaxiques à la liste de statut

        // Actions du robot
        ACTIONS, // le robot effectue une action (définie par utils.Action)
        RESET, // on supprime le robot d'id num et on le repositionne en x,y

        // Etat de la partie
    }

    /* Attribut principal : type du message */
    public TypeMessage type;

    /* Constructeur principal : type du message */
    public Message (TypeMessage type) {
        this.type = type;
    }

    /* Attributs secondaires */
    public int x, y, num;
    public ArrayList<Action> actionList;
    public IconList.IconType iconType;
    public ArrayList<String[]> errorList;
    public String text;
    public State orientation;
}
