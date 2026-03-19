/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Innkeeper;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

public class VillageLevel extends Level {

	{
		color1 = 0x48763c;
		color2 = 0x59994a;

		viewDistance = 36;
	}

	private static final int WIDTH  = 90;
	private static final int HEIGHT = 48;

	// ═══════════════════════════════════════════════════════════════
	//  Layout matching the reference image:
	//
	//   ┌──────────────────────────────────┐
	//   │  POND/LAKE        │  KITCHEN     │
	//   │  (water)          │  (tavern)    │
	//   │                   │              │
	//   ├───────path────WELL────path───────┤
	//   │  BLACKSMITH       │              │
	//   │  (forge)          │              │
	//   │                   │              │
	//   ├───────path────────────path───────┤
	//   │  INN              │  ARMORY      │
	//   │  (beds/books)     │  (shop)      │
	//   │                   │     EXIT↓    │
	//   └──────────────────────────────────┘
	// ═══════════════════════════════════════════════════════════════

	// ── Pond (top-left) ───────────────────────────────────────────
	private static final Rect pondOuter  = new Rect(1,  1,  12, 9);
	private static final Rect pondWater  = new Rect(1,  1,  12, 9);

	// ── Kitchen / Tavern (top-right) ──────────────────────────────
	private static final Rect kitchenBuilding = new Rect(19, 2,  28, 9);
	private static final Point kitchenDoor    = new Point(19, 5);  // left wall, faces the path

	// ── Blacksmith / Forge (middle-left) ──────────────────────────
	private static final Rect forgeBuilding   = new Rect(2,  12, 10, 19);
	private static final Point forgeDoor      = new Point(9, 15); // right wall, faces center

	// ── Well + central square ─────────────────────────────────────
	private static final Rect centralSquare   = new Rect(12, 11, 20, 19);
	private static final Point wellPos        = new Point(15, 15);

	// ── Inn (bottom-left) ─────────────────────────────────────────
	private static final Rect innBuilding     = new Rect(2,  22, 11, 29);
	private static final Point innDoor        = new Point(11, 25); // right wall, faces path

	// ── Armory / Shop (bottom-right) ──────────────────────────────
	private static final Rect armoryBuilding  = new Rect(19, 22, 28, 29);
	private static final Point armoryDoor     = new Point(19, 25); // left wall, faces path

	// ── Dungeon entrance (inside/near armory area) ────────────────
	private static final int DUNGEON_ENTRANCE_POS = 23 + 28 * WIDTH;

	// ── Innkeeper position (inside kitchen) ───────────────────────
	private static final Point innkeeperPos   = new Point(23, 5);

	// ── Torch / lamp positions ────────────────────────────────────
	private static final Point[] torchPositions = new Point[]{
			// Pond corners
			// Kitchen
			new Point(19, 2),  new Point(27, 2),
			// Forge
			new Point(2,  12), new Point(9,  12),
			// Central square corners
			new Point(12, 11), new Point(19, 11),
			new Point(12, 18), new Point(19, 18),
			// Inn
			new Point(2,  22), new Point(10, 22),
			// Armory
			new Point(19, 22), new Point(27, 22),
			// Path lamps near well
			new Point(14, 14), new Point(16, 14),
			new Point(14, 16), new Point(16, 16),
	};

