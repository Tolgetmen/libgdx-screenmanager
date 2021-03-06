package de.eskalon.commons.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import de.eskalon.commons.LibgdxUnitTest;
import de.eskalon.commons.input.BasicInputMultiplexer;
import de.eskalon.commons.screen.transition.ScreenTransition;

public class ScreenManagerTest2 extends LibgdxUnitTest {

	private int i = 0;
	private int k = 0;
	private int z = 0;

	private boolean firstRenderPassTransition = true,
			firstRenderPassScreen2 = true;

	/**
	 * Tests whether the screens are shown and hidden correctly while a
	 * transition is rendering as well as whether the input handlers are
	 * unregistered while transitioning.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testScreenLifecycleWhileTransition()
			throws TimeoutException, InterruptedException {
		BasicInputMultiplexer mult = new BasicInputMultiplexer();

		// Mock initBuffers() & screenToTexture() as they are using open gl
		// stuff
		ScreenManager<ManagedScreen, ScreenTransition> sm = new ScreenManager() {
			@Override
			TextureRegion screenToTexture(ManagedScreen screen,
					com.badlogic.gdx.graphics.glutils.FrameBuffer FBO,
					float delta) {
				screen.render(delta); // only render the screen

				return null;
			};

			@Override
			protected void initBuffers() {
				// do nothing
			}
		};
		sm.initialize(mult, 5, 5);

		ManagedScreen testScreen = new ManagedScreen() {
			@Override
			public void show() {
				initializeScreen(); // instead of super.show();

				assertEquals(0, i);
				i = 1;
			}

			@Override
			protected void create() {
				addInputProcessor(new InputAdapter());
			}

			@Override
			public void render(float delta) {
				z++;
				switch (z) { // ignore the first render pass when this screen is
								// pushed
				case 2: { // second render pass; while the transition is
							// rendered
					assertEquals(2, i);
					i = 3;
					break;
				}
				case 3: {
					assertEquals(15, (int) delta);
					assertEquals(5, i);
					i = 6;
					break;
				}
				case 4: {
					fail();
				}
				}
			}

			@Override
			public void hide() {
				assertEquals(8, i);
				i = 9;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void resize(int width, int height) {
			}

			@Override
			public boolean equals(Object obj) {
				return this == obj;
			}
		};

		ManagedScreen test2Screen = new ManagedScreen() {
			@Override
			public void show() {
				initializeScreen(); // instead of super.show();

				assertEquals(1, i);
				i = 2;
			}

			@Override
			protected void create() {
				addInputProcessor(new InputAdapter());
				addInputProcessor(new InputAdapter());
				addInputProcessor(new InputAdapter());
			}

			@Override
			public void render(float delta) {
				if (firstRenderPassScreen2) { // ignore the first render pass
												// when the second screen is
												// pushed
					firstRenderPassScreen2 = false;
					assertEquals(3, i);
					i = 4;
				} else {
					if (delta == 15) {
						assertEquals(6, i);
						i = 7;
					} else {
						assertEquals(9, i);
						i = 10;
					}
				}
			}

			@Override
			public void hide() {
			}

			@Override
			public void dispose() {
			}

			@Override
			public void resize(int width, int height) {
			}

			@Override
			public boolean equals(Object obj) {
				return this == obj;
			}
		};

		String screenName = "Test";
		String screen2Name = "Test2";

		sm.addScreen(screenName, testScreen);
		sm.addScreen(screen2Name, test2Screen);

		ScreenTransition transition = new ScreenTransition() {
			@Override
			public void reset() {
				super.reset();
				assertEquals(1, k);
				k = 2;
			}

			@Override
			public void render(float delta, TextureRegion currScreen,
					TextureRegion nextScreen) {
				if (firstRenderPassTransition) {
					firstRenderPassTransition = false;
					assertEquals(4, i);
					i = 5;
				} else {
					assertEquals(7, i);
					assertEquals(2, k);
					k = (int) delta;
				}
			}

			@Override
			public boolean isDone() {
				return k == 15;
			}

			@Override
			protected void create() {
				assertEquals(0, k);
				k = 1;
			}

			@Override
			public void resize(int width, int height) {
			}

			@Override
			public void dispose() {
			}

		};

		String transitionName = "Transition";

		sm.addScreenTransition(transitionName, transition);

		assertEquals(1, sm.getScreenTransitions().size());
		assertEquals(transition, sm.getScreenTransition(transitionName));

		// Push the first screen
		sm.pushScreen(screenName, null);
		assertEquals(null, sm.getCurrentScreen());
		sm.render(1F);
		assertEquals(1, i);

		assertEquals(1, mult.getProcessors().size);
		assertEquals(testScreen, sm.getCurrentScreen());
		assertEquals(null, sm.getLastScreen());

		// Push the second screen using a transition
		sm.pushScreen(screen2Name, transitionName);
		sm.render(1F);
		assertEquals(5, i);

		assertEquals(0, mult.getProcessors().size);
		assertEquals(test2Screen, sm.getCurrentScreen());
		assertEquals(testScreen, sm.getLastScreen());
		assertTrue(sm.inTransition());
		assertTrue(!transition.isDone());

		// Render enough so the transition is marked as done
		sm.render(15);

		assertEquals(0, mult.getProcessors().size);
		assertEquals(test2Screen, sm.getCurrentScreen()); // didnt change
		assertEquals(testScreen, sm.getLastScreen()); // didnt change
		assertTrue(transition.isDone());

		assertEquals(7, i);
		i = 8;

		// In the next render pass the transition is finished
		sm.render(1);
		assertEquals(10, i);
		i = 11; // end

		assertEquals(3, mult.getProcessors().size);
		assertTrue(!sm.inTransition());

		// Try to initialize the transition a second time
		transition.initializeScreenTransition();
	}

}
