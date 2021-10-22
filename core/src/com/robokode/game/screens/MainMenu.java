package com.robokode.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.robokode.game.RoboGame;
import com.robokode.game.screens.elements.StageBouton;
import com.robokode.game.screens.elements.WheelSprite;
import com.robokode.utils.Ressources;

import static com.robokode.game.RoboGame.W_HEIGHT;
import static com.robokode.game.RoboGame.W_WIDTH;

public class MainMenu implements Screen {

    // Dimensions des éléments du menu
    private static final int PLAY_BUTTON_WIDTH = 194;
    private static final int PLAY_BUTTON_HEIGHT = 55;
    private static final int PARAM_BUTTON_WIDTH = 386;
    private static final int PARAM_BUTTON_HEIGHT = 56;
    private static final int QUIT_BUTTON_WIDTH = 245;
    private static final int QUIT_BUTTON_HEIGHT = 56;
    private static final int LOGO_WIDTH = 530;
    private static final int LOGO_HEIGHT = 93;

    // Marge Y entre les boutons du menu
    private static final int MARGIN_Y = 40;

    // Boutons d'action
    StageBouton playButton, paramButton, quitButton;

    // Fond d'écran et logo
    Texture bgImage;

    // Engrenages
    WheelSprite bigWheel, smallWheel;

    // Attributs privés de classe
    private final RoboGame game;

    // Stage Scene2D
    private Stage stage;

    public MainMenu (final RoboGame game) {
        this.game = game;

        // On crée le stage Scene2D
        stage = new Stage();

        // On crée les boutons du menu
        playButton = new StageBouton(0,
                (RoboGame.W_WIDTH - PLAY_BUTTON_WIDTH)/2, RoboGame.W_HEIGHT/2,
                -90, -10);
        playButton.setTouchable(Touchable.enabled);

        paramButton = new StageBouton(1,
                (RoboGame.W_WIDTH - PARAM_BUTTON_WIDTH)/2, RoboGame.W_HEIGHT/2 - PLAY_BUTTON_HEIGHT - MARGIN_Y,
                -90, -10);
        paramButton.setTouchable(Touchable.enabled);

        quitButton = new StageBouton(2,
                (RoboGame.W_WIDTH - QUIT_BUTTON_WIDTH)/2, RoboGame.W_HEIGHT/2 - PARAM_BUTTON_HEIGHT - PLAY_BUTTON_HEIGHT - 2*MARGIN_Y,
                -90, -15);
        quitButton.setTouchable(Touchable.enabled);

        // On charge l'image de fond
        bgImage = Ressources.getImgRsc(0);

        // On crée le logo
        Sprite logoSp = new Sprite(Ressources.getImgRsc(3));
        logoSp.setBounds((RoboGame.W_WIDTH-LOGO_WIDTH)/2f, 510, LOGO_WIDTH, LOGO_HEIGHT);

        // On crée un acteur associé au logo
        Actor logo = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                logoSp.draw(batch, parentAlpha);
            }
        };

        // On crée les engrenages
        bigWheel = new WheelSprite(1, -250, -250, 0.5f);
        smallWheel = new WheelSprite(2, RoboGame.W_WIDTH-200, RoboGame.W_HEIGHT-466, 1);

        // On ajoute les éléments créés
        stage.addActor(playButton);      stage.addActor(paramButton);       stage.addActor(quitButton);
        stage.addActor(bigWheel);        stage.addActor(smallWheel);        stage.addActor(logo);

        // On définit les actions pour chacun des boutons
        final MainMenu mainMenu = this;
        playButton.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {

                // On crée une action pour basculer à l'autre fenêtre
                RunnableAction run = new RunnableAction();
                run.setRunnable(() -> {
                    mainMenu.dispose();
                    game.setScreenState(RoboGame.EtatJeu.SELECT_LVL);
                });

                // On ajoute l'action précédée par un effet de fondu
                stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), run));

                return true;
            }
        });

        quitButton.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // TU PRENDS TON MANTEAU ON S'EN VA
                Runtime.getRuntime().halt(0);

                return false;
            }
        });
    }

    @Override
    public void show () {
        /** On définit cet écran comme capteur de l'input utilisateur **/
        Gdx.input.setInputProcessor(stage);
        /** On ajoute un effet fadeIn **/
        stage.addAction(Actions.sequence(Actions.alpha(0), Actions.fadeIn(1.2f)));
    }

    @Override
    public void render (float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // On dessine l'image de fond
        stage.getBatch().begin();
        stage.getBatch().draw(bgImage, 0, 0, W_WIDTH, W_HEIGHT);
        stage.getBatch().end();

        // On dessine le Stage
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize (int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {  Gdx.input.setInputProcessor(null);  }

}
