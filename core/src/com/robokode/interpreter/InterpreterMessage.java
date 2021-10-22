package com.robokode.interpreter;

import com.robokode.utils.Direction;

import java.util.ArrayList;

/**
 * Liste les messages envoyés de l'interpréteur au contrôleur
 * Permet de normaliser les interactions I<=>C, et de gérer les pauses\déplacements\etc.
 */
public class InterpreterMessage {
    /* Enuméré pour qualifier le type de message envoyé */
    public enum TypeMessage {
        /** Actions spéciales **/
        END_PRG, // on a atteint la fin du script

        /** Actions normales **/
        DEPLACEMENT, // une instruction déplacement est exécutée
        TIRER, // une instruction de tir est exécutée
        MELEE, // une instruction d'attaque en melée est exécutée
        DETECTERENNEMI, // une instruction de détection d'ennemi est exécutée
        ESTVIVANT, // une instruction donnant le statut vivant de l'ennemi du joueur
        RECHARGER, // le joueur recharge ses munitions

        /** Erreurs à l'exécution **/
        SYNTAX_ERR, // des erreurs de syntaxe ont été détectées
        LOOP_LIMIT, // on a dépassé le nombre maximal d'instructions exécutable
    }

    /* Attribut principal : type du message */
    public final TypeMessage type;

    /* Constructeur principal : type du message */
    public InterpreterMessage (TypeMessage type) {
        this.type = type;
    }

    /* Attributs secondaires */
    public ArrayList<String[]> errorList;
    public String text;
    public Direction dirInst;
}
