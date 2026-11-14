// Types for the ThoroughChests global of ModJS server scripts (and global.ThoroughChests in KubeJS).

type ChestLevelId = 'copper' | 'iron' | 'gold' | 'diamond' | 'netherite';
type AugmentId = 'compact' | 'furnace' | 'blast_furnace' | 'magnet' | 'feeder' | 'void';

interface ThoroughChestsEvent {
  type(): string;
  get(key: string): any;
  string(key: string): string;
  number(key: string): number;
  keys(): string[];
}

type ThoroughChestsEventName =
  | 'chest_opened'
  | 'chest_closed'
  | 'chest_upgraded'
  | 'augment_installed'
  | 'augment_removed'
  | 'augment_action'
  ;

interface ThoroughChestsChestInfo {
  level: ChestLevelId;
  slots: number;
  used: number;
  augment: AugmentId | '';
  augmentLevel: number;
  open: number;
  comparator: number;
}

interface ThoroughChestsApi {
  events(): ThoroughChestsEventName[];
  on(event: ThoroughChestsEventName, listener: (event: ThoroughChestsEvent) => void): void;
  addItemRule(allow: (itemId: string) => boolean): void;
  levels(): ChestLevelId[];
  augments(): AugmentId[];
  slots(level: ChestLevelId): number;
  chest(dimension: string, x: number, y: number, z: number): ThoroughChestsChestInfo | null;
  count(dimension: string, x: number, y: number, z: number, itemId: string): number;
  settingKeys(): string[];
  setting(key: string): string;
  setSetting(key: string, value: string): void;
}

declare const ThoroughChests: ThoroughChestsApi;
