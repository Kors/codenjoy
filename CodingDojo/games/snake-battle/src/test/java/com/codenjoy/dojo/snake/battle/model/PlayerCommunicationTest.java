package com.codenjoy.dojo.snake.battle.model;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2016 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.codenjoy.dojo.services.Dice;
import com.codenjoy.dojo.services.EventListener;
import com.codenjoy.dojo.services.PrinterFactory;
import com.codenjoy.dojo.services.PrinterFactoryImpl;
import com.codenjoy.dojo.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Kors
 */
public class PlayerCommunicationTest {
    private SnakeBoard game;
    private Hero hero;
    private Hero enemy;
    private Dice dice;
    private EventListener listener;
    private Player player;
    private PrinterFactory printer = new PrinterFactoryImpl();

    String simpleField =
            "☼☼☼☼☼☼☼" +
                    "☼     ☼" +
                    "☼ →►  ☼" +
                    "☼     ☼" +
                    "☼ ⇒>  ☼" +
                    "☼     ☼" +
                    "☼☼☼☼☼☼☼";

    @Before
    public void setup() {
        dice = mock(Dice.class);
    }

    private void givenFl(String board) {
        LevelImpl level = new LevelImpl(board);
        Hero hero = level.getHero().get(0);
        Hero enemy = level.getEnemy().get(0);
        game = new SnakeBoard(level, dice);
        listener = mock(EventListener.class);
        player = new Player(listener);
        Player player2 = new Player(mock(EventListener.class));
        game.newGame(player);
        game.newGame(player2);
        player2.setHero(enemy);
        player.setHero(hero);
        hero.init(game);
        enemy.init(game);
        this.hero = game.getHeroes().get(0);
        this.enemy = game.getHeroes().get(1);
    }

    // проверяем что соперник отображается на карте
    @Test
    public void enemyOnField() {
        givenFl(simpleField);
        assertE(simpleField);
    }

    // проверяем что змейка умирает, когда врезается в соперника
    @Test
    public void diedByAnotherHero() {
        givenFl(simpleField);
        hero.down();
        enemy.up();
        game.tick();
        assertE("☼☼☼☼☼☼☼" +
                "☼     ☼" +
                "☼  ↓  ☼" +
                "☼  ☻  ☼" +
                "☼  ⇑  ☼" +
                "☼     ☼" +
                "☼☼☼☼☼☼☼");
        game.tick();
        assertE("☼☼☼☼☼☼☼" +
                "☼     ☼" +
                "☼     ☼" +
                "☼     ☼" +
                "☼     ☼" +
                "☼     ☼" +
                "☼☼☼☼☼☼☼");
    }

    private void assertE(String expected) {
        assertEquals(TestUtils.injectN(expected),
                printer.getPrinter(game.reader(), player).print());
    }
}