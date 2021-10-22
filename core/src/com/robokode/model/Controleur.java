package com.robokode.model;

import com.robokode.game.mvc.Action;
import com.robokode.game.mvc.Message;
import com.robokode.game.mvc.Observateur;
import com.robokode.game.sprites.characters.State;
import com.robokode.interpreter.Interpreter;
import com.robokode.interpreter.InterpreterMessage;
import com.robokode.utils.BulleType;
import com.robokode.utils.ConsoleLog;
import com.robokode.utils.Direction;
import com.robokode.utils.maps.MapInfo;
import com.robokode.utils.maps.MapLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.robokode.game.mvc.Message.TypeMessage;
import static com.robokode.game.ui.IconList.IconType;

/**
 * Contrôleur : logique du jeu
 */
public class Controleur {

    private Observateur ihm;

    /**
     * Elements du modèle
     **/
    private HashMap<Cible, Robot> robots;

    private enum Cible {
        ROBOT_JOUEUR, ROBOT_ENNEMI, TOURELLE
    }

    private Grille grille;

    // Etat du contrôleur : mode de jeu et mapI courante
    private GameMode gameMode;
    private MapInfo mapI;

    public enum GameMode {
        TUTORIAL, MULTI
    }

    public Controleur() {
        // On crée la liste des robots
        robots = new HashMap<>();

        // On crée la grille
        grille = new Grille();
    }

    /* Interaction IHM <=> Contrôleur
     * =================================== */

    // Lance la partie sur la map mapIndex
    public void lancerPartie(GameMode mode, int mapID) {
        ConsoleLog.put("Contrôleur : Lancement de la partie");

        /** Modification de l'état du contrôleur **/
        gameMode = mode;

        /** On lit les informations de la carte **/
        mapI = MapLoader.Instance().getInfo(mapID);

        // => On place les obstacles sur la grille
        getGrille().setObstacles(mapI.obstacles);

        // => On place les barrières sur la grille
        getGrille().setBarrieres(mapI.barrieres);

        // => On initialise le jeu
        initGame(mode);
    }

