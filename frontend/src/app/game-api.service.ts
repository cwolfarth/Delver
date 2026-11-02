import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type Action = 'MOVE_NORTH' | 'MOVE_SOUTH' | 'MOVE_EAST' | 'MOVE_WEST' | 'USE_POTION' | 'EQUIP_ITEM' | 'DISCARD_ITEM' | 'WAIT';
export type TileType = 'WALL' | 'FLOOR' | 'STAIRS' | 'FOGGED_WALL' | 'FOGGED_FLOOR';
export type ItemRarity = 'COMMON' | 'UNCOMMON' | 'RARE' | 'LEGENDARY';
export type WeaponType = 'WORN_BLADE' | 'IRON_SWORD' | 'STEEL_SWORD' | 'KNIGHT_BLADE' | 'DRAGONFANG';
export type ArmorType =
  | 'CLOTH_ARMOR' | 'CLOTH_HELMET' | 'CLOTH_GAUNTLETS' | 'CLOTH_BOOTS'
  | 'LEATHER_ARMOR' | 'LEATHER_HELMET' | 'LEATHER_GAUNTLETS' | 'LEATHER_BOOTS'
  | 'CHAINMAIL' | 'CHAINMAIL_HELMET' | 'CHAINMAIL_GAUNTLETS' | 'CHAINMAIL_BOOTS'
  | 'PLATE_ARMOR' | 'PLATE_HELMET' | 'PLATE_GAUNTLETS' | 'PLATE_BOOTS'
  | 'DRAGON_SCALE' | 'DRAGON_SCALE_HELMET' | 'DRAGON_SCALE_GAUNTLETS' | 'DRAGON_SCALE_BOOTS';
export type ArmorSlot = 'CHEST' | 'HELMET' | 'GAUNTLETS' | 'BOOTS';
export type EnemyType = 'GOBLIN' | 'SKELETON' | 'ORC' | 'TROLL' | 'WRAITH' | 'DEMON';

export interface Weapon {
  type: WeaponType;
  name: string;
  bonus: number;
  accuracy: number;
  rarity: ItemRarity;
  sellValue: number;
}

export interface Armor {
  type: ArmorType;
  name: string;
  slot: ArmorSlot;
  bonus: number;
  rarity: ItemRarity;
  sellValue: number;
}

export interface InventoryItem {
  type: 'POTION' | 'WEAPON' | 'ARMOR';
  name: string;
  bonus: number;
  accuracy: number;
  weaponType: WeaponType | null;
  armorType: ArmorType | null;
  armorSlot: ArmorSlot | null;
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
    x: number; y: number; hp: number; maxHp: number; gold: number; attack: number; defense: number;
    equippedWeapon: Weapon; equippedArmor: Armor; equippedHelmet: Armor | null;
    equippedGauntlets: Armor | null; equippedBoots: Armor | null;
    inventory: InventoryItem[]; inventoryCapacity: number;
  };
  enemies: { type: EnemyType; name: string; x: number; y: number; hp: number }[];
  loot: { type: string; name: string; x: number; y: number; amount: number; weaponType: WeaponType | null; armorType: ArmorType | null; armorSlot: ArmorSlot | null; accuracy: number | null; rarity: ItemRarity | null; sellValue: number }[];
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
