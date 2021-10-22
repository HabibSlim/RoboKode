package com.robokode.game.mvc;

import com.robokode.utils.BulleType;
import com.robokode.utils.Direction;

/**
 * Objet définissant une action (déplacement, attaque au corps à corps, bulle d'info, tir, etc.)
 */
public class Action {

    public enum ActionType {
        MOVE,            // déplacement classique dans une direction
        SHOOT,           // le joueur tire dans une direction donnée, vers une destination (x,y) => coord[]
        BULLE,           // affichage d'une bulle d'erreur
        SET_HP_TOUR,     // définit le montant de la vie d'une tour
        SET_HP_ROBOT,    // définit le montant de la vie d'un robot
        GAGNE_TUTO,      // partie gagnée, le joueur a atteint l'objectif du niveau tutoriel
                         // champ nbEtoiles initialisé
        WAIT             // pause pendant une unité de temps
    }

    /** Données de l'action **/

    public Action linkedAction; // action à déclencher à la fin de celle de l'instance
    public ActionType type;
    public Direction dir;
    public BulleType bulle;
    public int coord[];
    public int HP, nbEtoiles;

    public int indexJ; // Index du joueur à qui on applique l'action
    public int indexT; // Index de la tourelle

    /** Constructeurs **/

    public Action(ActionType type) { this.type = type; }
    public Action(Direction dir, int indexJ) { this.type = ActionType.MOVE; this.dir = dir; this.indexJ = indexJ;}
    public Action(BulleType bulle, int indexJ) { this.type = ActionType.BULLE; this.bulle = bulle; this.indexJ = indexJ;}
    public Action(Direction dir, int indexJ, int[] coord) {
        this.type = ActionType.SHOOT; this.dir = dir; this.indexJ = indexJ; this.coord = coord;
    }
    public Action(int indexT, int HP) {  this.type = ActionType.SET_HP_TOUR; this.HP = HP;  }
    public Action(int nbEtoiles) {  this.type = ActionType.GAGNE_TUTO; this.nbEtoiles = nbEtoiles;  }

}
