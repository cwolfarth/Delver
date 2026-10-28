import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type Action = 'MOVE_NORTH' | 'MOVE_SOUTH' | 'MOVE_EAST' | 'MOVE_WEST' | 'USE_POTION' | 'EQUIP_ITEM' | 'DISCARD_ITEM' | 'WAIT';
export type TileType = 'WALL' | 'FLOOR' | 'STAIRS' | 'FOGGED_WALL' | 'FOGGED_FLOOR';
export type ItemRarity = 'COMMON' | 'UNCOMMON' | 'RARE' | 'LEGENDARY';
export type WeaponType = 'WORN_BLADE' | 'IRON_SWORD' | 'STEEL_SWORD' | 'KNIGHT_BLADE' | 'DRAGONFANG';
export type ArmorType = 'CLOTH_ARMOR' | 'LEATHER_ARMOR' | 'CHAINMAIL' | 'PLATE_ARMOR' | 'DRAGON_SCALE';
export type EnemyType = 'GOBLIN' | 'SKELETON' | 'ORC' | 'TROLL' | 'WRAITH' | 'DEMON';

export interface Weapon {
  type: WeaponType;
  name: string;
  bonus: number;
  rarity: ItemRarity;
  sellValue: number;
}

export interface Armor {
  type: ArmorType;
  name: string;
  bonus: number;
  rarity: ItemRarity;
  sellValue: number;
}

export interface InventoryItem {
  type: 'POTION' | 'WEAPON' | 'ARMOR';
  name: string;
  bonus: number;
  weaponType: WeaponType | null;
  armorType: ArmorType | null;
  rarity: ItemRarity | null;
  sellValue: number;
}

export interface GameSnapshot {
  runId: string;
  playerName: string;
  floor: number;
  turn: number;
  width: number;
  height: number;
  map: TileType[][];
  player: {
    x: number; y: number; hp: number; maxHp: number; gold: number; attack: number;
    equippedWeapon: Weapon; equippedArmor: Armor; inventory: InventoryItem[]; inventoryCapacity: number;
  };
  enemies: { type: EnemyType; name: string; x: number; y: number; hp: number }[];
  loot: { type: string; name: string; x: number; y: number; amount: number; weaponType: WeaponType | null; armorType: ArmorType | null; rarity: ItemRarity | null; sellValue: number }[];
  messages: string[];
  gameOver: boolean;
  score: number;
}

export interface HighScore {
  playerName: string;
  score: number;
  floor: number;
  gold: number;
  achievedAt: string;
}

@Injectable({ providedIn: 'root' })
export class GameApiService {
  private readonly http = inject(HttpClient);

  start(playerName: string): Observable<GameSnapshot> {
    return this.http.post<GameSnapshot>('/api/game/runs', { playerName });
  }

  act(runId: string, action: Action, inventorySlot?: number): Observable<GameSnapshot> {
    return this.http.post<GameSnapshot>(`/api/game/runs/${runId}/actions`, { action, inventorySlot });
  }

  highScores(): Observable<HighScore[]> {
    return this.http.get<HighScore[]>('/api/highscores');
  }
}
