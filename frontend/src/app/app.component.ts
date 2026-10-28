import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core';
import { finalize, timeout } from 'rxjs';
import { GameApiService, Action, GameSnapshot, HighScore, InventoryItem } from './game-api.service';

interface BoardCell {
  key: string;
  className: string;
  marker: string;
  label: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly api = inject(GameApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  run: GameSnapshot | null = null;
  board: BoardCell[][] = [];
  scores: HighScore[] = [];
  playerName = '';
  busy = false;
  error = '';
  scoreError = '';

  ngOnInit(): void {
    this.loadScores();
  }

  startRun(): void {
    if (this.busy) return;
    this.busy = true;
    this.error = '';
    this.api.start(this.playerName).pipe(
      timeout(15000),
      finalize(() => {
        this.busy = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: (run) => this.applySnapshot(run),
      error: (error: unknown) => { this.error = this.errorMessage(error); }
    });
  }

  onNameInput(event: Event): void {
    this.playerName = (event.target as HTMLInputElement).value;
  }

  act(action: Action, inventorySlot?: number): void {
    if (!this.run || this.run.gameOver || this.busy) return;
    this.busy = true;
    this.error = '';
    this.api.act(this.run.runId, action, inventorySlot).pipe(
      timeout(15000),
      finalize(() => {
        this.busy = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: (run) => {
        this.applySnapshot(run);
        if (run.gameOver) this.loadScores();
      },
      error: (error: unknown) => { this.error = this.errorMessage(error); }
    });
  }

  @HostListener('window:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const target = event.target;
    if (!this.run || this.run.gameOver || event.repeat
      || target instanceof HTMLInputElement
      || target instanceof HTMLTextAreaElement
      || target instanceof HTMLSelectElement
      || target instanceof HTMLButtonElement
      || (target instanceof HTMLElement && target.isContentEditable)) return;
    const actions: Record<string, Action> = {
      ArrowUp: 'MOVE_NORTH', w: 'MOVE_NORTH', W: 'MOVE_NORTH',
      ArrowDown: 'MOVE_SOUTH', s: 'MOVE_SOUTH', S: 'MOVE_SOUTH',
      ArrowRight: 'MOVE_EAST', d: 'MOVE_EAST', D: 'MOVE_EAST',
      ArrowLeft: 'MOVE_WEST', a: 'MOVE_WEST', A: 'MOVE_WEST',
      h: 'USE_POTION', H: 'USE_POTION', ' ': 'WAIT'
    };
    const action = actions[event.key];
    if (action) {
      event.preventDefault();
      this.act(action);
    }
  }

  hpPercent(): number {
    return this.run ? Math.max(0, (this.run.player.hp / this.run.player.maxHp) * 100) : 0;
  }

  inventorySlots(): number[] {
    return Array.from({ length: this.run?.player.inventoryCapacity ?? 5 }, (_, slot) => slot);
  }

  hasPotion(): boolean {
    return this.run?.player.inventory.some((item) => item.type === 'POTION') ?? false;
  }

  potionCount(): number {
    return this.run?.player.inventory.filter((item) => item.type === 'POTION').length ?? 0;
  }

  inventoryItemDescription(item: InventoryItem): string {
    if (item.type === 'WEAPON') return `Attack +${item.bonus} · ${item.rarity} · ${item.sellValue} gold`;
    if (item.type === 'ARMOR') return `Defense +${item.bonus} · ${item.rarity} · ${item.sellValue} gold`;
    return 'Restores health';
  }

  private loadScores(): void {
    this.api.highScores().subscribe({
      next: (scores) => {
        this.scores = scores;
        this.scoreError = '';
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.scoreError = this.errorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }

  private applySnapshot(run: GameSnapshot): void {
    const enemies = new Map(run.enemies.map((enemy) => [`${enemy.x}:${enemy.y}`, enemy]));
    const loot = new Map(run.loot.map((item) => [`${item.x}:${item.y}`, item]));
    this.board = run.map.map((row, y) => row.map((tile, x) => {
      const key = `${x}:${y}`;
      let marker = '';
      let className = 'tile wall-tile';
      if (run.player.x === x && run.player.y === y) {
        marker = '@';
        className = 'tile player-tile';
      } else {
        const enemy = enemies.get(key);
        const item = loot.get(key);
        if (enemy) {
          marker = enemy.type === 'GOBLIN' ? 'g' : enemy.type[0];
          className = `tile enemy-tile ${enemy.type.toLowerCase()}`;
        } else if (item) {
          marker = item.type === 'GOLD' ? '$' : item.type === 'POTION' ? '!' : item.type === 'ARMOR' ? 'A' : '⚔';
          className = 'tile loot-tile';
        } else if (tile === 'STAIRS') {
          marker = '⇩';
          className = 'tile stairs-tile';
        } else if (tile === 'FOGGED_WALL') {
          className = 'tile fogged-wall-tile';
        } else if (tile === 'FOGGED_FLOOR') {
          className = 'tile fogged-floor-tile';
        } else if (tile === 'FLOOR') {
          className = 'tile floor-tile';
        }
      }
      const label = `${tile}${marker ? `, ${marker}` : ''}`;
      const foggedLabel = tile === 'FOGGED_FLOOR' ? 'Unseen floor' : tile === 'FOGGED_WALL' ? 'Unseen wall' : label;
      return { key, className, marker, label: foggedLabel };
    }));
    this.run = run;
  }

  private errorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'The dungeon server did not respond in time. Please try again.';
    }
    const message = error.error?.message;
    return typeof message === 'string' ? message : 'Could not reach the dungeon server. Check that the API and database are running.';
  }
}
