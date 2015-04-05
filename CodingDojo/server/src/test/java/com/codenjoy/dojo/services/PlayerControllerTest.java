package com.codenjoy.dojo.services;

import com.codenjoy.dojo.integration.mocker.SpringMockerJettyRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * User: oleksandr.baglai
 * Date: 10/1/12
 * Time: 4:30 AM
 */
public class PlayerControllerTest {

    public static final int PORT = 8081;
    public static final String CONTEXT_PATH = "/codenjoy-contest";
    private static WebSocketRunner client;

    private static String url;

    private static WsPlayerController controller;
    private static TimerService timer;
    private static SpringMockerJettyRunner runner;

    private static Joystick joystick;
    private static Player player;

    private static final String SERVER = "ws://127.0.0.1:" + PORT + CONTEXT_PATH + "/ws";
    private static String USER_NAME = "apofig";

    private static List<String> serverMessages = new LinkedList<String>();

    @BeforeClass
    public static void setupJetty() throws Exception {
        runner = new SpringMockerJettyRunner("src/main/webapp", CONTEXT_PATH);
        int port = runner.start(PORT);

        url = runner.getUrl();
        System.out.println("web application started at: " + url);

        timer = runner.getBean(TimerService.class, "timerService");
        controller = runner.getBean(WsPlayerController.class, "wsPlayerController");

        timer.pause();

        joystick = new Joystick() {
            @Override
            public void down() {
                serverMessages.add("down");
            }

            @Override
            public void up() {
                serverMessages.add("up");
            }

            @Override
            public void left() {
                serverMessages.add("left");
            }

            @Override
            public void right() {
                serverMessages.add("right");
            }

            @Override
            public void act(int... p) {
                serverMessages.add("act" + Arrays.toString(p));
            }
        };

        player = new Player(USER_NAME, "127.0.0.1", PlayerTest.mockGameType("game"),
                NullPlayerScores.INSTANCE, NullInformation.INSTANCE, Protocol.WS);

        controller.registerPlayerTransport(player, joystick);

        client = WebSocketRunner.run(SERVER, USER_NAME);
    }

    @Before
    public void clean() {
        client.reset();
        serverMessages.clear();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.stop();
    }

    @Test
    public void shouldLeft() {
        client.willAnswer("LEFT");
        waitForPlayerResponse();

        assertEquals("[left]", serverMessages.toString());
    }

    @Test
    public void shouldRight() {
        client.willAnswer("right");
        waitForPlayerResponse();

        assertEquals("[right]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldUp() {
        client.willAnswer("Up");
        waitForPlayerResponse();

        assertEquals("[up]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldAct() {
        client.willAnswer("aCt");
        waitForPlayerResponse();

        assertEquals("[act[]]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldActWithParameters() {
        client.willAnswer("ACt(1,2 ,3, 5)");
        waitForPlayerResponse();

        assertEquals("[act[1, 2, 3, 5]]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldDown() {
        client.willAnswer("DowN");
        waitForPlayerResponse();

        assertEquals("[down]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldRightAct() {
        client.willAnswer("right,Act");
        waitForPlayerResponse();

        assertEquals("[right, act[]]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldMixed() {
        client.willAnswer("Act,right, left ,act");
        waitForPlayerResponse();

        assertEquals("[act[], right, left, act[]]", serverMessages.toString());
        clean();
    }

    @Test
    public void shouldCheckRequest() {
        client.willAnswer("act");
        waitForPlayerResponse();

        assertEquals("board=some-request-0", client.getRequest());
    }

    private void waitForPlayerResponse() {
        waitForPlayerResponse(1);
    }

    @Test
    public void shouldServerGotOnlyOneWhenClientAnswerTwice() {
        // given, when
        client.willAnswer("LEFT").times(2);
        waitForPlayerResponse();

        // then
        assertEquals("[board=some-request-0]", client.messages.toString());
        assertEquals("[left]", serverMessages.toString());
    }

    private void waitForPlayerResponse(int times) {
        try {
            for (int index = 0; index < times; index++) {
                controller.requestControl(player, "some-request-" + index);
            }
            while (serverMessages.isEmpty()) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldClientGotOnlyOneWhenServerRequestTwice() {
        // given, when
        client.willAnswer("LEFT").times(1).onlyOnce();
        waitForPlayerResponse(2);

        // then
        assertEquals("[board=some-request-0]", client.messages.toString());
        assertEquals("[left]", serverMessages.toString());
    }
}