import React, { useState, useEffect } from 'react';
import {
  Wifi,
  Bluetooth,
  Bell,
  Battery,
  Shield,
  Headphones,
  Signal,
  ArrowRight,
  Play,
  RotateCcw,
  Sparkles,
  Info
} from 'lucide-react';
import { StatusBarSettings, BackupData, AutomationStepId, DeviceOem } from '../types';
import { StatusBarManagerAppView } from './StatusBarManagerAppView';
import { NativeSettingsView } from './NativeSettingsView';

interface PhoneSimulatorProps {
  settings: StatusBarSettings;
  setSettings: React.Dispatch<React.SetStateAction<StatusBarSettings>>;
  backupData: BackupData | null;
  setBackupData: React.Dispatch<React.SetStateAction<BackupData | null>>;
  isHideActive: boolean;
  setIsHideActive: React.Dispatch<React.SetStateAction<boolean>>;
  isAccessibilityGranted: boolean;
  setIsAccessibilityGranted: React.Dispatch<React.SetStateAction<boolean>>;
  selectedOem: DeviceOem;
  onOpenAccessibilityModal: () => void;
  onOpenBackupModal: () => void;
}

export const PhoneSimulator: React.FC<PhoneSimulatorProps> = ({
  settings,
  setSettings,
  backupData,
  setBackupData,
  isHideActive,
  setIsHideActive,
  isAccessibilityGranted,
  setIsAccessibilityGranted,
  selectedOem,
  onOpenAccessibilityModal,
  onOpenBackupModal
}) => {
  const [activeScreen, setActiveScreen] = useState<'app' | 'settings'>('app');
  const [currentStepId, setCurrentStepId] = useState<AutomationStepId>('idle');
  const [stepMessage, setStepMessage] = useState<string>('Ready to configure');
  const [stepIndex, setStepIndex] = useState<number>(0);
  const totalSteps = 7;
  const [highlightTarget, setHighlightTarget] = useState<string | null>(null);

  // Time formatter for status bar
  const formatTime = () => {
    if (settings.timeDisplay === 'Do not show') return null;
    if (settings.timeDisplay === 'Show hours, minutes and seconds') return '09:41:28';
    return '09:41';
  };

  // Automated workflow runner
  const runAutomationWorkflow = async (targetHide: boolean) => {
    if (!isAccessibilityGranted) {
      onOpenAccessibilityModal();
      return;
    }

    // Step 1: Open Settings
    setCurrentStepId('opening_settings');
    setStepMessage('Opening phone Settings...');
    setStepIndex(1);
    setActiveScreen('settings');
    await new Promise((r) => setTimeout(r, 900));

    // Step 2: Navigate to Notifications & status bar
    setCurrentStepId('navigating_notifications');
    setStepMessage('Locating Notifications & status bar...');
    setStepIndex(2);
    setHighlightTarget('status_bar_section');
    await new Promise((r) => setTimeout(r, 900));

    // Step 3: Open Status bar
    setCurrentStepId('navigating_status_bar');
    setStepMessage('Opening Status bar section...');
    setStepIndex(3);
    setHighlightTarget('time');
    await new Promise((r) => setTimeout(r, 800));

    if (targetHide) {
      // Step 4: Backup existing baseline if not active
      if (!backupData) {
        setBackupData({
          id: 'backup_' + Date.now(),
          timestamp: Date.now(),
          timeDisplay: settings.timeDisplay,
          realTimeNetworkSpeed: settings.realTimeNetworkSpeed,
          statusIcons: { ...settings.statusIcons },
          deviceModel: 'OnePlus 12',
          manufacturer: 'OnePlus',
          androidVersion: 15,
          isHideActive: true
        });
      }

      // Step 5: Change Time to "Do not show"
      setCurrentStepId('changing_time');
      setStepMessage('Changing Time to "Do not show"...');
      setStepIndex(4);
      setSettings((prev) => ({ ...prev, timeDisplay: 'Do not show' }));
      await new Promise((r) => setTimeout(r, 800));

      // Step 6: Disable Real-time network speed
      setCurrentStepId('toggling_speed');
      setStepMessage('Disabling Real-time network speed...');
      setStepIndex(5);
      setHighlightTarget('speed');
      setSettings((prev) => ({ ...prev, realTimeNetworkSpeed: false }));
      await new Promise((r) => setTimeout(r, 800));

      // Step 7: Disable all status icons
      setCurrentStepId('toggling_icons');
      setStepMessage('Disabling every available status icon in Status icons submenu...');
      setStepIndex(6);
      setHighlightTarget('status_icons_menu');
      setSettings((prev) => ({
        ...prev,
        batteryIcon: false,
        statusIcons: {
          mobileData: false,
          wifi: false,
          bluetooth: false,
          alarm: false,
          nfc: false,
          earphones: false,
          hotspot: false,
          vpn: false,
          dataSaving: false,
          highPerformance: false
        }
      }));
      await new Promise((r) => setTimeout(r, 900));

      // Step 8: Verify and return to app
      setCurrentStepId('verifying');
      setStepMessage('Verifying applied states from Settings UI...');
      setStepIndex(7);
      setIsHideActive(true);
      await new Promise((r) => setTimeout(r, 800));

      setActiveScreen('app');
      setCurrentStepId('completed');
      setStepMessage('Status bar information hidden successfully across the entire phone.');
      setHighlightTarget(null);
    } else {
      // RESTORE WORKFLOW
      setCurrentStepId('changing_time');
      setStepMessage('Restoring original Time display setting...');
      setStepIndex(4);
      setHighlightTarget('time');
      const origTime = backupData?.timeDisplay || 'Show hours and minutes';
      setSettings((prev) => ({ ...prev, timeDisplay: origTime }));
      await new Promise((r) => setTimeout(r, 800));

      setCurrentStepId('toggling_speed');
      setStepMessage('Restoring original Real-time network speed...');
      setStepIndex(5);
      setHighlightTarget('speed');
      const origSpeed = backupData ? backupData.realTimeNetworkSpeed : true;
      setSettings((prev) => ({ ...prev, realTimeNetworkSpeed: origSpeed }));
      await new Promise((r) => setTimeout(r, 800));

      setCurrentStepId('toggling_icons');
      setStepMessage('Restoring each status icon to its exact recorded state...');
      setStepIndex(6);
      setHighlightTarget('status_icons_menu');
      if (backupData) {
        setSettings((prev) => ({
          ...prev,
          batteryIcon: true,
          statusIcons: {
            mobileData: backupData.statusIcons.mobileData ?? true,
            wifi: backupData.statusIcons.wifi ?? true,
            bluetooth: backupData.statusIcons.bluetooth ?? false,
            alarm: backupData.statusIcons.alarm ?? true,
            nfc: backupData.statusIcons.nfc ?? false,
            earphones: backupData.statusIcons.earphones ?? false,
            hotspot: backupData.statusIcons.hotspot ?? false,
            vpn: backupData.statusIcons.vpn ?? false,
            dataSaving: backupData.statusIcons.dataSaving ?? false,
            highPerformance: backupData.statusIcons.highPerformance ?? false
          }
        }));
      }
      await new Promise((r) => setTimeout(r, 900));

      setCurrentStepId('verifying');
      setStepMessage('Verifying restored configuration in Settings UI...');
      setStepIndex(7);
      setIsHideActive(false);
      setBackupData(null); // Clear backup after confirmed restoration
      await new Promise((r) => setTimeout(r, 800));

      setActiveScreen('app');
      setCurrentStepId('completed');
      setStepMessage('Original configuration restored accurately.');
      setHighlightTarget(null);
    }
  };

  return (
    <div className="flex flex-col xl:flex-row items-center justify-center gap-8 py-4">
      {/* Device Frame */}
      <div className="relative w-[360px] h-[720px] bg-[#000000] rounded-[52px] p-3 shadow-2xl ring-1 ring-white/10 shadow-black/80 flex flex-col justify-between shrink-0">
        {/* Outer Phone Bezel & Screen Container */}
        <div className="relative w-full h-full bg-[#0A0E17] rounded-[42px] overflow-hidden flex flex-col border border-gray-800">
          
          {/* Top Notch / Dynamic Android Status Bar */}
          <div className="w-full bg-black/60 backdrop-blur-md px-5 pt-3 pb-2 flex items-center justify-between text-xs z-30 transition-all select-none">
            {/* Left side: Time & Real-time network speed */}
            <div className="flex items-center space-x-2.5 min-w-[70px]">
              {formatTime() ? (
                <span className="font-semibold tracking-tight text-white transition-opacity duration-300">
                  {formatTime()}
                </span>
              ) : (
                <span className="text-[10px] text-gray-500 italic opacity-40">hidden</span>
              )}

              {settings.realTimeNetworkSpeed && (
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-500/30 transition-all duration-300 animate-pulse">
                  1.2 MB/s
                </span>
              )}
            </div>

            {/* Center: Punch-hole Front Camera */}
            <div className="w-3.5 h-3.5 bg-black rounded-full ring-2 ring-gray-900 mx-auto" />

            {/* Right side: Status Bar Icons */}
            <div className="flex items-center space-x-1.5 text-gray-200 min-w-[110px] justify-end">
              {settings.statusIcons.alarm && (
                <Bell className="w-3 h-3 text-gray-300 transition-opacity duration-200" />
              )}
              {settings.statusIcons.nfc && (
                <Shield className="w-3 h-3 text-orange-400 transition-opacity duration-200" />
              )}
              {settings.statusIcons.earphones && (
                <Headphones className="w-3 h-3 text-pink-400 transition-opacity duration-200" />
              )}
              {settings.statusIcons.bluetooth && (
                <Bluetooth className="w-3 h-3 text-blue-400 transition-opacity duration-200" />
              )}
              {settings.statusIcons.wifi && (
                <Wifi className="w-3 h-3 text-gray-200 transition-opacity duration-200" />
              )}
              {settings.statusIcons.mobileData && (
                <div className="flex items-center space-x-0.5">
                  <Signal className="w-3 h-3 text-gray-200" />
                  <span className="text-[9px] font-bold text-gray-300">5G</span>
                </div>
              )}
              {settings.batteryPercentage && (
                <span className="text-[10px] font-medium text-gray-300 ml-0.5">86%</span>
              )}
              {settings.batteryIcon && (
                <Battery className="w-3.5 h-3.5 text-gray-200 transition-opacity duration-200" />
              )}
              {!settings.batteryIcon && !settings.batteryPercentage && !settings.statusIcons.wifi && !settings.statusIcons.mobileData && (
                <span className="text-[10px] text-gray-500 italic opacity-40">clean</span>
              )}
            </div>
          </div>

          {/* Screen Content Viewport */}
          <div className="flex-1 overflow-hidden relative">
            {activeScreen === 'app' ? (
              <StatusBarManagerAppView
                isHideActive={isHideActive}
                hasBackup={!!backupData}
                backupData={backupData}
                isAccessibilityGranted={isAccessibilityGranted}
                currentStepId={currentStepId}
                stepMessage={stepMessage}
                stepIndex={stepIndex}
                totalSteps={totalSteps}
                selectedOem={selectedOem}
                onToggleMaster={(target) => runAutomationWorkflow(target)}
                onRestoreOriginal={() => runAutomationWorkflow(false)}
                onRequestAccessibility={onOpenAccessibilityModal}
                onOpenSettingsApp={() => setActiveScreen('settings')}
                onOpenBackupModal={onOpenBackupModal}
              />
            ) : (
              <NativeSettingsView
                settings={settings}
                onUpdateSettings={setSettings}
                onBackToApp={() => setActiveScreen('app')}
                highlightTarget={highlightTarget}
              />
            )}
          </div>

          {/* Android Bottom Navigation Pill */}
          <div className="w-full bg-black/80 py-2 flex items-center justify-center">
            <div className="w-28 h-1 bg-gray-500 rounded-full" />
          </div>
        </div>
      </div>

      {/* Interactive Control Console & Simulation Inspector */}
      <div className="flex flex-col space-y-4 max-w-md w-full">
        {/* Active Screen Navigator */}
        <div className="p-4 rounded-2xl bg-[#111827] border border-gray-800">
          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
            Virtual Phone Screen Switcher
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => setActiveScreen('app')}
              className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 transition-all ${
                activeScreen === 'app'
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20'
                  : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
              }`}
            >
              <span>StatusBar Manager App</span>
            </button>
            <button
              onClick={() => setActiveScreen('settings')}
              className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 transition-all ${
                activeScreen === 'settings'
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20'
                  : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
              }`}
            >
              <span>Native Settings App</span>
            </button>
          </div>
          <p className="text-[11px] text-gray-400 mt-2 leading-relaxed">
            Switch between the app and Android's native Settings app to observe how the Accessibility Service navigates and changes values.
          </p>
        </div>

        {/* Quick Automation Actions */}
        <div className="p-4 rounded-2xl bg-[#111827] border border-gray-800">
          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
            Simulate Accessibility Action
          </div>
          <div className="space-y-2">
            <button
              onClick={() => runAutomationWorkflow(!isHideActive)}
              disabled={currentStepId !== 'idle' && currentStepId !== 'completed' && currentStepId !== 'failed'}
              className="w-full py-2.5 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 active:scale-98 text-white rounded-xl text-xs font-bold flex items-center justify-center space-x-2 shadow-lg shadow-blue-500/20 disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>
                {isHideActive
                  ? 'Run Accessibility Automation: Restore Original'
                  : 'Run Accessibility Automation: Hide All'}
              </span>
            </button>

            <div className="flex items-center justify-between pt-2 border-t border-gray-800">
              <span className="text-xs text-gray-300">Accessibility Service Permission:</span>
              <button
                onClick={() => setIsAccessibilityGranted(!isAccessibilityGranted)}
                className={`text-xs px-2.5 py-1 rounded-lg font-semibold transition-colors ${
                  isAccessibilityGranted
                    ? 'bg-emerald-950/80 text-emerald-300 border border-emerald-500/40'
                    : 'bg-red-950/80 text-red-300 border border-red-500/40'
                }`}
              >
                {isAccessibilityGranted ? 'Granted' : 'Revoked (Click to test)'}
              </button>
            </div>
          </div>
        </div>

        {/* Live Status Bar Visualizer Summary */}
        <div className="p-4 rounded-2xl bg-[#111827] border border-gray-800">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
              Live Status Bar Elements State
            </span>
            <span
              className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                isHideActive
                  ? 'bg-blue-950 text-blue-400 border border-blue-500/30'
                  : 'bg-emerald-950 text-emerald-400 border border-emerald-500/30'
              }`}
            >
              {isHideActive ? 'All Hidden' : 'Active / Restored'}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="bg-[#1A2234] p-2 rounded-lg flex items-center justify-between">
              <span className="text-gray-400">Clock:</span>
              <span className="font-semibold text-white">
                {settings.timeDisplay === 'Do not show' ? (
                  <span className="text-red-400 font-normal">Hidden</span>
                ) : (
                  <span className="text-emerald-400">Visible</span>
                )}
              </span>
            </div>

            <div className="bg-[#1A2234] p-2 rounded-lg flex items-center justify-between">
              <span className="text-gray-400">Network speed:</span>
              <span className="font-semibold text-white">
                {settings.realTimeNetworkSpeed ? (
                  <span className="text-emerald-400">Visible</span>
                ) : (
                  <span className="text-red-400 font-normal">Hidden</span>
                )}
              </span>
            </div>

            <div className="bg-[#1A2234] p-2 rounded-lg flex items-center justify-between">
              <span className="text-gray-400">Wi-Fi & 5G:</span>
              <span className="font-semibold text-white">
                {settings.statusIcons.wifi || settings.statusIcons.mobileData ? (
                  <span className="text-emerald-400">Visible</span>
                ) : (
                  <span className="text-red-400 font-normal">Hidden</span>
                )}
              </span>
            </div>

            <div className="bg-[#1A2234] p-2 rounded-lg flex items-center justify-between">
              <span className="text-gray-400">Bluetooth / NFC:</span>
              <span className="font-semibold text-white">
                {settings.statusIcons.bluetooth || settings.statusIcons.nfc ? (
                  <span className="text-emerald-400">Visible</span>
                ) : (
                  <span className="text-red-400 font-normal">Hidden</span>
                )}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