    // Validation du code du joueur
    public void validerCode(String codeJ) {
        // Quand le joueur valide son code, il réinitialise automatiquement la position de son robot
        reinitJeu();

        // Si on est en mode tutoriel
        if (gameMode == GameMode.TUTORIAL) {

            // On instancie l'interpréteur
            Interpreter rKode = new Interpreter();

            // On interprète le script ouvert
            InterpreterMessage msg = rKode.interpret(codeJ);

            // Selon le message reçu par l'interpréteur, on fait des trucs différents
            // => s'il y a une erreur de syntaxe, on l'affiche et on arrête là
            if (msg.type == InterpreterMessage.TypeMessage.SYNTAX_ERR) {
                ConsoleLog.put("Interpréteur : Il y a des erreurs");
                // On envoie toutes les erreurs reçues de l 'interpréteur à la vue
                Message viewM = new Message(TypeMessage.ADD_LIST_ERROR);
                viewM.errorList = msg.errorList;
                ihm.traiterMessage(viewM);
            }
            // Sinon on exécute le script du joueur
            else {
                // Signale l'interruption de l'exécution
                boolean fini = false;
                // Signale l'exécution d'une action (déplacement\attaque)
                boolean acted = false;

                Message tempM;
                ArrayList<Action> actions = new ArrayList<>();
                ArrayList<Message> msgList = new ArrayList<>();

                while (!fini) {
                    // Si on est en mode TUTO_MULTI, on effectue une action pour le robot
                    if (mapI.typeMap == MapInfo.MapType.TUTO_MULTI && acted) {
                        Action action = actSecondRobot();
                        if (action != null) actions.add(action);
                        acted = false;
                    }

                    switch (msg.type) {
                        case LOOP_LIMIT:
                            ConsoleLog.put("Interpréteur : Nombre maximal d'instructions dépassé !");
                            fini = true;
                            // On vide la liste des actions et messages précédemment ajoutées : ils sont invalides
                            actions.clear();
                            msgList.clear();

                            // On envoie un message à la vue pour signaler que le code fait une boucle infinie
                            tempM = new Message(TypeMessage.ADD_LIST_MSG);
                            tempM.iconType = IconType.INFINITE;
                            tempM.text = "Ton programme tourne en rond...";
                            msgList.add(tempM);

                            // On affiche une bulle au dessus du robot
                            actions.add(new Action(BulleType.INFINITE, 0));
                        break;
                        case END_PRG:
                            ConsoleLog.put("Interpréteur : Programme du joueur terminé !");
                            fini = true;

                            // On envoie un message à la vue pour signaler que le programme est terminé
                            tempM = new Message(TypeMessage.ADD_LIST_MSG);
                            tempM.iconType = IconType.FINISH;
                            tempM.text = "Ton robot a exécuté toutes tes instructions !";
                            msgList.add(tempM);

                            // On affiche une bulle au dessus du robot
                            actions.add(new Action(BulleType.FINISH, 0));
                        break;
                        case DETECTERENNEMI:
                            ConsoleLog.put("Interpréteur : Le joueur se demande la position de son adversaire.");

                            // On renvoie la direction de l'adversaire sur le modèle
                            Direction dir = determinerSens(getRobotJoueur());

                            // Direction :
                            ConsoleLog.put("Contrôleur : Direction de l'ennemi : "+dir);

                            // Si une direction a été trouvée, on assigne une valeur à la variable
                            rKode.setDirVar(dir);

                        break;
                        case ESTVIVANT:
                            ConsoleLog.put("Interpréteur : Le joueur se demande si son adversaire est vivant.");

                            Robot opponent = (getRobotSecond() != null ? getRobotSecond() : getTourelle());

                            // On vérifie si l'adversaire est vivant ou non
                            if (opponent != null) {
                                rKode.setBoolVar(opponent.vivant() ? 1 : 0);
                            } else {
                                // On ne peut utiliser cette instruction que s'il existe un ennemi : on affiche un message d'erreur
                                tempM = new Message(TypeMessage.ADD_LIST_MSG);
                                tempM.iconType = IconType.QUESTION;
                                tempM.text = "Aucun ennemi pour l'instant !";
                                msgList.add(tempM);

                                // On affiche une bulle au dessus du robot
                                actions.add(new Action(BulleType.QUESTION, 0));
                            }

                        break;
                        case DEPLACEMENT:
                            ConsoleLog.put("Interpréteur : Le joueur se déplace en direction : " + msg.dirInst);

                            // Si la direction est définie : on se déplace
                            if (msg.dirInst != null) {
                                // On déplace le joueur dans la direction voulue : sur le modèle
                                if (deplacer(getRobotJoueur(), msg.dirInst)) {
                                    // Si le déplacement est possible, on l'ajoute à la liste d'actions
                                    actions.add(new Action(msg.dirInst, 0));
                                } else {
                                    // Sinon on change l'orientation du personnage, et on affiche la bulle "impossible"
                                    actions.add(new Action(BulleType.NOWALK, 0));
                                    ConsoleLog.put("Contrôleur : Impossible de se déplacer là !");

                                    // On envoie un message à la vue pour signaler que le joueur ne peut se déplacer par là
                                    tempM = new Message(TypeMessage.ADD_LIST_MSG);
                                    tempM.iconType = IconType.BLOCK;
                                    tempM.text = "Ton robot est bloqué par un obstacle !";
                                    msgList.add(tempM);
                                }
                            } else {
                                // On ajoute un message pour signaler au joueur que le paramètre direction qu'il utilise
                                // n'est pas valué
                                tempM = new Message(TypeMessage.ADD_LIST_MSG);
                                tempM.iconType = IconType.QUESTION;
                                tempM.text = "Direction de déplacement invalide !";
                                msgList.add(tempM);

                                // On affiche une bulle au dessus du robot
                                actions.add(new Action(BulleType.QUESTION, 0));
                            }
                            acted = true;
                        break;
                        case TIRER:
                            ConsoleLog.put("Interpréteur : Le joueur attaque en direction : " + msg.dirInst);

                            Action action, linkedAction = null;

                            // On vérifie si la direction est définie
                            if (msg.dirInst != null) {
                                // Le joueur attaque dans la direction voulue
                                Robot target = attaqueDist(getRobotJoueur(), msg.dirInst);
                                if (target != null) {
                                    // => Si quelque chose est touché : il faut une animation "dégâts subis"

                                    // Si c'est une tourelle :
                                    if (target.getNom().equals("Tourelle")) {
                                        // On ajoute une action liée pour signaler que la tourelle a subi des dégâts
                                        linkedAction = new Action(0, target.getPv());
                                    }
                                    // Si c'est un robot
                                    else {
                                        // On ajoute une action liée pour signaler que le robot a subi des dégâts
                                        linkedAction = new Action(Action.ActionType.SET_HP_ROBOT);
                                        linkedAction.indexJ = 1;  linkedAction.HP = target.getPv();
                                    }
                                }

                                // On envoie en paramètres les coordonnées du premier obstacle dans cette direction
                                int[] coord = getGrille().getFirstObstacle(getRobotJoueur().getPositionAct(), msg.dirInst, true);
                                ConsoleLog.put("Contrôleur : Le projectile touche : (" + coord[0] + ";" + coord[1] + ").");

                                // Dans tous les cas, on envoie un message pour faire tirer le joueur
                                action = new Action(msg.dirInst, 0, coord);
                                action.linkedAction = linkedAction;
                                actions.add(action);
                            } else {
                                // On ajoute un message pour signaler au joueur que le paramètre direction qu'il utilise
                                // n'est pas valué
                                tempM = new Message(TypeMessage.ADD_LIST_MSG);
                                tempM.iconType = IconType.QUESTION;
                                tempM.text = "Direction d'attaque invalide !";
                                msgList.add(tempM);

                                // On affiche une bulle au dessus du robot
                                actions.add(new Action(BulleType.QUESTION, 0));
                            }
                            acted = true;
                        break;
                        case RECHARGER:
                            // Le joueur recharge ses munitions
                            acted = true;

                            // On marque un temps d'attente
                            actions.add(new Action(Action.ActionType.WAIT));
                        break;
                        default:
                            ConsoleLog.put("Contrôleur : Impossible de gérer le message : " + msg.type, true);
                    }

                    // On vérifie si le joueur a fini le niveau tutoriel après cette dernière action
                    if (niveauFini()) {
                        fini = true;

                        // On ajoute une action liée "jeu terminé" au joueur => 3 étoiles : 3
                        // On désactive les boutons "Valider" et "Recommencer" ? A VOIR
                        Action action = new Action(3);
                        actions.add(action);

                        ConsoleLog.put("Contrôleur : Jeu terminé. C'est gagné !");
                    }

                    // Si le programme du joueur n'est pas terminé, on continue l'exécution du programme
                    if (!fini) msg = rKode.restart();
                }

                // Si on a ajouté des actions, on envoie un message à l'IHM pour qu'elles soient effectués
                if (actions.size() > 0) {
                    Message msgV = new Message(TypeMessage.ACTIONS);
                    msgV.actionList = actions;
                    ihm.traiterMessage(msgV);
                }

                // Si on a envoyé d'autres messages à l'IHM, on les envoie tous
                if (msgList.size() > 0) {
                    ihm.traiterMessage(msgList);
                }
            }
        }
    }

