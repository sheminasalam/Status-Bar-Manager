export type TimeDisplayOption = 'Show hours and minutes' | 'Show hours, minutes and seconds' | 'Do not show';

export interface StatusBarSettings {
  timeDisplay: TimeDisplayOption;
  realTimeNetworkSpeed: boolean;
  batteryPercentage: boolean; // separate setting: show battery percentage on status bar
  batteryIcon: boolean; // actual battery icon visibility if supported
  statusIcons: {
    mobileData: boolean;
    wifi: boolean;
    bluetooth: boolean;
    alarm: boolean;
    nfc: boolean;
    earphones: boolean;
    hotspot: boolean;
    vpn: boolean;
    dataSaving: boolean;
    highPerformance: boolean;
  };
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
  | 'toggling_icons'
  | 'verifying'
  | 'completed'
  | 'failed';

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
