package color.guard.state;

import color.guard.rules.PieceKind;
import com.badlogic.gdx.utils.ObjectSet;
import squidpony.squidgrid.Direction;
import squidpony.squidmath.BardRNG;
import squidpony.squidmath.Coord;
import squidpony.squidmath.GreasedRegion;
import squidpony.squidmath.OrderedMap;
import squidpony.squidmath.OrderedSet;
import squidpony.squidmath.RNG;

/**
 * Created by Tommy Ettinger on 10/28/2016.
 */
public class BattleState {
    public OrderedMap<Coord, Piece> pieces;
    public int moverLimit;
    public OrderedSet<Coord> moveTargets;
    public RNG rng;
    public BardRNG bard;
    private transient GreasedRegion working;
    public int[][] map;
    public BattleState()
    {
        pieces = new OrderedMap<>(128);
        moverLimit = 0;
        moveTargets = new OrderedSet<>(128);
        rng = new RNG(bard = new BardRNG());
        map = new int[64][64];
        working = new GreasedRegion(64, 64);
    }
    public BattleState(long seed, int[][] map, Faction[] factions)
    {
        this.map = map;
        rng = new RNG(bard = new BardRNG(seed));
        int pieceCount = PieceKind.kinds.size(), mapWidth = map.length, mapHeight = map[0].length;
        working = new GreasedRegion(mapWidth, mapHeight);
        int[] tempOrdering = new int[pieceCount];
        pieces = new OrderedMap<>(16 + mapHeight * mapWidth >>> 4);
        moveTargets = new OrderedSet<>(16 + mapHeight * mapWidth >>> 4);
        Coord pt;
        ObjectSet<String> names = new ObjectSet<>(16 + mapHeight * mapWidth >>> 4);
        for (int x = mapWidth - 1; x >= 0; x--) {
            CELL_WISE:
            for (int y = mapHeight - 1; y >= 0; y--) {
                if(bard.next(4) == 0) {
                    rng.randomOrdering(pieceCount, tempOrdering);
                    for (int i = 0; i < pieceCount; i++) {
                        if((PieceKind.kinds.getAt(tempOrdering[i]).permits & 1 << map[x][y]) != 0)
                        {
                            Faction fact = Faction.whoOwns(x, y, rng, factions);
                            Piece p = new Piece(tempOrdering[i], fact);
                            while(names.contains(p.name))
                                p.resetName(fact);
                            pt = Coord.get(x, y);
                            pieces.put(pt, p);
                            names.add(p.name);
                            moveTargets.add(pt);
                            continue CELL_WISE;
                        }
                    }
                }
            }
        }
        moverLimit = pieces.size();
        Coord city, capital;
        Coord[] cities;
        for (int i = 0; i < factions.length; i++) {
            capital = factions[i].capital;
            working.remake(factions[i].territory).remove(capital);
            cities = working.randomSeparated(0.04, rng, 8);
            for (int j = 0; j < cities.length; j++) {
                city = cities[j];
                Piece p = new Piece(pieceCount, factions[i]);
                p.cityName(factions[i]);
                while(names.contains(p.name))
                    p.resetName(factions[i]);
                if (!pieces.containsKey(city)) {
                    pieces.put(city, p);
                    names.add(p.name);
                    moveTargets.add(city);
                }
            }
            working.surface().and(new GreasedRegion(map, 9).fringe()).remove(capital);
            cities = working.randomSeparated(0.03, rng, 3);
            for (int j = 0; j < cities.length; j++) {
                city = cities[j];
                Piece p = new Piece(pieceCount + 1, factions[i]);
                p.cityName(factions[i]);
                while(names.contains(p.name))
                    p.resetName(factions[i]);
                if (!pieces.containsKey(city)) {
                    pieces.put(city, p);
                    names.add(p.name);
                    moveTargets.add(city);
                }
            }
            city = capital;
            Piece p = new Piece(pieceCount + 2, factions[i]);
            p.cityName(factions[i]);
            while(names.contains(p.name))
                p.resetName(factions[i]);
            if(moveTargets.remove(city))
                --moverLimit;
            pieces.remove(city);
            pieces.put(city, p);
            names.add(p.name);
            moveTargets.add(city);
        }
    }

    public void advanceTurn()
    {
        int ct = moverLimit, r;
        Direction dir;
        Coord pt, next;
        Piece p;
        for (int i = 0; i < ct; i++) {
            pt = moveTargets.getAt(i);
            p = pieces.alterAt(i, pt);
            r = bard.next(3);
            if(r < 5)
            {
                dir = Piece.facingDirection(p.facing);
                next = pt.translateCapped(dir.deltaX, dir.deltaY, map.length, map[0].length);
                if(pieces.containsKey(next) || moveTargets.contains(next)
                        || (p.pieceKind.permits & 1 << map[next.x][next.y]) == 0)
                {
                    if(bard.nextInt() < 0)
                        p.facing = p.turnLeft();
                    else
                        p.facing = p.turnRight();
                }
                else
                {

                    moveTargets.alter(pt, next);
                }
            }
            else
            {
                if(bard.nextInt() < 0)
                    p.facing = p.turnLeft();
                else
                    p.facing = p.turnRight();
            }
        }
    }
}
