package rs.pedjaapps.smc.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;

import java.util.ArrayList;
import java.util.List;

import rs.pedjaapps.smc.MaryoGame;
import rs.pedjaapps.smc.assets.Assets;
import rs.pedjaapps.smc.audio.MusicManager;
import rs.pedjaapps.smc.object.Box;
import rs.pedjaapps.smc.object.GameObject;
import rs.pedjaapps.smc.object.World;
import rs.pedjaapps.smc.object.maryo.Maryo;
import rs.pedjaapps.smc.utility.Constants;
import rs.pedjaapps.smc.utility.GameSave;
import rs.pedjaapps.smc.utility.LevelLoader;
import rs.pedjaapps.smc.utility.MyMathUtils;
import rs.pedjaapps.smc.utility.NAHudText;
import rs.pedjaapps.smc.utility.PrefsManager;
import rs.pedjaapps.smc.utility.TextUtils;
import rs.pedjaapps.smc.utility.Utility;
import rs.pedjaapps.smc.view.Background;
import rs.pedjaapps.smc.view.HUD;

import static rs.pedjaapps.smc.utility.GameSave.save;

public class GameScreen extends AbstractScreen implements InputProcessor {
    private static final float LEVEL_END_ANIMATION_DURATION = .5f;
    private static final String GOD_MOD_TEXT = "god";
    public OrthographicCamera cam;
    public OrthographicCamera guiCam;
    public HUD hud;
    public String levelName;
    public KillPointsTextHandler killPointsTextHandler;
    public String entryName;
    public GameScreen parent;
    public boolean resumed, forceCheckEnter;
    private World world;
    private OrthographicCamera pCamera;
    private ShapeRenderer shapeRenderer = new ShapeRenderer();
    /**
     * Textures *
     */
    private ParticleEffect globalEffect;
    private SpriteBatch spriteBatch;
    private boolean debug = PrefsManager.isDebug();
    private BitmapFont debugFont, debugObjectFont;
    private GlyphLayout debugGlyph;
    private Vector2 camMin = new Vector2();
    private Vector2 camMax = new Vector2();
    private float width, height;
    private Rectangle maryoBWO = new Rectangle();
    private GAME_STATE gameState;
    private LevelLoader loader;
    private Music music;
    private float goAlpha = 0.0f;
    private boolean goTouched = false;
    private boolean cameraForceSnap;
    private Vector3 cameraEditModeTranslate = new Vector3();
    private float levelEndAnimationStateTime;
    private String mNextLevelName;
    private StringBuilder mGodModInputCheckBuilder = new StringBuilder();

    public GameScreen(MaryoGame game, boolean fromMenu, String levelName) {
        this(game, fromMenu, levelName, null);
    }

