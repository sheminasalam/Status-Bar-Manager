export type TimeDisplayOption = 'Show hours and minutes' | 'Show hours, minutes and seconds' | 'Do not show';

export interface StatusBarSettings {
  timeDisplay: TimeDisplayOption;
  realTimeNetworkSpeed: boolean;
  batteryPercentage: boolean; // separate setting: show battery percentage on status bar (never touched)
  batteryIcon: boolean; // actual battery icon visibility
  statusIcons: Record<string, boolean>; // Comprehensive dynamic map including VoLTE, VoNR, 5G, Wi-Fi, etc.
  hasWiredHeadphonesConnected: boolean; // user simulation toggle to test wired 3.5mm jack
}

export interface BackupData {
  id: string;
  timestamp: number;
  timeDisplay: TimeDisplayOption;
  realTimeNetworkSpeed: boolean;
  statusIcons: Record<string, boolean>;
  deviceModel: string;
  manufacturer: string;
  androidVersion: number;
  isHideActive: boolean;
  isPartial: boolean;
  headphoneStatus: string;
}

export type AutomationStepId =
  | 'idle'
  | 'opening_settings'
  | 'navigating_notifications'
  | 'navigating_status_bar'
  | 'backing_up'
  | 'changing_time'
  | 'toggling_speed'
  | 'navigating_icons'
  | 'discovering_all_icons'
  | 'toggling_icons'
  | 'checking_headphone'
  | 'verifying'
  | 'completed'
  | 'partial'
  | 'failed';

export interface IconStats {
  discovered: number;
  disabled: number;
  alreadyDisabled: number;
  restored: number;
  remaining: number;
}

export interface ProcessChecklistItem {
  id: string;
  title: string;
  status: 'pending' | 'in_progress' | 'completed' | 'partial' | 'failed';
  details?: string;
}

export interface AutomationStep {
  id: AutomationStepId;
  label: string;
  detail: string;
}

export type DeviceOem = 'color_os' | 'one_ui' | 'hyper_os' | 'pixel';

export interface CodeFile {
  path: string;
  name: string;
  language: 'kotlin' | 'xml' | 'gradle' | 'properties' | 'markdown';
  category: 'Kotlin Source' | 'Manifest & Resources' | 'Gradle Build';
  content: string;
}
