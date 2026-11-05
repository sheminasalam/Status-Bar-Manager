import React from 'react';
import {
  EyeOff,
  CheckCircle2,
  AlertTriangle,
  Smartphone,
  RotateCcw,
  Sliders,
  Settings as SettingsIcon,
  HelpCircle,
  ShieldCheck,
  ChevronRight,
  Database
} from 'lucide-react';
import { BackupData, StatusBarSettings, AutomationStepId, DeviceOem } from '../types';

interface StatusBarManagerAppViewProps {
  isHideActive: boolean;
  hasBackup: boolean;
  backupData: BackupData | null;
  isAccessibilityGranted: boolean;
  currentStepId: AutomationStepId;
  stepMessage: string;
  stepIndex: number;
  totalSteps: number;
  selectedOem: DeviceOem;
  onToggleMaster: (enabled: boolean) => void;
  onRestoreOriginal: () => void;
  onRequestAccessibility: () => void;
  onOpenSettingsApp: () => void;
  onOpenBackupModal: () => void;
}

export const StatusBarManagerAppView: React.FC<StatusBarManagerAppViewProps> = ({
  isHideActive,
  hasBackup,
  backupData,
  isAccessibilityGranted,
  currentStepId,
  stepMessage,
  stepIndex,
  totalSteps,
  selectedOem,
  onToggleMaster,
  onRestoreOriginal,
  onRequestAccessibility,
  onOpenSettingsApp,
  onOpenBackupModal
}) => {
  const isRunning =
    currentStepId !== 'idle' &&
    currentStepId !== 'completed' &&
    currentStepId !== 'failed';

  // OEM display names
  const oemLabels: Record<DeviceOem, string> = {
    color_os: 'ColorOS / OxygenOS (Target Hardware)',
    one_ui: 'Samsung One UI',
    hyper_os: 'Xiaomi HyperOS / MIUI',
    pixel: 'Google Pixel (AOSP)'
  };

  return (
    <div className="flex flex-col h-full bg-[#0A0E17] text-white overflow-y-auto p-4 select-none">
      {/* Top Bar */}
      <div className="flex items-center justify-between pb-3 mb-2 border-b border-gray-800">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
            <EyeOff className="w-4 h-4 text-white" />
          </div>
          <div>
            <h1 className="text-base font-bold text-white tracking-tight leading-tight">
              StatusBar Manager
            </h1>
            <p className="text-[10px] text-gray-400">
              Control the visibility of your Android status bar.
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-3.5 pb-6">
        {/* Permission Required Banner */}
        {!isAccessibilityGranted && (
          <div className="p-3 rounded-xl bg-red-950/50 border border-red-500/30 text-red-200 text-xs">
            <div className="flex items-center space-x-2 mb-1.5">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
              <span className="font-semibold text-white">Accessibility Permission Required</span>
            </div>
            <p className="text-[11px] text-red-300 leading-relaxed mb-2">
              StatusBar Manager needs Accessibility Service permission to automate system settings without root.
            </p>
            <button
              onClick={onRequestAccessibility}
              className="w-full py-1.5 px-3 bg-red-600 hover:bg-red-500 active:scale-95 text-white font-medium text-xs rounded-lg transition-all"
            >
              Open Accessibility Settings
            </button>
          </div>
        )}

        {/* Master ON/OFF Switch Card */}
        <div
          className={`p-4 rounded-2xl bg-[#111827] border transition-all duration-300 ${
            isHideActive
              ? 'border-blue-500/70 shadow-lg shadow-blue-500/10'
              : 'border-gray-800'
          }`}
        >
          <div className="flex items-center justify-between">
            <div className="pr-3">
              <h2 className="text-sm font-bold text-white">Hide Status Bar Information</h2>
              <p className="text-xs text-gray-400 mt-0.5 leading-snug">
                {isHideActive
                  ? 'Time, network speed, and all system status icons are currently hidden.'
                  : 'Hides clock, real-time speed, Wi-Fi, 5G, VoLTE, VoNR, and status icons across the entire phone.'}
              </p>
            </div>

            {/* Custom Material Switch */}
            <button
              onClick={() => {
                if (!isAccessibilityGranted) {
                  onRequestAccessibility();
                  return;
                }
                onToggleMaster(!isHideActive);
              }}
              disabled={isRunning}
              className={`relative inline-flex h-7 w-12 shrink-0 cursor-pointer rounded-full transition-colors duration-200 ease-in-out focus:outline-none ${
                isHideActive ? 'bg-blue-600' : 'bg-gray-700'
              } ${isRunning ? 'opacity-60 cursor-not-allowed' : ''}`}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out mt-1 ${
                  isHideActive ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {/* Determinate Horizontal Progress Bar */}
          {isRunning && (
            <div className="mt-3 space-y-1.5">
              <div className="flex items-center justify-between text-[11px]">
                <span className="text-blue-300 font-medium">Automating Settings...</span>
                <span className="font-mono font-bold text-blue-400">
                  {Math.round((stepIndex / totalSteps) * 100)}%
                </span>
              </div>
              <div className="w-full bg-gray-800 h-2 rounded-full overflow-hidden">
                <div
                  className="bg-gradient-to-r from-blue-500 to-indigo-500 h-2 rounded-full transition-all duration-300 ease-out"
                  style={{ width: `${Math.max(8, (stepIndex / totalSteps) * 100)}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* System Status & Headphone State Pills */}
        <div className="p-3.5 rounded-xl bg-[#111827] border border-gray-800 space-y-2">
          <div className="text-xs font-semibold text-gray-300">System Status &amp; Policy</div>
          <div className="grid grid-cols-2 gap-2">
            {/* Status State */}
            <div className="bg-[#1F2937] p-2.5 rounded-lg">
              <span className="text-[10px] text-gray-400 uppercase font-medium block">
                Status Bar
              </span>
              <div className="flex items-center space-x-1.5 mt-1">
                <div
                  className={`w-2 h-2 rounded-full ${
                    isHideActive
                      ? 'bg-blue-500 animate-pulse'
                      : hasBackup
                      ? 'bg-amber-400'
                      : 'bg-emerald-400'
                  }`}
                />
                <span className="text-xs font-semibold text-white">
                  {isHideActive ? 'Hidden (Partial)' : hasBackup ? 'Partially hidden' : 'Normal'}
                </span>
              </div>
            </div>

            {/* Accessibility State */}
            <div className="bg-[#1F2937] p-2.5 rounded-lg">
              <span className="text-[10px] text-gray-400 uppercase font-medium block">
                Accessibility
              </span>
              <div className="flex items-center space-x-1.5 mt-1">
                <div
                  className={`w-2 h-2 rounded-full ${
                    isAccessibilityGranted ? 'bg-emerald-400' : 'bg-red-500'
                  }`}
                />
                <span className="text-xs font-semibold text-white">
                  {isAccessibilityGranted ? 'Active' : 'Disabled'}
                </span>
              </div>
            </div>
          </div>

          {/* Wired Headphone Indicator Finding (Objective 2) */}
          <div className="p-2.5 rounded-lg bg-[#182030] border border-blue-900/40 text-[11px] text-gray-300 space-y-1">
            <div className="flex items-center justify-between">
              <span className="font-semibold text-white flex items-center space-x-1.5">
                <span>Wired Headphone Indicator</span>
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-950 text-amber-300 border border-amber-600/30 font-semibold">
                System-managed
              </span>
            </div>
            <p className="text-[10px] text-gray-400 leading-relaxed">
              No user-facing visibility toggle in Settings. Handled internally by SystemUI / AudioService.
            </p>
          </div>
        </div>

        {/* Live Process Checklist & Icon Statistics (Objective 3) */}
        {(isRunning || currentStepId !== 'idle') && (
          <div className="p-3.5 rounded-xl bg-blue-950/40 border border-blue-500/30 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                {currentStepId === 'completed' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                ) : currentStepId === 'failed' ? (
                  <AlertTriangle className="w-4 h-4 text-red-400" />
                ) : (
                  <Sliders className="w-4 h-4 text-blue-400 animate-spin" />
                )}
                <span className="text-xs font-bold text-white">
                  {currentStepId === 'completed'
                    ? 'Operation Completed (Partial: Wired Headphone System-Managed)'
                    : currentStepId === 'failed'
                    ? 'Operation Failed'
                    : 'Automating Settings Live'}
                </span>
              </div>
              <span className="text-[10px] font-mono text-blue-300">
                Step {stepIndex}/{totalSteps}
              </span>
            </div>

            <p className="text-xs text-blue-100">{stepMessage}</p>

            {/* Live Checklist */}
            <div className="space-y-1.5 pt-1 border-t border-blue-900/50 text-[11px]">
              <div className="flex items-center justify-between py-1 border-b border-blue-900/30">
                <span className="text-gray-300">1. Open Settings &amp; Navigate to Status bar</span>
                <span className={`text-[10px] font-semibold ${stepIndex >= 3 ? 'text-emerald-400' : stepIndex >= 1 ? 'text-amber-300' : 'text-gray-500'}`}>
                  {stepIndex >= 3 ? 'Completed' : stepIndex >= 1 ? 'In Progress' : 'Pending'}
                </span>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-blue-900/30">
                <span className="text-gray-300">2. Time display configuration</span>
                <span className={`text-[10px] font-semibold ${stepIndex >= 4 ? 'text-emerald-400' : stepIndex === 4 ? 'text-amber-300' : 'text-gray-500'}`}>
                  {stepIndex >= 4 ? 'Completed' : stepIndex === 4 ? 'In Progress' : 'Pending'}
                </span>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-blue-900/30">
                <span className="text-gray-300">3. Real-time network speed</span>
                <span className={`text-[10px] font-semibold ${stepIndex >= 5 ? 'text-emerald-400' : stepIndex === 5 ? 'text-amber-300' : 'text-gray-500'}`}>
                  {stepIndex >= 5 ? 'Completed' : stepIndex === 5 ? 'In Progress' : 'Pending'}
                </span>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-blue-900/30">
                <span className="text-gray-300">4. Dynamic status icons discovery &amp; toggle</span>
                <span className={`text-[10px] font-semibold ${stepIndex >= 6 ? 'text-emerald-400' : stepIndex === 6 ? 'text-amber-300' : 'text-gray-500'}`}>
                  {stepIndex >= 6 ? 'Completed' : stepIndex === 6 ? 'In Progress' : 'Pending'}
                </span>
              </div>
              <div className="flex items-center justify-between py-1">
                <span className="text-gray-300">5. Headphone inspection &amp; verification</span>
                <span className={`text-[10px] font-semibold ${stepIndex >= 7 ? 'text-emerald-400' : stepIndex === 7 ? 'text-amber-300' : 'text-gray-500'}`}>
                  {stepIndex >= 7 ? 'Verified' : 'Pending'}
                </span>
              </div>
            </div>

            {/* Live Icon Statistics Box */}
            <div className="p-2.5 rounded-lg bg-[#0F172A] border border-blue-800/40">
              <div className="text-[10px] font-bold text-blue-300 uppercase tracking-wider mb-1.5">
                Live Icon Statistics
              </div>
              <div className="grid grid-cols-4 gap-1 text-center">
                <div className="bg-gray-800/80 p-1.5 rounded">
                  <div className="text-[9px] text-gray-400">Found</div>
                  <div className="text-xs font-bold text-white">8</div>
                </div>
                <div className="bg-gray-800/80 p-1.5 rounded">
                  <div className="text-[9px] text-gray-400">Disabled</div>
                  <div className="text-xs font-bold text-emerald-400">
                    {stepIndex >= 6 ? (isHideActive ? '6' : '0') : '0'}
                  </div>
                </div>
                <div className="bg-gray-800/80 p-1.5 rounded">
                  <div className="text-[9px] text-gray-400">Already Off</div>
                  <div className="text-xs font-bold text-amber-300">2</div>
                </div>
                <div className="bg-gray-800/80 p-1.5 rounded">
                  <div className="text-[9px] text-gray-400">Left</div>
                  <div className="text-xs font-bold text-blue-400">
                    {stepIndex >= 6 ? '0' : '8'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Original Settings Backup Card */}
        <div className="p-3.5 rounded-xl bg-[#111827] border border-gray-800">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-xs font-semibold text-gray-200">
              Original Settings Baseline
            </span>
            {hasBackup && (
              <button
                onClick={onOpenBackupModal}
                className="text-[11px] text-blue-400 hover:text-blue-300 font-medium flex items-center space-x-1"
              >
                <Database className="w-3 h-3" />
                <span>View Baseline</span>
              </button>
            )}
          </div>
          <p className="text-xs text-gray-400 leading-snug">
            {hasBackup
              ? 'Exact original baseline saved in DataStore. Restores actual prior Time string and leaves untouched icons off.'
              : 'No active backup. Baseline will be saved automatically before modifying any setting.'}
          </p>

          <button
            onClick={onRestoreOriginal}
            disabled={isRunning || !hasBackup}
            className={`w-full mt-3 py-2 px-3 rounded-lg flex items-center justify-center space-x-2 text-xs font-semibold transition-all ${
              hasBackup && !isRunning
                ? 'bg-[#1F2937] hover:bg-gray-700 text-white active:scale-98'
                : 'bg-gray-800/50 text-gray-500 cursor-not-allowed'
            }`}
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Restore Original Settings</span>
          </button>
        </div>

        {/* Device Compatibility Card */}
        <div className="p-3.5 rounded-xl bg-[#111827] border border-gray-800">
          <div className="flex items-center space-x-2 text-xs font-semibold text-gray-200 mb-1">
            <Smartphone className="w-3.5 h-3.5 text-blue-400" />
            <span>Device Profile & Compatibility</span>
          </div>
          <p className="text-xs font-medium text-white">{oemLabels[selectedOem]}</p>
          <p className="text-[11px] text-gray-400 mt-0.5">
            Automates: Settings → Notifications & status bar → Status bar
          </p>
        </div>

        {/* Manual Assistance Card */}
        <div className="p-3.5 rounded-xl bg-[#111827] border border-gray-800">
          <span className="text-xs font-semibold text-gray-200 block mb-1">
            Manual Assistance
          </span>
          <p className="text-[11px] text-gray-400 mb-2.5">
            If a manufacturer UI variant requires manual assistance, you can jump directly into Settings.
          </p>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={onOpenSettingsApp}
              className="py-1.5 px-2 bg-[#1F2937] hover:bg-gray-700 text-white rounded-lg text-xs font-medium flex items-center justify-center space-x-1.5"
            >
              <SettingsIcon className="w-3 h-3 text-gray-300" />
              <span>Open Settings</span>
            </button>
            <button
              onClick={onRequestAccessibility}
              className="py-1.5 px-2 bg-[#1F2937] hover:bg-gray-700 text-white rounded-lg text-xs font-medium flex items-center justify-center space-x-1.5"
            >
              <ShieldCheck className="w-3 h-3 text-gray-300" />
              <span>Accessibility</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