    // Réinitialisation de la grille, et de la position du robot (modele et vue)
    public void reinitJeu() {
        // On supprime tous les sprites de l'IHM : dans tous les cas
        Message msg = new Message(TypeMessage.RESET);
        ihm.traiterMessage(msg);

        // On réinitialise les éléments du modèle
        grille.resetGrille();

        initGame(gameMode);
    }

    // Initialise le jeu => dépend du mode de jeu courant
    private void initGame(GameMode gameMode) {
        /** Initialisation de la vue **/
        switch (gameMode) {
            case TUTORIAL:

                /** On ajoute un robot à la grille en position de départ **/
                // => S'il n'existe pas déjà !
                if (getRobotJoueur() == null) {
                    ajouterRobot("Robot1", mapI.startPos[0], mapI.startPos[1]);

                } else {
                    // Inutile de réinitialise ses PVs en mode tutoriel
                    // A changer dans certains cas limites
                    getRobotJoueur().setPosition(grille.getCase(mapI.startPos[0], mapI.startPos[1]));
                }

                /** On ajoute un robot à la vue **/
                // On crée un message de création d'un robot[0]
                Message msg = new Message(TypeMessage.ADD_ROBOT);
                msg.x = mapI.startPos[0];
                msg.y = mapI.startPos[1];
                msg.num = 0;

                // Si on est en mode tuto multi, on oriente le sprite
                if (mapI.typeMap == MapInfo.MapType.TUTO_MULTI) msg.orientation = State.STATIC_BG;

                // On envoie le message à l'IHM
                ihm.traiterMessage(msg);

                /** Actions spécifiques à chaque type de tutoriel **/
                switch (mapI.typeMap) {
                    case TUTO_SHOOT:
                        // On crée une tourelle aux coordonnées demandées
                        // => Dans la vue
                        msg = new Message(TypeMessage.ADD_TOWER);
                        msg.x = mapI.towerPos[0];
                        msg.y = mapI.towerPos[1];

                        // => Dans le modèle : tourelle
                        if (getTourelle() == null) {
                            // On crée la tourelle si elle n'existait pas déjà
                            ajouterTourelle(mapI.towerPos[0], mapI.towerPos[1]);
                        } else {
                            // On repositionne ses PVs au maximum
                            getTourelle().setPv(Robot.PVMAX_T);

                            // On place la tourelle dans la grille
                            getTourelle().setPosition(grille.getCase(mapI.towerPos[0], mapI.towerPos[1]));
                        }

                        // On envoie le message à l'IHM
                        ihm.traiterMessage(msg);
                    break;
                    case TUTO_MULTI:
                        // On crée un robot ennemi
                        // => Dans la vue
                        msg = new Message(TypeMessage.ADD_ROBOT);
                        msg.x = mapI.ennemyPos[0];
                        msg.y = mapI.ennemyPos[1];
                        msg.num = 1;

                        // On envoie le message à l'IHM
                        ihm.traiterMessage(msg);

                        // => Dans le modèle
                        // => S'il n'existe pas déjà !
                        if (getRobotSecond() == null) {
                            ajouterRobotEnnemi(msg.x, msg.y);
                        } else {
                            getRobotSecond().setPosition(getGrille().getCase(mapI.ennemyPos[0],  mapI.ennemyPos[1]));

                            // On repositionne ses PVs au maximum
                            getRobotSecond().setPv(Robot.PVMAX_T);
                        }
                    break;
                }
                break;
            default:
                // => A FAIRE
        }

    }