	// ══════════════════════════════════════════════════════════════
	//  Music
	// ══════════════════════════════════════════════════════════════

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.end();
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_SEWERS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	// ── Save / Load ─────────────────────────────────────────────

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
	}

	// ── Create — reveal entire village ───────────────────────────

	@Override
	public void create() {
		super.create();
		for (int i = 0; i < length(); i++) {
			visited[i] = true;
			mapped[i]  = true;
		}
	}

	// ══════════════════════════════════════════════════════════════
	//  Build
	// ══════════════════════════════════════════════════════════════

	@Override
	protected boolean build() {
		setSize( WIDTH, HEIGHT );

		// ── 1. Base: fill everything with high grass (forest) ─────
		Painter.fill( this, 0, 0, WIDTH, HEIGHT, Terrain.HIGH_GRASS );

		// Border walls (dense tree line at edges)
		Painter.fill( this, 0, 0, WIDTH, 1, Terrain.CHASM ); // top
		Painter.fill( this, 0, HEIGHT - 1, WIDTH, 1, Terrain.CHASM ); // bottom
		Painter.fill( this, 0, 0, 1, HEIGHT, Terrain.CHASM );
		Painter.fill( this, WIDTH - 1, 0, 1, HEIGHT, Terrain.WALL );

		// ── 2. Pond (top-left) ───────────────────────────────────
		// Stone border then water inside
		Painter.fill( this, pondOuter, Terrain.EMPTY );
		Painter.fill( this, pondWater, Terrain.WATER );
		// Scatter a few grass tiles along the pond edge for a natural look

		// Painter.set( this, 10, 7, Terrain.GRASS );

		// ── 3. Kitchen / Tavern (top-right) ──────────────────────
		buildBuilding( kitchenBuilding );
		// Interior: shelves along top wall, worktable in center
		Painter.set( this, 21, 3, Terrain.BOOKSHELF );
		Painter.set( this, 22, 3, Terrain.BOOKSHELF );
		Painter.set( this, 23, 3, Terrain.BOOKSHELF );
		Painter.set( this, 24, 3, Terrain.BOOKSHELF );
		Painter.set( this, 25, 3, Terrain.BOOKSHELF );
		Painter.set( this, 26, 3, Terrain.BOOKSHELF );
		// Barrels / crates (use BARRICADE for crate-like objects)
		Painter.set( this, 26, 5, Terrain.BARRICADE );
		Painter.set( this, 26, 7, Terrain.BARRICADE );

		// ── 4. Blacksmith / Forge (middle-left) ──────────────────
		buildBuilding( forgeBuilding );
		// Forge fire in center
		Painter.set( this, 5, 15, Terrain.EMBERS );
		Painter.set( this, 6, 15, Terrain.EMBERS );
		// Weapon racks on walls (bookshelves as stand-in)
		Painter.set( this, 3, 13, Terrain.BOOKSHELF );
		Painter.set( this, 4, 13, Terrain.BOOKSHELF );
		Painter.set( this, 5, 13, Terrain.BOOKSHELF );
		Painter.set( this, 7, 13, Terrain.BOOKSHELF );
		Painter.set( this, 8, 13, Terrain.BOOKSHELF );
		// Anvil (use STATUE terrain or EMPTY_DECO as stand-in)
		Painter.set( this, 5, 17, Terrain.STATUE );

		// ── 5. Central square + well ─────────────────────────────
		Painter.fill( this, centralSquare, Terrain.EMPTY_SP );
		Painter.set( this, wellPos, Terrain.WELL );
		// Decorative cobblestone border around the square
		for (int x = centralSquare.left; x < centralSquare.right; x++) {
			Painter.set( this, x, centralSquare.top,        Terrain.EMPTY_DECO );
			Painter.set( this, x, centralSquare.bottom - 1, Terrain.EMPTY_DECO );
		}
		for (int y = centralSquare.top; y < centralSquare.bottom; y++) {
			Painter.set( this, centralSquare.left,      y, Terrain.EMPTY_DECO );
			Painter.set( this, centralSquare.right - 1, y, Terrain.EMPTY_DECO );
		}

		// ── 6. Inn (bottom-left) ─────────────────────────────────
		buildBuilding( innBuilding );
		// Beds along the left wall
		Painter.set( this, 3, 23, Terrain.EMPTY_DECO );
		Painter.set( this, 3, 24, Terrain.EMPTY_DECO );
		Painter.set( this, 3, 26, Terrain.EMPTY_DECO );
		Painter.set( this, 3, 27, Terrain.EMPTY_DECO );
		// Bookshelf + desk along the top
		Painter.set( this, 6, 23, Terrain.BOOKSHELF );
		Painter.set( this, 7, 23, Terrain.BOOKSHELF );
		Painter.set( this, 8, 23, Terrain.BOOKSHELF );
		Painter.set( this, 9, 23, Terrain.BOOKSHELF );

		// ── 7. Armory / Shop (bottom-right) ──────────────────────
		buildBuilding( armoryBuilding );
		// Armor stands (statues)
		Painter.set( this, 22, 24, Terrain.STATUE );
		Painter.set( this, 24, 24, Terrain.STATUE );
		Painter.set( this, 26, 24, Terrain.STATUE );
		// Shelves along right wall
		Painter.set( this, 26, 23, Terrain.BOOKSHELF );
		Painter.set( this, 26, 26, Terrain.BOOKSHELF );
		Painter.set( this, 26, 27, Terrain.BOOKSHELF );
		// Barrels
		Painter.set( this, 20, 27, Terrain.BARRICADE );
		Painter.set( this, 21, 27, Terrain.BARRICADE );

		// ── 8. Dirt paths ────────────────────────────────────────
		// Main vertical path (top to bottom through center)
		Painter.fill( this, 15, 1,  1, 11, Terrain.EMPTY_SP );  // top to square
		Painter.fill( this, 15, 18, 1, 13, Terrain.EMPTY_SP );  // square to bottom
		// Widen the main vertical path
		Painter.fill( this, 14, 1,  1, 11, Terrain.EMPTY_SP );
		Painter.fill( this, 14, 18, 1, 13, Terrain.EMPTY_SP );

		// Main horizontal path (left to right through center)
		Painter.fill( this, 1,  15, 12, 1, Terrain.EMPTY_SP );  // left to square
		Painter.fill( this, 19, 15, 12, 1, Terrain.EMPTY_SP );  // square to right
		// Widen the horizontal path
		Painter.fill( this, 1,  14, 12, 1, Terrain.EMPTY_SP );
		Painter.fill( this, 19, 14, 12, 1, Terrain.EMPTY_SP );

		// Path from pond down to horizontal path
		Painter.fill( this, 7,  8,  1, 7,  Terrain.EMPTY_SP );

		// Path from kitchen left to vertical path
		Painter.fill( this, 14, 5,  6, 1,  Terrain.EMPTY_SP );

		// Path from forge right to square (already covered by horizontal)

		// Path from inn right to vertical path
		Painter.fill( this, 11, 25, 4, 1,  Terrain.EMPTY_SP );

		// Path from armory left to vertical path
		Painter.fill( this, 14, 25, 6, 1,  Terrain.EMPTY_SP );

		// ── 9. Doors — placed AFTER paths ────────────────────────
		Painter.set( this, kitchenDoor, Terrain.DOOR );
		Painter.set( this, forgeDoor,   Terrain.DOOR );
		Painter.set( this, innDoor,     Terrain.DOOR );
		Painter.set( this, armoryDoor,  Terrain.DOOR );

		// ── 10. Dungeon entrance ─────────────────────────────────
		Painter.set( this, DUNGEON_ENTRANCE_POS, Terrain.EXIT );

		LevelTransition exit = new LevelTransition(
				this,
				DUNGEON_ENTRANCE_POS,
				LevelTransition.Type.REGULAR_EXIT );
		exit.destDepth  = 1;
		exit.destBranch = 0;
		exit.destType   = LevelTransition.Type.REGULAR_ENTRANCE;
		transitions.add( exit );

		// ── 11. Scatter some regular grass for variety ────────────
		for (int i = 0; i < 80; i++) {
			int cell = Random.Int( length() );
			if (map[cell] == Terrain.HIGH_GRASS && !isEdge( cell )) {
				map[cell] = Terrain.GRASS;
			}
		}

		// ── 12. Torches ──────────────────────────────────────────
		for (Point p : torchPositions) {
			Painter.set( this, p, Terrain.WALL_DECO );
		}

		return true;
	}

	/** Builds walls and empty interior. Doors placed separately. */
	private void buildBuilding( Rect bounds ) {
		Painter.fill( this, bounds, Terrain.WALL );
		Painter.fill( this, bounds, 1, Terrain.EMPTY_SP );
	}

	private boolean isEdge( int cell ) {
		int x = cell % WIDTH;
		int y = cell / WIDTH;
		return x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1;
	}

	// ── Mobs ─────────────────────────────────────────────────────

	@Override
	protected void createMobs() {
		Innkeeper innkeeper = new Innkeeper();
		innkeeper.pos = pointToCell( innkeeperPos );
		mobs.add( innkeeper );
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	// ── Items ────────────────────────────────────────────────────

	@Override
	protected void createItems() {
	}

	// ── Respawn ──────────────────────────────────────────────────

	@Override
	public int randomRespawnCell( Char ch ) {
		int center = pointToCell( wellPos );
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = center + i;
			if (passable[cell]
					&& Actor.findChar( cell ) == null
					&& (!Char.hasProp( ch, Char.Property.LARGE ) || openSpace[cell])) {
				candidates.add( cell );
			}
		}
		if (candidates.isEmpty()) return -1;
		return Random.element( candidates );
	}

	// ── Visuals ──────────────────────────────────────────────────

	@Override
	public Group addVisuals() {
		super.addVisuals();
		return visuals;
	}

	// ── Tile descriptions ────────────────────────────────────────

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get( VillageLevel.class, "water_name" );
			case Terrain.HIGH_GRASS:
				return Messages.get( VillageLevel.class, "high_grass_name" );
			case Terrain.WELL:
				return Messages.get( VillageLevel.class, "well_name" );
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.ENTRANCE:
				return Messages.get( VillageLevel.class, "entrance_desc" );
			case Terrain.EXIT:
				return Messages.get( VillageLevel.class, "exit_desc" );
			case Terrain.EMPTY_DECO:
				return Messages.get( VillageLevel.class, "empty_deco_desc" );
			case Terrain.BOOKSHELF:
				return Messages.get( VillageLevel.class, "bookshelf_desc" );
			case Terrain.WELL:
				return Messages.get( VillageLevel.class, "well_desc" );
			case Terrain.HIGH_GRASS:
				return Messages.get( VillageLevel.class, "high_grass_desc" );
			case Terrain.EMBERS:
				return Messages.get( VillageLevel.class, "embers_desc" );
			case Terrain.STATUE:
				return Messages.get( VillageLevel.class, "statue_desc" );
			case Terrain.BARRICADE:
				return Messages.get( VillageLevel.class, "barricade_desc" );
			default:
				return super.tileDesc( tile );
		}
	}

}