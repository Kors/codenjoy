package com.codenjoy.dojo.snake.battle.model.board;

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


import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.snake.battle.model.Player;
import com.codenjoy.dojo.snake.battle.model.hero.Hero;
import com.codenjoy.dojo.snake.battle.model.level.Level;
import com.codenjoy.dojo.snake.battle.model.objects.Apple;
import com.codenjoy.dojo.snake.battle.model.objects.StartFloor;
import com.codenjoy.dojo.snake.battle.model.objects.Stone;
import com.codenjoy.dojo.snake.battle.model.objects.Wall;
import com.codenjoy.dojo.snake.battle.services.Events;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * О! Это самое сердце игры - борда, на которой все происходит.
 * Если какой-то из жителей борды вдруг захочет узнать что-то у нее, то лучше ему дать интефейс {@see Field}
 * Борда реализует интерфейс {@see Tickable} чтобы быть уведомленной о каждом тике игры. Обрати внимание на {SnakeBoard#tick()}
 */
public class SnakeBoard implements Tickable, Field {

	public boolean debugMode = false;
	private static final int pause = 5;
    private List<Wall> walls;
    private List<StartFloor> starts;
    private List<Apple> apples;
    private List<Stone> stones;

    private List<Player> players;
    private int startCounter;

    private final int size;
    private Dice dice;

    public SnakeBoard(Level level, Dice dice) {
        this.dice = dice;
        walls = level.getWalls();
        starts = level.getStartPoints();
        apples = level.getApples();
        stones = level.getStones();
        size = level.getSize();
        players = new LinkedList<>();
        startCounter = pause;
    }

    /**
     * @see Tickable#tick()
     */
    @Override
    public void tick() {
        if (startCounter >= 0) {
            setStartCounter(startCounter - 1);
        }
        int dieCounter = 0;
        // продвижение живых змеек
        for (Player player : players) {
            if (startCounter == 0)
                player.event(Events.START);
            if (!player.isActive())
                continue;
            Hero hero = player.getHero();
            Point head = hero.getNextPoint();
            hero.tick();

            Point rand = getFreeRandom();

            if (apples.contains(head)) {
                apples.remove(head);
                apples.add(new Apple(rand));
                player.event(Events.APPLE);
            }
            if (stones.contains(head)) {
                stones.remove(head);
                stones.add(new Stone(rand));
                player.event(Events.STONE);
            }
            if (!hero.isAlive()) {
                dieCounter++;
                player.event(Events.DIE);
            }
        }
        // реакция на столкновения змей друг с другом, подсчёт живых.
        int activeCount = 0;
        for (Player player : players) {
            if (!player.isActive())
                continue;
            Hero hero = player.getHero();
            Hero enemy = checkHeadByHeadCollision(hero);
            if (!(enemy == null)) {
                int hSize = hero.size();
                hero.reduce(enemy.size());
                enemy.reduce(hSize);
            } else if (isAnotherHero(hero)) {
                player.getHero().die();
            }
            activeCount += player.isActive() ? 1 : 0;
            if(!player.isActive())
                dieCounter++;
        }
        // победа последнего игрока и рестарт игры
        if (activeCount < 2 && startCounter < 0) {
            for (Player player : players)
                if (player.isActive()) {
                    player.event(Events.WIN);
                    newGame(player);
                }
            setStartCounter(pause);
        } else {
            for (Player player : players)
                for (int i = 0; i < dieCounter; i++) {
                    player.event(Events.ALIVE);
                }
        }
    }

	public int size() {
        return size;
    }

    @Override
    public boolean isBarrier(Point p) {
        return p.isOutOf(size) || walls.contains(p) || starts.contains(p);
    }

    @Override
    public Point getFreeRandom() {
        int rndX = 0;
        int rndY = 0;
        int c = 0;
        do {
            rndX = dice.next(size);
            rndY = dice.next(size);
        } while (!isFree(rndX, rndY) && c++ < 100);

        if (c >= 100) {
            return PointImpl.pt(0, 0);
        }

        return PointImpl.pt(rndX, rndY);
    }

    @Override
    public Point getFreeStart() {
        for (StartFloor start : starts)
            if (freeOfHero(start))
                return start;
        return PointImpl.pt(0, 0);
    }

    @Override
    public boolean isFree(int x, int y) {
        Point pt = PointImpl.pt(x, y);
        return isFree(pt);
    }

    public boolean isFree(Point pt) {
        if (apples.contains(pt) ||
                stones.contains(pt) ||
                walls.contains(pt) ||
                starts.contains(pt))
            return false;
        return freeOfHero(pt);
    }

    private boolean freeOfHero(Point pt){
        for (Hero h : getHeroes()) {
            if (h != null && h.getBody().contains(pt) &&
                    !pt.equals(h.getTailPoint()))
                return false;
        }
        return true;
    }

    @Override
    public boolean isStone(Point p) {
        return stones.contains(p);
    }

    @Override
    public boolean isApple(Point p) {
        return apples.contains(p);
    }

	@Override
	public boolean isAnotherHero(Hero h) {
		for (Player anotherPlayer : players) {
			Hero enemy = anotherPlayer.getHero();
			if (enemy.equals(h))
				continue;
			if (!enemy.isAlive())
				continue;
			if (enemy.getBody().contains(h.getHead()) &&
					!enemy.getTailPoint().equals(h.getHead()))
				return true;
		}
		return false;
	}

	private Hero checkHeadByHeadCollision(Hero h) {
		for (Player anotherPlayer : players) {
			Hero enemy = anotherPlayer.getHero();
			if (enemy.equals(h))
				continue;
			if (!enemy.isAlive())
				continue;
			if (enemy.getHead().equals(h.getHead()) ||
					enemy.getNeck().equals(h.getHead()) && h.getNeck().equals(enemy.getHead()) )
				return anotherPlayer.getHero();
		}
		return null;
	}

    @Override
    public void setStone(Point p) {
        if (!stones.contains(p)) {
            stones.add(new Stone(p));
        }
    }

    @Override
    public void removeStone(Point p) {
        stones.remove(p);
    }

    public List<Apple> getApples() {
        return apples;
    }

    public List<Hero> getHeroes() {
        List<Hero> result = new ArrayList<>(players.size());
        for (Player player : players) {
            result.add(player.getHero());
        }
        return result;
    }

    public void newGame(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
        player.newHero(this);
    }

    public void remove(Player player) {
        players.remove(player);
    }

    public List<Wall> getWalls() {
        return walls;
    }

    public List<StartFloor> getStarts() {
        return starts;
    }

    public List<Stone> getStones() {
        return stones;
    }

    public BoardReader reader() {
        return new BoardReader() {
            private int size = SnakeBoard.this.size;

            @Override
            public int size() {
                return size;
            }

            @Override
            public Iterable<? extends Point> elements() {
                List<Point> result = new LinkedList<>();
                List<Hero> heroes = SnakeBoard.this.getHeroes();
                for (Hero hero : heroes)
                    result.addAll(hero.getBody());
                result.addAll(SnakeBoard.this.getWalls());
                result.addAll(SnakeBoard.this.getApples());
                result.addAll(SnakeBoard.this.getStones());
                result.addAll(SnakeBoard.this.getStarts());
                for(int i=0; i<result.size(); i++) {
                    Point p = result.get(i);
                    if (p.isOutOf(SnakeBoard.this.size())) { // TODO могут ли существовать объекты за границей поля? (выползать из-за края змея)
                        result.remove(p);
                    }
                }
                return result;
            }
        };
    }

    public void setStartCounter(int newValue) {
        if(!debugMode)
	        this.startCounter = newValue;
    }
}