    /* Getters
     * =================================== */

    public Grille getGrille() {
        return grille;
    }

    // On reset totalement le contenu du modèle
    public void hardResetModele() {
        this.robots.clear();
        this.grille = new Grille();
    }

    private HashMap<Cible, Robot> getRobots() {
        return robots;
    }

    // Renvoie le robot du joueur principal
    private Robot getRobotJoueur() {
        if (getRobots().size() > 0)
            return getRobots().get(Cible.ROBOT_JOUEUR);
        else
            return null;
    }

    // Renvoie le deuxième robot (IA ou joueur en multi)
    public Robot getRobotSecond() {
        if (getRobots().size() > 1)
            return getRobots().get(Cible.ROBOT_ENNEMI);
        else
            return null;
    }

    // Renvoie une tourelle placée sur la map
    public Robot getTourelle() {
        if (getRobots().size() > 1)
            return getRobots().get(Cible.TOURELLE);
        else
            return null;
    }

    /* Setters
     * =================================== */

    public void setObservateur(Observateur ihm) {
        this.ihm = ihm;
    }

    /* Méthodes d'action sur les robots
     * =================================== */

    // Ajoute un robot aux coordonnées entrées de la grille en paramètre
    public Robot ajouterRobot(String nom, int ligne, int colonne) {
        // On crée un robot avec en position actuelle la tuile de coordonnées en paramètre
        Robot roboCop = new Robot(nom, grille.getCase(ligne, colonne));
        robots.put(Cible.ROBOT_JOUEUR, roboCop); // => toujours un robot joueur pour l'instant

        return roboCop;
    }

    // Ajoute un robot ennemi à la partie
    public Robot ajouterRobotEnnemi(int ligne, int colonne) {
        // On crée un robot ennemi aux coordonnées passées en paramètres
        Robot roboCop = new Robot("Ennemi", grille.getCase(ligne, colonne));
        robots.put(Cible.ROBOT_ENNEMI, roboCop);

        return roboCop;
    }

    // Ajoute une tourelle à la partie
    public Robot ajouterTourelle(int ligne, int colonne) {
        Robot newR = new Robot("Tourelle", grille.getCase(ligne, colonne));
        newR.setPv(Robot.PVMAX_T);

        // On ajoute la tourelle à la liste des robots
        robots.put(Cible.TOURELLE, newR);

        return newR;
    }

    // Simule une action pour le deuxième robot en mode tutoriel
    public Action actSecondRobot() {
        Action act;
        Direction dir;

        // Si le robot n'a plus de points de vie, il n'est pas déplacé
        if (!getRobotSecond().vivant()) return null;

        // Tant que le robot n'atteint pas la colonne 1, on le déplace vers le bas
        if (getRobotSecond().getColonne() == 5) {
            dir = Direction.BG;
        } else if (getRobotSecond().getColonne() == 1) {
            dir = Direction.HD;
        } else {
            dir = lastDir;
        }

        // On déplace le robot dans le modèle
        if (dir == Direction.BG)
            getRobotSecond().setPosition(getGrille().getCase(getRobotSecond().getLigne(), getRobotSecond().getColonne()-1));
        else
            getRobotSecond().setPosition(getGrille().getCase(getRobotSecond().getLigne(), getRobotSecond().getColonne()+1));

        act = new Action(dir, 1);

        lastDir = dir;

        return act;
    }