    public GameScreen(MaryoGame game, boolean fromMenu, String levelName, GameScreen parent) {
        super(game);
        this.parent = parent;
        this.levelName = levelName;
        gameState = GAME_STATE.GAME_READY;
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        world = new World(this);
        hud = new HUD(world, this);
        this.cam = new OrthographicCamera(Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        this.cam.setToOrtho(false, Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        this.cam.update();

        pCamera = new OrthographicCamera(Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        pCamera.setToOrtho(false, Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        pCamera.position.set(Constants.CAMERA_WIDTH / 2f, Constants.CAMERA_HEIGHT / 2f, 0);
        pCamera.update();

        guiCam = new OrthographicCamera(width, height);
        guiCam.position.set(width / 2f, height / 2f, 0);
        guiCam.update();

        spriteBatch = new SpriteBatch();

        loader = new LevelLoader(levelName);
        //Gdx.graphics.setContinuousRendering(false);
        if (fromMenu) GameSave.startLevelFresh();
    }

    public GAME_STATE getGameState() {
        return gameState;
    }

    public void setGameState(GAME_STATE gameState) {
        this.gameState = gameState;
        hud.onGameStateChange();
        hud.updateTimer = !(gameState == GAME_STATE.PLAYER_DEAD || gameState == GAME_STATE.PLAYER_UPDATING ||
                gameState == GAME_STATE.SHOW_BOX || gameState == GAME_STATE.GAME_OVER);
        if (gameState == GAME_STATE.GAME_OVER) {
            music = game.assets.manager.get("data/music/game/lost_1.mp3");
            MusicManager.play(music);
        }
    }

    public Music getMusic() {
        return music;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void show() {
        GameSave.unlockLevel(levelName);
        music = game.assets.manager.get(loader.level.music.first());
        if (!resumed)
            music.setPosition(0);
        music.setLooping(true);
        MusicManager.play(music);
        if (debug) GLProfiler.enable();
        if (!resumed || forceCheckEnter) {
            world.maryo.checkLevelEnter(entryName);
            forceCheckEnter = false;
        }
        Gdx.input.setCatchBackKey(true);

        InputMultiplexer multiInput = new InputMultiplexer();
        multiInput.addProcessor(this);
        multiInput.addProcessor(hud.stage);

        Gdx.input.setInputProcessor(multiInput);
        if (!resumed) {
            game.levelStart(levelName);
        }
        if (resumed) {
            cameraForceSnap = true;
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);
        if (delta <= 0)
            return;

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //physics
        updateObjects(delta);
        //physics end

        moveCamera(cam, gameState == GAME_STATE.GAME_EDIT_MODE ? cameraEditModeTranslate : world.maryo.position,
                gameState == GAME_STATE.GAME_EDIT_MODE || (gameState != GAME_STATE.GAME_RUNNING && gameState !=
                        GAME_STATE.PLAYER_UPDATING && gameState != GAME_STATE.PLAYER_DEAD));
        drawBackground();
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        drawObjects();
        killPointsTextHandler.render(spriteBatch, delta);

        if (globalEffect != null) {
            globalEffect.setPosition(cam.position.x - Constants.CAMERA_WIDTH * 0.5f, cam.position.y + 0.5f *
                    Constants.CAMERA_HEIGHT);
            globalEffect.draw(spriteBatch);

            if (gameState == GAME_STATE.GAME_RUNNING) globalEffect.update(delta);
        }

        spriteBatch.end();

        spriteBatch.setProjectionMatrix(guiCam.combined);
        spriteBatch.begin();
        if (debug) drawDebugText();
        spriteBatch.end();
        if (debug) drawDebug();

        hud.render(gameState, delta);

        if (gameState == GAME_STATE.GAME_OVER) {
            handleGameOver();
        }

        if (gameState == GAME_STATE.GAME_LEVEL_END) {
            handleLevelEnded(delta);
        }

        //cleanup
        for (int i = 0; i < world.trashObjects.size; i++) {
            world.level.gameObjects.remove(world.trashObjects.get(i));
        }
        world.trashObjects.clear();

        //debug
        if (gameState == GAME_STATE.GAME_EDIT_MODE) {
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                cameraEditModeTranslate.x += 0.2f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                cameraEditModeTranslate.x -= 0.2f;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                cameraEditModeTranslate.y += 0.2f;
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                cameraEditModeTranslate.y -= 0.2f;
            }
        }
        if (debug) GLProfiler.reset();
    }

    public void endLevel(String nextLevelName) {
        mNextLevelName = nextLevelName;
        levelEndAnimationStateTime = LEVEL_END_ANIMATION_DURATION;
        setGameState(GAME_STATE.GAME_LEVEL_END);
    }

    private void handleLevelEnded(float delta) {
        if (levelEndAnimationStateTime <= 0) {
            if ("game_tutorial".equals(levelName)) {
                GameSave.reset();
            }
            world.screen.game.levelEnd(((GameScreen) world.screen).levelName, true);
            game.setScreen(new LoadingScreen(new GameScreen(game, false, mNextLevelName), false));
            mNextLevelName = null;
            game.showAd();
            return;
        }
        levelEndAnimationStateTime -= delta;

        float percent = 1 - levelEndAnimationStateTime / LEVEL_END_ANIMATION_DURATION;

        float camWidth = hud.stage.getWidth();
        float camHeight = hud.stage.getHeight();

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        shapeRenderer.setProjectionMatrix(hud.stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(0, 0, 0, percent);
        shapeRenderer.rect(0, 0, camWidth, camHeight);

        shapeRenderer.end();
    }

    public void showBoxText(Box box) {
        setGameState(GAME_STATE.SHOW_BOX);
        hud.showPopupBox(box.text);
    }

    public void discardBoxText() {
        if (gameState == GAME_STATE.SHOW_BOX) {
            if (hud.hidePopupBox())
                setGameState(GAME_STATE.GAME_RUNNING);
        }
    }

    private void handleGameOver() {
        if (save.lifes < 0) {
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl.glEnable(GL20.GL_BLEND);

            shapeRenderer.setProjectionMatrix(guiCam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(0, 0, width, height);
            shapeRenderer.end();

            spriteBatch.setProjectionMatrix(guiCam.combined);
            spriteBatch.begin();

            Texture go = game.assets.manager.get("data/game/game_over.png");
            go.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            float width = this.width * 0.8f;
            float height = width / 4;

            float x = this.width / 2 - width / 2;
            float y = this.height / 2 - height / 2;
            spriteBatch.draw(go, x, y, width, height);

            spriteBatch.end();
            if (!goTouched) return;
        }

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        shapeRenderer.setProjectionMatrix(guiCam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, goAlpha += 0.033f);
        shapeRenderer.rect(0, 0, width, height);
        shapeRenderer.end();

        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        spriteBatch.end();
        //background changes to black if i don't add this after blend

        if (goAlpha >= 1) {
            if (save.lifes < 0) {
                game.setScreen(new LoadingScreen(new MainMenuScreen(game), false));
            } else {
                game.setScreen(new LoadingScreen(new GameScreen(game, false, levelName, parent), false));
            }
            game.levelEnd(levelName, false);
        }
    }

    public void won() {
        music = game.assets.manager.get("data/music/game/courseclear.mp3");
        //TODO einfache Verzögerung!?
        music.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                game.setScreen(new LoadingScreen(new MainMenuScreen(game), false));
            }
        });
        MusicManager.play(music);
    }

    private void drawBackground() {
        for (Background background : world.level.backgrounds) {
            background.render(cam, spriteBatch);
        }
    }

    private void moveCamera(OrthographicCamera cam, Vector3 pos, boolean snap) {
        if ((gameState == GAME_STATE.PLAYER_UPDATING && !world.maryo.entering && !world.maryo.exiting))
            return;
        if (snap || cameraForceSnap) {
            cam.position.set(pos);
            cameraForceSnap = false;
        } else {
            cam.position.lerp(pos, 0.05f);
        }
        cam.update();
        if (gameState != GAME_STATE.GAME_EDIT_MODE) keepCameraInBounds(cam);
    }

    private void keepCameraInBounds(OrthographicCamera cam) {
        float camX = cam.position.x;
        float camY = cam.position.y;

        camMin.set(cam.viewportWidth, cam.viewportHeight);
        camMin.scl(cam.zoom / 2); //bring to center and scale by the zoom level
        camMax.set(world.level.width, world.level.height);
        camMax.sub(camMin); //bring to center

        //keep camera within borders
        camX = MyMathUtils.min(camMax.x, MyMathUtils.max(camX, camMin.x));
        camY = MyMathUtils.min(camMax.y, MyMathUtils.max(camY, camMin.y));

        cam.position.set(camX, camY, cam.position.z);
        cam.update();
    }

    private void updateObjects(float delta) {
        world.createMaryoRectWithOffset(maryoBWO, 8);
        for (int i = 0, size = world.level.gameObjects.size(); i < size; i++) {
            GameObject go = world.level.gameObjects.get(i);
            if (maryoBWO.overlaps(go.mColRect)) {
                if (gameState == GAME_STATE.GAME_RUNNING || ((gameState == GAME_STATE.PLAYER_DEAD || gameState ==
                        GAME_STATE.PLAYER_UPDATING) && go instanceof Maryo)) {
                    go._update(delta);
                }
            }
        }
    }

    private void drawObjects() {
        world.drawVisibleObjects(cam, spriteBatch);
    }

    private void drawDebug() {
        // render blocks
        shapeRenderer.setProjectionMatrix(cam.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < world.getVisibleObjects().size; i++) {
            GameObject go = world.getVisibleObjects().get(i);
            Rectangle colRect = go.mColRect;
            Rectangle drawRect = go.mDrawRect;
            shapeRenderer.setColor(0, 1, 0, 1);
            shapeRenderer.rect(colRect.x, colRect.y, colRect.width, colRect.height);
            shapeRenderer.setColor(1, 0, 0, 1);
            shapeRenderer.rect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
        }
        // render maryo
        Maryo maryo = world.maryo;
        Rectangle body = maryo.mColRect;
        Rectangle bounds = maryo.mDrawRect;
        world.createMaryoRectWithOffset(maryoBWO, 8);
        shapeRenderer.setColor(0, 1, 0, 1);
        shapeRenderer.rect(body.x, body.y, body.width, body.height);
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.setColor(0, 0, 1, 1);
        shapeRenderer.rect(maryo.debugRayRect.x, maryo.debugRayRect.y, maryo.debugRayRect.width, maryo.debugRayRect
                .height);
        shapeRenderer.setColor(0.3f, 0.9f, 0, 0);
        shapeRenderer.rect(maryoBWO.x, maryoBWO.y, maryoBWO.width, maryoBWO.height);
        shapeRenderer.end();
    }

    private void drawDebugText() {
        String debugMessage = generateDebugMessage();
        debugGlyph.setText(debugFont, debugMessage);
        debugFont.draw(spriteBatch, debugMessage, 20, height - 20);

        Vector2 point = World.VECTOR2_POOL.obtain();
        float x = Gdx.input.getX();
        float y = invertY(Gdx.input.getY());
        Utility.guiPositionToGamePosition(x, y, this, point);

        for (GameObject gameObject : world.level.gameObjects) {
            if (gameObject.mDrawRect.contains(point)) {
                String objectDebugText = gameObject.toString();
                float tWidth = width * 0.4f;
                debugGlyph.setText(debugObjectFont, objectDebugText, Color.BLACK, tWidth, Align.left, true);
                float height = debugGlyph.height;
                debugObjectFont.draw(spriteBatch, debugGlyph, x - tWidth, y + height);
                break;
            }
        }

        World.VECTOR2_POOL.free(point);
    }

    private String generateDebugMessage() {
        return "Level: width=" + world.level.width + ", height=" + world.level.height
                + "\n" + "Player: x=" + world.maryo.position.x + ", y=" + world.maryo.position.y
                + "\n" + "LevelName: " + levelName
                + "\n" + "Player Vel: x=" + world.maryo.velocity.x + ", y=" + world.maryo.velocity.y
                + "\n" + "World Camera: x=" + cam.position.x + ", y=" + cam.position.y
                + "\n" + "JavaHeap: " + Gdx.app.getJavaHeap() / 1000000 + "MB"
                + "\n" + "NativeHeap: " + Gdx.app.getNativeHeap() / 1000000 + "MB"
                + "\n" + "OGL Draw Calls: " + GLProfiler.drawCalls
                + "\n" + "OGL TextureBindings: " + GLProfiler.textureBindings
                + "\n" + "Screen w=" + width + "h=" + height
                + "\n" + "FPS: " + Gdx.graphics.getFramesPerSecond();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.width = width;
        this.height = height;

        Constants.initCamera();

        cam = new OrthographicCamera(Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        cam.setToOrtho(false, Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        cam.update();

        pCamera = new OrthographicCamera(Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        pCamera.setToOrtho(false, Constants.CAMERA_WIDTH, Constants.CAMERA_HEIGHT);
        pCamera.position.set(Constants.CAMERA_WIDTH / 2f, Constants.CAMERA_HEIGHT / 2f, 0);
        pCamera.update();

        guiCam = new OrthographicCamera(width, height);
        guiCam.position.set(width / 2f, height / 2f, 0);
        guiCam.update();

        for (Background background : world.level.backgrounds) {
            background.resize(cam);
        }
        hud.resize(width, height);
    }

    @Override
    public void hide() {
        setGameState(GAME_STATE.GAME_PAUSED);
        music.stop();
    }

    @Override
    public void pause() {
        super.pause();
        setGameState(GAME_STATE.GAME_PAUSED);
        music.stop();
    }

    @Override
    public void resume() {
        MusicManager.play(music);
    }

    @Override
    public void dispose() {
        super.dispose();
        music.stop();
        hud.dispose();
        Gdx.input.setInputProcessor(null);
        game.assets.dispose();
        world.dispose();
        if (globalEffect != null) {
            globalEffect.dispose();
        }
        if (debug) GLProfiler.disable();
    }

    @Override
    public void loadAssets() {
        loader.parseLevel(world);
        for (Maryo.MaryoState ms : Maryo.MaryoState.values()) {
            game.assets.manager.load("data/maryo/" + ms.toString() + ".pack", TextureAtlas.class);
        }
        game.assets.manager.load("data/animation/fireball.pack", TextureAtlas.class);
        game.assets.manager.load("data/animation/particles/fireball_emitter_2.p", ParticleEffect.class, game.assets
                .particleEffectParameter);
        game.assets.manager.load("data/animation/particles/fireball_explosion_emitter.p", ParticleEffect.class, game
                .assets.particleEffectParameter);
        game.assets.manager.load("data/animation/particles/iceball_emitter.p", ParticleEffect.class, game.assets
                .particleEffectParameter);
        game.assets.manager.load("data/animation/particles/iceball_explosion_emitter.p", ParticleEffect.class, game
                .assets.particleEffectParameter);
        game.assets.manager.load("data/animation/particles/star_trail.p", ParticleEffect.class, game.assets
                .particleEffectParameter);
        game.assets.manager.load("data/animation/particles/maryo_star.p", ParticleEffect.class, game.assets
                .particleEffectParameter);
        game.assets.manager.load("data/animation/iceball.png", Texture.class, game.assets.textureParameter);
        game.assets.manager.load("data/game/game_over.png", Texture.class);
        hud.loadAssets();

        //audio
        game.assets.manager.load("data/sounds/audio_on.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/goldpiece_1.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/goldpiece_red.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/dead.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/jump_big.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/jump_big_power.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/jump_small.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/jump_small_power.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/pickup_item.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/powerdown.mp3", Sound.class);
        game.assets.manager.load("data/sounds/player/run_stop.mp3", Sound.class);
        game.assets.manager.load("data/sounds/wall_hit.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/fireball.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/iceball.mp3", Sound.class);

        game.assets.manager.load("data/sounds/sprout_1.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/star_kill.mp3", Sound.class);
        game.assets.manager.load("data/sounds/itembox_set.mp3", Sound.class);
        game.assets.manager.load("data/sounds/leave_pipe.mp3", Sound.class);
        game.assets.manager.load("data/sounds/enter_pipe.mp3", Sound.class);

        game.assets.manager.load("data/sounds/enemy/furball/die.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/fireball_repelled.mp3", Sound.class);

        game.assets.manager.load("data/sounds/item/iceball_explosion.mp3", Sound.class);
        game.assets.manager.load("data/sounds/item/fireball_explosion.mp3", Sound.class);

        game.assets.manager.load("data/music/game/lost_1.mp3", Music.class);
        game.assets.manager.load("data/music/game/courseclear.mp3", Music.class);

    }

    @Override
    public void onAssetsLoaded() {
        hud.initAssets();
        world.level = loader.level;

        debugFont = new BitmapFont();
        debugFont.setColor(1, 0, 0, 1);

        debugObjectFont = new BitmapFont();
        debugGlyph = new GlyphLayout();

        for (GameObject go : loader.level.gameObjects) {
            go.initAssets();
        }

        BitmapFont pointsFont = game.assets.manager.get(Assets.SKIN_HUD, Skin.class).getFont(Assets.FONT_SIMPLE25);
        pointsFont.setColor(1, 1, 1, 1);
        killPointsTextHandler = new KillPointsTextHandler(pointsFont);
        for (Background background : world.level.backgrounds) {
            background.onAssetsLoaded(cam, game.assets);
        }

        if (!TextUtils.isEmpty(world.level.particleEffect)) {
            globalEffect = new ParticleEffect(game.assets.manager.get(world.level.particleEffect, ParticleEffect
                    .class));
            globalEffect.start();
        }
    }

    // * InputProcessor methods ***************************//

    @Override
    public boolean keyDown(int keycode) {
        if (gameState == GAME_STATE.GAME_READY)
            setGameState(GAME_STATE.GAME_RUNNING);
        else if (gameState == GAME_STATE.SHOW_BOX)
            discardBoxText();

        if (gameState == GAME_STATE.GAME_PAUSED && keycode == Input.Keys.ENTER)
            setGameState(GAME_STATE.GAME_RUNNING);
        else if (gameState == GAME_STATE.GAME_PAUSED)
            return false;

        if (keycode == Input.Keys.LEFT)
            world.maryo.leftPressed();

        if (keycode == Input.Keys.RIGHT)
            world.maryo.rightPressed();

        if (keycode == Input.Keys.SPACE)
            world.maryo.jumpPressed();

        if (keycode == Input.Keys.ALT_LEFT)
            world.maryo.firePressed();

        if (keycode == Input.Keys.X)
            world.maryo.firePressed();

        if (keycode == Input.Keys.DOWN)
            world.maryo.downPressed();

        if (keycode == Input.Keys.UP)
            world.maryo.upPressed();

        if (keycode == Input.Keys.F8) {
            if (gameState == GAME_STATE.GAME_EDIT_MODE) {
                setGameState(GAME_STATE.GAME_RUNNING);
                cam.zoom = 1;
            } else {
                cameraEditModeTranslate.set(cam.position);
                setGameState(GAME_STATE.GAME_EDIT_MODE);
            }
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {

        if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            if (gameState == GAME_STATE.GAME_PAUSED) {
                exitToMenu();
            } else {
                setGameState(GAME_STATE.GAME_PAUSED);
            }
            return true;
        }

        if (gameState == GAME_STATE.GAME_PAUSED)
            return false;

        if (keycode == Input.Keys.LEFT)
            world.maryo.leftReleased();

        if (keycode == Input.Keys.RIGHT)
            world.maryo.rightReleased();

        if (keycode == Input.Keys.SPACE)
            world.maryo.jumpReleased();

        if (keycode == Input.Keys.ALT_LEFT)
            world.maryo.fireReleased();

        if (keycode == Input.Keys.X)
            world.maryo.fireReleased();

        if (keycode == Input.Keys.DOWN)
            world.maryo.downReleased();

        if (keycode == Input.Keys.UP)
            world.maryo.upReleased();

        if (keycode == Input.Keys.D)
            debug = !debug;

        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        mGodModInputCheckBuilder.append(character);
        if (mGodModInputCheckBuilder.length() == GOD_MOD_TEXT.length()) {
            if (mGodModInputCheckBuilder.toString().equals(GOD_MOD_TEXT)) {
                Maryo.STAR_EFFECT_TIMEOUT = Float.MAX_VALUE;
                world.maryo.canWalkOnAir = true;
                world.maryo.starPicked();
            } else {
                mGodModInputCheckBuilder = new StringBuilder();
            }
        }
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (gameState == GAME_STATE.GAME_READY)
            setGameState(GAME_STATE.GAME_RUNNING);
        else if (gameState == GAME_STATE.SHOW_BOX)
            discardBoxText();
        else if (gameState == GAME_STATE.GAME_OVER)
            goTouched = true;
        else
            return false;

        return true;
    }

    private float invertY(float y) {
        return height - y;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (gameState == GAME_STATE.GAME_EDIT_MODE || debug) {
            cam.zoom += amount * 0.1f;
            cam.update();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public enum GAME_STATE {
        GAME_READY, GAME_RUNNING, GAME_PAUSED, GAME_LEVEL_END, GAME_OVER, PLAYER_DEAD,
        SHOW_BOX, PLAYER_UPDATING, GAME_EDIT_MODE
    }

    public static class KillPointsTextHandler {
        private final List<KillPoint> pointsTextPool = new ArrayList<>(10);
        NAHudText<Integer> text = new NAHudText<>(null, null);
        private BitmapFont font;

        KillPointsTextHandler(BitmapFont font) {
            this.font = font;
        }

        public void add(int points, float positionX, float positionY) {
            for (KillPoint point : pointsTextPool) {
                if (point.recycled) {
                    point.reset(positionX, positionY, points);
                    return;
                }
            }
            KillPoint point = new KillPoint(points, positionX, positionY);
            pointsTextPool.add(point);
        }

        public void render(SpriteBatch batch, float deltaTime) {
            //noinspection ForLoopReplaceableByForEach
            float oldScale = font.getData().scaleX;
            boolean useInt = font.usesIntegerPositions();
            font.getData().setScale(0.015f);
            font.setUseIntegerPositions(false);

            for (int i = 0, size = pointsTextPool.size(); i < size; i++) {
                KillPoint point = pointsTextPool.get(i);
                if (!point.recycled) {
                    point.draw(batch, deltaTime, font, text);
                }
            }

            font.getData().setScale(oldScale);
            font.setUseIntegerPositions(useInt);

        }

        private static class KillPoint {
            static final float velocity = 0.9f;
            static final float maxDistance = 0.4f;
            private boolean recycled = false;
            private int points;
            private float positionX, positionY, origPosY;
            private float alpha = 1;

            KillPoint(int points, float positionX, float positionY) {
                this.points = points;
                this.positionX = positionX;
                this.positionY = positionY;
                this.origPosY = positionY;
            }

            public void draw(SpriteBatch spriteBatch, float deltaTime, BitmapFont font, NAHudText<Integer> text) {
                if (positionY >= origPosY + maxDistance) {
                    recycled = true;
                    return;
                }
                float velDelta = velocity * deltaTime;
                positionY += maxDistance * velDelta;
                alpha -= 1 / (maxDistance / (maxDistance * velDelta));
                font.getColor().set(1, 1, 1, alpha);
                font.draw(spriteBatch, text.toString(points), positionX, positionY);
            }

            void reset(float posX, float posY, int points) {
                recycled = false;
                positionX = posX;
                positionY = posY;
                this.points = points;
                origPosY = posY;
                alpha = 1;
            }
        }
    }
}
