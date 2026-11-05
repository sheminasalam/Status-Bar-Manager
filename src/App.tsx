import React, { useState } from 'react';
import {
  Smartphone,
  FileCode,
  BookOpen,
  Download,
  ShieldCheck,
  CheckCircle2,
  Sparkles,
  Layers,
  ExternalLink,
  Github
} from 'lucide-react';
import { StatusBarSettings, BackupData, DeviceOem } from './types';
import { PhoneSimulator } from './components/PhoneSimulator';
import { CodeExplorer } from './components/CodeExplorer';
import { ArchitectureDoc } from './components/ArchitectureDoc';
import { AccessibilityModal, BackupModal } from './components/Modals';
import { exportAndroidProjectZip } from './utils/zipExporter';

export default function App() {
  const [activeTab, setActiveTab] = useState<'simulator' | 'code' | 'docs'>('simulator');
  const [selectedOem, setSelectedOem] = useState<DeviceOem>('color_os');
  const [isAccessibilityGranted, setIsAccessibilityGranted] = useState(true);
  const [isHideActive, setIsHideActive] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  // Initial phone settings simulating a user phone before using StatusBar Manager
  const [settings, setSettings] = useState<StatusBarSettings>({
    timeDisplay: 'Show hours and minutes',
    realTimeNetworkSpeed: true,
    batteryPercentage: true,
    batteryIcon: true,
    statusIcons: {
      'Mobile network': true,
      mobileData: true,
      VoLTE: true,
      VoNR: true,
      'Wi-Fi': true,
      wifi: true,
      Bluetooth: true,
      bluetooth: true,
      Alarm: true,
      alarm: true,
      NFC: false,
      nfc: false,
      Earphones: false,
      earphones: false
    },
    hasWiredHeadphonesConnected: false
  });

  const [backupData, setBackupData] = useState<BackupData | null>(null);
  const [isAccessibilityModalOpen, setIsAccessibilityModalOpen] = useState(false);
  const [isBackupModalOpen, setIsBackupModalOpen] = useState(false);

  const handleExportZip = async () => {
    try {
      setIsExporting(true);
      await exportAndroidProjectZip();
    } catch (e) {
      console.error('Export failed:', e);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#070A10] text-gray-100 flex flex-col font-sans selection:bg-blue-600 selection:text-white">
      {/* Top Global Navigation Bar */}
      <header className="sticky top-0 z-40 bg-[#0B0F19]/90 backdrop-blur-md border-b border-gray-800 px-4 lg:px-8 py-3">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          {/* Brand Logo & Title */}
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-700 to-blue-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
              <Smartphone className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-base text-white tracking-tight">
                  SysHush
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-cyan-950 text-cyan-400 border border-cyan-500/30 uppercase tracking-wide">
                  System Mode Control
                </span>
              </div>
              <p className="text-[11px] text-gray-400 hidden sm:block">
                Control your status bar and manage background applications with one simple action.
              </p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="flex items-center bg-[#111827] p-1 rounded-xl border border-gray-800">
            <button
              onClick={() => setActiveTab('simulator')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'simulator'
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Interactive Phone</span>
            </button>

            <button
              onClick={() => setActiveTab('code')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'code'
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <FileCode className="w-3.5 h-3.5" />
              <span>Code Explorer</span>
            </button>

            <button
              onClick={() => setActiveTab('docs')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'docs'
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <BookOpen className="w-3.5 h-3.5" />
              <span>Architecture &amp; Docs</span>
            </button>
          </div>

          {/* Download Project ZIP Button */}
          <div className="flex items-center space-x-2">
            <button
              onClick={handleExportZip}
              disabled={isExporting}
              className="py-1.5 px-3.5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 active:scale-95 text-white font-semibold text-xs rounded-xl shadow-md shadow-blue-500/20 flex items-center space-x-2 transition-all disabled:opacity-50"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{isExporting ? 'Generating ZIP...' : 'Export Android Project (.zip)'}</span>
            </button>
          </div>
        </div>
      </header>

      {/* Subheader Toolbar */}
      <div className="bg-[#0D121F] border-b border-gray-800/80 px-4 lg:px-8 py-2 text-xs">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          {/* Target OEM Selector */}
          <div className="flex items-center space-x-2">
            <span className="text-gray-400 font-medium">Target Device Profile:</span>
            <select
              value={selectedOem}
              onChange={(e) => setSelectedOem(e.target.value as DeviceOem)}
              className="bg-[#1A2234] border border-gray-700 text-white rounded-lg px-2.5 py-1 text-xs focus:outline-none focus:border-blue-500 font-medium"
            >
              <option value="color_os">ColorOS / OxygenOS (Target Hardware)</option>
              <option value="one_ui">Samsung One UI</option>
              <option value="hyper_os">Xiaomi HyperOS / MIUI</option>
              <option value="pixel">Google Pixel (AOSP)</option>
            </select>
          </div>

          {/* Quick Info Badges */}
          <div className="flex items-center space-x-4 text-gray-400">
            <div className="flex items-center space-x-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span>Min SDK: Android 8.0 (API 26)</span>
            </div>
            <div className="hidden md:flex items-center space-x-1.5">
              <span className="w-2 h-2 rounded-full bg-blue-400" />
              <span>Compile SDK: 35</span>
            </div>
            <div className="hidden md:flex items-center space-x-1.5">
              <span className="w-2 h-2 rounded-full bg-purple-400" />
              <span>No Internet Permission Required</span>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-8">
        {activeTab === 'simulator' && (
          <div className="space-y-6">
            {/* Guide strip */}
            <div className="p-4 rounded-2xl bg-[#111827]/70 border border-gray-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-bold text-white flex items-center space-x-2">
                  <span>Interactive Android Phone Simulator</span>
                  <span className="text-[10px] font-normal px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-500/30">
                    Live UI Verification
                  </span>
                </h2>
                <p className="text-xs text-gray-400 mt-0.5">
                  Toggle the master switch below to watch the simulated native Accessibility Service navigate Settings,
                  back up the exact baseline, toggle Time to &quot;Do not show&quot;, disable real-time network speed,
                  and disable status icons. Notice the phone&apos;s status bar reacts live!
                </p>
              </div>

              <div className="flex items-center space-x-2 shrink-0">
                <button
                  onClick={() => setIsBackupModalOpen(true)}
                  className="px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-xs font-medium text-gray-200 transition-colors"
                >
                  Inspect DataStore
                </button>
                <button
                  onClick={() => setIsAccessibilityModalOpen(true)}
                  className="px-3 py-1.5 rounded-lg bg-blue-600/20 text-blue-400 border border-blue-500/30 hover:bg-blue-600/30 text-xs font-medium transition-colors"
                >
                  Accessibility Info
                </button>
              </div>
            </div>

            {/* Phone Simulator Component */}
            <PhoneSimulator
              settings={settings}
              setSettings={setSettings}
              backupData={backupData}
              setBackupData={setBackupData}
              isHideActive={isHideActive}
              setIsHideActive={setIsHideActive}
              isAccessibilityGranted={isAccessibilityGranted}
              setIsAccessibilityGranted={setIsAccessibilityGranted}
              selectedOem={selectedOem}
              onOpenAccessibilityModal={() => setIsAccessibilityModalOpen(true)}
              onOpenBackupModal={() => setIsBackupModalOpen(true)}
            />
          </div>
        )}

        {activeTab === 'code' && (
          <CodeExplorer onExportZip={handleExportZip} />
        )}

        {activeTab === 'docs' && (
          <ArchitectureDoc />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-gray-800/80 bg-[#090D16] py-4 px-4 lg:px-8 text-xs text-gray-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>StatusBar Manager • Native Android Architecture (Kotlin, Compose, Accessibility Service)</span>
          <div className="flex items-center space-x-3 text-gray-400">
            <span>Settings → Notifications &amp; status bar → Status bar</span>
          </div>
        </div>
      </footer>

      {/* Modals */}
      <AccessibilityModal
        isOpen={isAccessibilityModalOpen}
        onClose={() => setIsAccessibilityModalOpen(false)}
        onGrant={() => setIsAccessibilityGranted(true)}
        isGranted={isAccessibilityGranted}
      />

      <BackupModal
        isOpen={isBackupModalOpen}
        onClose={() => setIsBackupModalOpen(false)}
        backup={backupData}
      />
    </div>
  );
}
