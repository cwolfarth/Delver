# Delver

A single-player, turn-based dungeon crawler vertical slice built with Angular, Spring Boot, and PostgreSQL.

## Play locally

Requirements: Java 27, Node.js 22.12+ (or 24+), npm, and PostgreSQL 17.

Start PostgreSQL with Docker Compose:

```sh
docker compose up -d database
```

In one terminal, start the API from the project root:

```sh
./gradlew bootRun
```

On Windows, use `gradlew.bat bootRun`.

In another terminal, start the Angular client:

```sh
cd frontend
npm install
npm start
```

Open <http://localhost:4200>. The development server proxies `/api` requests to Spring Boot on port 8080.

To run the full stack in containers, use `docker compose up --build` and open <http://localhost:4200>. Set `POSTGRES_PASSWORD` in the environment before launching outside local development.

## How to play

- Move with **WASD** or the **arrow keys**. Each attempted move advances a turn; enemies act after you.
- Walk into enemies to attack. Collect gold, potions, and gear by moving over them. Gear is an occasional drop, with common items appearing most often and legendary items exceptionally rarely; weapons and armor show their rarity and sell value.
- Enemy types get stronger as you descend: a new type joins the possible spawns every three floors, beginning on floor 3.
- Each floor varies between four and six rooms, with randomized room placement, spawn, and stairs. Room and corridor shapes remain visible through muted fog; entering a corridor reveals only its segment, not other branches. Explored terrain remains mapped as you move on.
- Your five-slot inventory holds weapons, armor, and individual health potions. Items stay on the ground when the inventory is full. Use the item controls to equip gear, drink a potion, or discard an item; each action advances a turn.
- Press **H** or use the potion button to drink a potion from your inventory. Move onto the stairs to descend.
- Death ends the run and records a score in the Hall of the Fallen.

## Project boundaries

- `frontend/src/app`: API client, game screen, input and presentation.
- `src/main/java/.../game`: `DungeonGenerator` builds floors, `GameSnapshotFactory` applies fog-of-war and projects API views, and `GameService` coordinates turns and combat.
- `src/main/java/.../game/content`: enemy, loot, weapon, and armor types with their domain stats.
- `src/main/java/.../game/map`: map tile types used by generation and API snapshots.
- `src/main/java/.../score`: high-score persistence.
- `src/main/resources/schema.sql`: database schema initialized on startup.

Active run state is currently held in backend memory and is lost when the API restarts. High scores are durable in PostgreSQL. Enemy, weapon, and armor enums own their display names and combat bonuses; gear enums also define rarity and sell value. Each floor guarantees one gear item, with a chance for one additional drop based on floor depth; rarity is weighted toward common drops. The game domain and API models remain separate from the Angular presentation so more content can be added without coupling rules to the UI.
