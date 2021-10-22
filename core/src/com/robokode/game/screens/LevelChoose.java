package com.robokode.game.screens;

/**
 * Ecran de sélection du niveau
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.robokode.game.RoboGame;
import com.robokode.game.screens.elements.LevelBouton;
import com.robokode.game.screens.elements.LevelPreview;
import com.robokode.game.screens.elements.StageBouton;
import com.robokode.utils.Ressources;

import static com.robokode.game.RoboGame.W_HEIGHT;
import static com.robokode.game.RoboGame.W_WIDTH;

public class LevelChoose implements Screen {

    /**   Dimensions\positions des éléments   **/

    private static final int CIRCLE_DIAMETER = 425;
    private static final int CIRCLE_X = (RoboGame.W_WIDTH- CIRCLE_DIAMETER)/2;
    private static final int CIRCLE_Y = (RoboGame.W_HEIGHT- CIRCLE_DIAMETER)/2 + 20;
    // Dimensions des boutons de la barre
    private static final int NAV_PADDING = 11, NAV_DIAM = 20;
    // Propriétés des boutons de navigation droite\gauche
    private static final int LR_DIAMETER = 65, LR_PADDING = 20;

    // Barre de sélection des niveaux
    LevelBouton[] levelBoutons;

    // Boutons d'action
    StageBouton tutoriel, multiJoueur, menuPrincipal, leftSlide, rightSlide;

    // Autres éléments graphiques
    LevelPreview levelPreview;
    Texture bgImage;

    // Niveau sélectionné
    public int currentIndex = 0;

    // Attributs privés de classe
    private final RoboGame game;

    // Stage Scene2D
    private Stage stage;

    public LevelChoose (final RoboGame game) {
        this.game = game;

        // On crée le stage Scene2D
        stage = new Stage();  Gdx.input.setInputProcessor(stage);

        // On charge l'image de fond
        bgImage = Ressources.getImgRsc(0);

        // Création de l'aperçu de niveau
        levelPreview = new LevelPreview(CIRCLE_X, CIRCLE_Y);

        // Création des boutons d'action
        tutoriel = new StageBouton(3, 54, W_HEIGHT - 63, 0, -5);
        multiJoueur = new StageBouton(4, 280, W_HEIGHT - 63, 0, -5);
        menuPrincipal = new StageBouton(5, W_WIDTH-260, W_HEIGHT - 63, 0, -5);

        // Boutons de navigation droite\gauche
        leftSlide = new StageBouton(6, CIRCLE_X-LR_DIAMETER-LR_PADDING,
                CIRCLE_Y+(CIRCLE_DIAMETER-LR_DIAMETER)/2, 0, 0);
        rightSlide = new StageBouton(7, CIRCLE_X+CIRCLE_DIAMETER+LR_PADDING,
                CIRCLE_Y+(CIRCLE_DIAMETER-LR_DIAMETER)/2, 0, 0);

        // Ajout des éléments au stage
        stage.addActor(tutoriel);       stage.addActor(multiJoueur);    stage.addActor(rightSlide);
        stage.addActor(menuPrincipal);  stage.addActor(leftSlide);      stage.addActor(levelPreview);

        // Création de la barre des niveaux
        // Calcul de la coordonnée X de départ des boutons :
        float xBar = (W_WIDTH - (Ressources.NB_LEVELS*(NAV_DIAM + NAV_PADDING) - NAV_PADDING))/2;

        // On crée chaque bouton un par un, et on les ajoute au stage
        levelBoutons = new LevelBouton[Ressources.NB_LEVELS];
        for (int i=0; i<Ressources.NB_LEVELS; i++) {
            levelBoutons[i] = new LevelBouton(xBar + i*(NAV_DIAM + NAV_PADDING),
                    CIRCLE_Y - 50, i);
            stage.addActor(levelBoutons[i]);
        }

        // On sélectionne le premier bouton par défaut
        levelBoutons[0].setActive(true);

        /**   Définition des actions des autres boutons  **/
        // On définit une action pour chacun des boutons
        final LevelChoose levelChoose = this;

        // Bouton menu principal
        menuPrincipal.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // On crée une action pour basculer à l'autre fenêtre
                levelChoose.dispose();
                RunnableAction run = new RunnableAction();
                run.setRunnable(() -> {
                    game.setScreenState(RoboGame.EtatJeu.MAIN_MENU);
                });

                // On ajoute l'action précédée par un effet de fondu
                stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), run));

                return true;
            }
        });

        // Action reliée à l'aperçu
        levelPreview.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                levelChoose.dispose();

                // On définit le niveau choisi
                game.setMapIndex(levelChoose.getCurrentIndex());

                // On crée une action pour basculer au mode de jeu
                RunnableAction run = new RunnableAction();
                run.setRunnable(() -> {
                    // On passe au jeu
                    game.setScreenState(RoboGame.EtatJeu.MAIN_GAME);
                });

                // On ajoute l'action précédée par un effet de fondu
                stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), run));

                return true;
            }
        });

        // Actions icônes en bas de l'écran
        for (int i=0; i<Ressources.NB_LEVELS; i++) {
            final int i_i = i;
            levelBoutons[i].addListener(new InputListener() {
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    // On passe au niveau i_i
                    levelChoose.setLevel(i_i);
                    return true;
                }
            });
        }

        // Actions boutons gauche\droite
        leftSlide.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // On passe au niveau à gauche
                levelChoose.setLevel(currentIndex-1);
                return true;
            }
        });
        rightSlide.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // On passe au niveau à gauche
                levelChoose.setLevel(currentIndex+1);
                return true;
            }
        });
    }

    // Méthode de passage d'un niveau à l'autre
    public void setLevel(int index) {
        // Si l'index est valide
        if ((index < Ressources.NB_LEVELS) && (index >= 0)) {
            // On passe l'ancien bouton à faux, et le nouveau à vrai
            levelBoutons[currentIndex].setActive(false);
            levelBoutons[index].setActive(true);
            currentIndex = index;

            // On modifie l'image active => A FAIRE
            levelPreview.setIndex(currentIndex);
        }
    }

    // Pré-sélection d'un niveau sans effet de transition
    public void directSetLevel(int index) {
        // Si l'index est valide
        if ((index < Ressources.NB_LEVELS) && (index >= 0)) {
            setLevel(index);
            levelPreview.started = false;
        }
    }

    @Override
    public void show () {
        /** On définit cet écran comme capteur de l'input utilisateur **/
        Gdx.input.setInputProcessor(stage);
        /** On ajoute un effet fadeIn **/
        stage.addAction(Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.5f)));
    }

    @Override
    public void render (float delta) {
        // A FAIRE => CHANGER
        // Gdx.gl.glClearColor(0.15f, 0.15f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // On dessine l'image de fond
        stage.getBatch().begin();
        stage.getBatch().draw(bgImage, 0, 0, W_WIDTH, W_HEIGHT);
        stage.getBatch().end();

        // On dessine le Stage
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public int getCurrentIndex() { return this.currentIndex; }

    @Override
    public void resize (int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
    }
}