    // Déplace le robot en paramètre vers la direction en paramètres
    // Renvoie true si le déplacement est possible, false sinon
    public boolean deplacer(Robot robot, Direction dir) {
        if (dir == Direction.HG && robot.getLigne() != 0 && grille.getCase(robot.getLigne() - 1, robot.getColonne()).aucunObstacle() &&
                !grille.verifBarrieresTuile(robot.getPositionAct(), grille.getCase(robot.getLigne() - 1, robot.getColonne()))) { //on vérifie qu'un déplacement est possible en haut à gauche
            robot.setPosition(grille.getCase(robot.getLigne() - 1, robot.getColonne())); //on met à jour la position du robot
        } else if (dir == Direction.BG && robot.getColonne() != 0 && grille.getCase(robot.getLigne(), robot.getColonne() - 1).aucunObstacle() &&
                !grille.verifBarrieresTuile(robot.getPositionAct(), grille.getCase(robot.getLigne(), robot.getColonne() - 1))) { //on vérifie qu'un déplacement est possible en bas à gauche
            robot.setPosition(grille.getCase(robot.getLigne(), robot.getColonne() - 1)); //on met à jour la position du robot
        } else if (dir == Direction.BD && robot.getLigne() != 9 && grille.getCase(robot.getLigne() + 1, robot.getColonne()).aucunObstacle() &&
                !grille.verifBarrieresTuile(robot.getPositionAct(), grille.getCase(robot.getLigne() + 1, robot.getColonne()))) { //on vérifie qu'un déplacement est possible en bas à droite
            robot.setPosition(grille.getCase(robot.getLigne() + 1, robot.getColonne())); //on met à jour la position du robot
        } else if (dir == Direction.HD && robot.getColonne() != 9 && grille.getCase(robot.getLigne(), robot.getColonne() + 1).aucunObstacle() &&
                !grille.verifBarrieresTuile(robot.getPositionAct(), grille.getCase(robot.getLigne(), robot.getColonne() + 1))) { //on vérifie qu'un déplacement est possible en haut à droite
            robot.setPosition(grille.getCase(robot.getLigne(), robot.getColonne() + 1)); //on met à jour la position du robot
        } else //si on ne reçoit pas de direction, qu'un obstacle bloque la route ou qu'on se déplace vers l'extérieur de la map
            return false;

        // On renvoie vrai dans tous les autres cas
        return true;
    }

    /**
     * Méthodes d'attaque
     **/

    // Fait attaquer à distance le robot en paramètre dans la direction en paramètre
    public Robot attaqueDist(Robot robot, Direction dir) {
        // S'il y a un deuxième robot, ou une tourelle
        if (getRobots().size() > 1) {
            // On vérifie si un robot est atteignable dans cette direction
            Robot target = getGrille().getFirstRobot(robot.getPositionAct(), dir);
            if (target != null) {
                target.takeDamage(Robot.DEGATSDIST);
                return target;  // On renvoie le robot touché
            }
        }

        return null;
    }

    /* Lignes de vue et directions
     * =================================== */

    // Renvoie la direction de l'ennemi de robot1, s'ils sont sur une même ligne de vue
    // Et null sinon
    public Direction determinerSens(Robot robot1) {
        // On teste la présence d'un robot en ligne de vue dans chacune des directions
        for (Direction dir : Direction.values()) {
            // Si la case contient un ennemi, on renvoie sa direction
            if (!getGrille().getFirstObstacleT(robot1.getPositionAct(), dir, false).estLibre()) {
                return dir;
            }
        }

        return null;
    }

    /** Méthodes générales **/

    /**
     * Méthodes pour le mode TUTORIEL
     **/
    // Renvoie vrai si le niveau tutoriel est achevé
    public boolean niveauFini() {
        if (gameMode == GameMode.TUTORIAL) {
            // Les objectifs varient selon le type de tutoriel
            switch (mapI.typeMap) {
                case TUTO_MOVE:
                    // => Si le but est d'arriver à une case, on vérifie si la position est atteinte
                    return Arrays.equals(getRobotJoueur().getPositionInt(), mapI.endPos);
                case TUTO_SHOOT:
                    // => On vérifie si la tourelle n'a plus de points de vie
                    return !getTourelle().vivant();
                case TUTO_MULTI:
                    // => On vérifie si la tourelle ennemie est en vie
                    return !getRobotSecond().vivant();
                default:
                    ConsoleLog.put("Contrôleur : Impossible de traiter ce type de map.", true);
                    break;
            }
        }

        return false;
    }

    /** Méthodes pour le mode MULTIJOUEUR **/
    // A FAIRE

    // Attribut temporaire pour le statut du robot en mode tutoriel
    private Direction lastDir;
}