import React, { useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Wifi,
  Bluetooth,
  Bell,
  Gauge,
  Clock,
  Battery,
  Percent,
  Radio,
  Headphones,
  Shield,
  Layers,
  Flame,
  Check
} from 'lucide-react';
import { StatusBarSettings, TimeDisplayOption } from '../types';

interface NativeSettingsViewProps {
  settings: StatusBarSettings;
  onUpdateSettings: (updater: (prev: StatusBarSettings) => StatusBarSettings) => void;
  onBackToApp: () => void;
  highlightTarget?: string | null;
}

export const NativeSettingsView: React.FC<NativeSettingsViewProps> = ({
  settings,
  onUpdateSettings,
  onBackToApp,
  highlightTarget
}) => {
  // Navigation stack within Settings: 'status_bar' | 'time_dialog' | 'status_icons'
  const [currentView, setCurrentView] = useState<'status_bar' | 'status_icons'>('status_bar');
  const [isTimeModalOpen, setIsTimeModalOpen] = useState(false);

  const timeOptions: TimeDisplayOption[] = [
    'Show hours and minutes',
    'Show hours, minutes and seconds',
    'Do not show'
  ];

  return (
    <div className="flex flex-col h-full bg-[#121212] text-white overflow-hidden select-none">
      {/* Settings Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-[#1E1E1E] border-b border-gray-800">
        <div className="flex items-center space-x-2">
          {currentView === 'status_icons' ? (
            <button
              onClick={() => setCurrentView('status_bar')}
              className="p-1 -ml-1 text-gray-300 hover:text-white"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
          ) : (
            <button
              onClick={onBackToApp}
              className="p-1 -ml-1 text-gray-300 hover:text-white flex items-center space-x-1"
            >
              <ChevronLeft className="w-5 h-5" />
              <span className="text-xs text-blue-400 font-medium">StatusBar Manager</span>
            </button>
          )}
          <span className="text-sm font-semibold text-white">
            {currentView === 'status_icons' ? 'Status icons' : 'Status bar'}
          </span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {currentView === 'status_bar' ? (
          <>
            <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider px-1">
              General display
            </div>

            {/* Time Setting Item */}
            <div
              onClick={() => setIsTimeModalOpen(true)}
              className={`p-3.5 rounded-xl bg-[#1E1E1E] hover:bg-[#282828] cursor-pointer flex items-center justify-between transition-all ${
                highlightTarget === 'time' ? 'ring-2 ring-blue-500 bg-blue-950/40' : ''
              }`}
            >
              <div className="flex items-center space-x-3">
                <Clock className="w-4 h-4 text-blue-400" />
                <div>
                  <div className="text-xs font-medium text-white">Time</div>
                  <div className="text-[11px] text-gray-400">{settings.timeDisplay}</div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-gray-500" />
            </div>

            {/* Real-time network speed Toggle */}
            <div
              className={`p-3.5 rounded-xl bg-[#1E1E1E] flex items-center justify-between transition-all ${
                highlightTarget === 'speed' ? 'ring-2 ring-blue-500 bg-blue-950/40' : ''
              }`}
            >
              <div className="flex items-center space-x-3">
                <Gauge className="w-4 h-4 text-emerald-400" />
                <div>
                  <div className="text-xs font-medium text-white">Real-time network speed</div>
                  <div className="text-[11px] text-gray-400">Display current network upload/download rate</div>
                </div>
              </div>
              <button
                onClick={() =>
                  onUpdateSettings((prev) => ({
                    ...prev,
                    realTimeNetworkSpeed: !prev.realTimeNetworkSpeed
                  }))
                }
                className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors duration-200 ${
                  settings.realTimeNetworkSpeed ? 'bg-blue-600' : 'bg-gray-700'
                }`}
              >
                <div
                  className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200 ${
                    settings.realTimeNetworkSpeed ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>

            {/* Show Battery Percentage Toggle (MUST NOT BE CONFUSED WITH BATTERY ICON!) */}
            <div className="p-3.5 rounded-xl bg-[#1E1E1E] flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <Percent className="w-4 h-4 text-amber-400" />
                <div>
                  <div className="text-xs font-medium text-white">Show battery percentage</div>
                  <div className="text-[11px] text-gray-400">Numerical percentage format (separate from icon)</div>
                </div>
              </div>
              <button
                onClick={() =>
                  onUpdateSettings((prev) => ({
                    ...prev,
                    batteryPercentage: !prev.batteryPercentage
                  }))
                }
                className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors duration-200 ${
                  settings.batteryPercentage ? 'bg-blue-600' : 'bg-gray-700'
                }`}
              >
                <div
                  className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-200 ${
                    settings.batteryPercentage ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>

            <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider px-1 pt-2">
              System icon management
            </div>

            {/* Submenu: Status icons */}
            <div
              onClick={() => setCurrentView('status_icons')}
              className={`p-3.5 rounded-xl bg-[#1E1E1E] hover:bg-[#282828] cursor-pointer flex items-center justify-between transition-all ${
                highlightTarget === 'status_icons_menu' ? 'ring-2 ring-blue-500 bg-blue-950/40' : ''
              }`}
            >
              <div className="flex items-center space-x-3">
                <Layers className="w-4 h-4 text-purple-400" />
                <div>
                  <div className="text-xs font-medium text-white">Status icons</div>
                  <div className="text-[11px] text-gray-400">
                    Control visibility of Wi-Fi, SIM, Bluetooth, and system icons
                  </div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-gray-500" />
            </div>
          </>
        ) : (
          /* Status icons submenu */
          <div className="space-y-2">
            <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider px-1 mb-2">
              Available Status Icons
            </div>

            {/* Mobile Network / SIM */}
            <SettingToggleItem
              icon={<Radio className="w-4 h-4 text-blue-400" />}
              label="Mobile network / SIM"
              description="Signal reception and cellular data indicators"
              checked={settings.statusIcons['Mobile network'] ?? settings.statusIcons['mobileData'] ?? true}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, 'Mobile network': val, mobileData: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_Mobile network' || highlightTarget === 'icon_mobileData'}
            />

            {/* VoLTE (Voice over LTE) */}
            <SettingToggleItem
              icon={<Radio className="w-4 h-4 text-sky-400" />}
              label="VoLTE"
              description="Voice over LTE high-definition call indicator"
              checked={settings.statusIcons['VoLTE'] ?? true}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, VoLTE: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_VoLTE'}
            />

            {/* VoNR (Voice over New Radio / 5G) */}
            <SettingToggleItem
              icon={<Radio className="w-4 h-4 text-teal-400" />}
              label="VoNR"
              description="Voice over 5G New Radio call indicator"
              checked={settings.statusIcons['VoNR'] ?? true}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, VoNR: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_VoNR'}
            />

            {/* Wi-Fi */}
            <SettingToggleItem
              icon={<Wifi className="w-4 h-4 text-cyan-400" />}
              label="Wi-Fi"
              description="Wi-Fi connectivity and standard indicator"
              checked={settings.statusIcons['Wi-Fi'] ?? settings.statusIcons['wifi'] ?? true}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, 'Wi-Fi': val, wifi: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_Wi-Fi' || highlightTarget === 'icon_wifi'}
            />

            {/* Bluetooth */}
            <SettingToggleItem
              icon={<Bluetooth className="w-4 h-4 text-indigo-400" />}
              label="Bluetooth"
              description="Bluetooth active & connected badge"
              checked={settings.statusIcons['Bluetooth'] ?? settings.statusIcons['bluetooth'] ?? true}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, Bluetooth: val, bluetooth: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_Bluetooth' || highlightTarget === 'icon_bluetooth'}
            />

            {/* Alarm */}
            <SettingToggleItem
              icon={<Bell className="w-4 h-4 text-yellow-400" />}
              label="Alarm"
              description="Active scheduled alarm icon"
              checked={settings.statusIcons['Alarm'] ?? settings.statusIcons['alarm'] ?? true}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, Alarm: val, alarm: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_Alarm' || highlightTarget === 'icon_alarm'}
            />

            {/* NFC */}
            <SettingToggleItem
              icon={<Shield className="w-4 h-4 text-orange-400" />}
              label="NFC"
              description="Near Field Communication enabled icon"
              checked={settings.statusIcons['NFC'] ?? settings.statusIcons['nfc'] ?? false}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, NFC: val, nfc: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_NFC' || highlightTarget === 'icon_nfc'}
            />

            {/* Earphones / Headset */}
            <SettingToggleItem
              icon={<Headphones className="w-4 h-4 text-pink-400" />}
              label="Earphones / Headset"
              description="Wireless audio profile indicator (if exposed by OEM)"
              checked={settings.statusIcons['Earphones'] ?? settings.statusIcons['earphones'] ?? false}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  statusIcons: { ...prev.statusIcons, Earphones: val, earphones: val }
                }))
              }
              isHighlighted={highlightTarget === 'icon_Earphones' || highlightTarget === 'icon_earphones'}
            />

            {/* Battery icon (actual icon, not percentage) */}
            <SettingToggleItem
              icon={<Battery className="w-4 h-4 text-emerald-400" />}
              label="Battery indicator"
              description="Physical battery shape symbol in status bar"
              checked={settings.batteryIcon}
              onChange={(val) =>
                onUpdateSettings((prev) => ({
                  ...prev,
                  batteryIcon: val
                }))
              }
              isHighlighted={highlightTarget === 'icon_Battery' || highlightTarget === 'icon_battery'}
            />
          </div>
        )}
      </div>

      {/* Time Selection Dialog Modal */}
      {isTimeModalOpen && (
        <div className="absolute inset-0 bg-black/70 flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="bg-[#242424] rounded-2xl w-full max-w-xs p-5 shadow-2xl border border-gray-700">
            <h3 className="text-sm font-bold text-white mb-3">Time Display</h3>
            <div className="space-y-1">
              {timeOptions.map((opt) => (
                <div
                  key={opt}
                  onClick={() => {
                    onUpdateSettings((prev) => ({ ...prev, timeDisplay: opt }));
                    setIsTimeModalOpen(false);
                  }}
                  className={`flex items-center justify-between p-2.5 rounded-lg cursor-pointer transition-colors ${
                    settings.timeDisplay === opt ? 'bg-blue-600/30 text-blue-300' : 'hover:bg-gray-700/50 text-gray-200'
                  }`}
                >
                  <span className="text-xs font-medium">{opt}</span>
                  {settings.timeDisplay === opt && <Check className="w-4 h-4 text-blue-400" />}
                </div>
              ))}
            </div>
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setIsTimeModalOpen(false)}
                className="text-xs text-gray-400 hover:text-white px-3 py-1 font-medium"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

interface SettingToggleItemProps {
  icon: React.ReactNode;
  label: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  isHighlighted?: boolean;
}

const SettingToggleItem: React.FC<SettingToggleItemProps> = ({
  icon,
  label,
  description,
  checked,
  onChange,
  isHighlighted
}) => {
  return (
    <div
      className={`p-3 rounded-xl bg-[#1E1E1E] flex items-center justify-between transition-all ${
        isHighlighted ? 'ring-2 ring-blue-500 bg-blue-950/40 scale-[1.01]' : ''
      }`}
    >
      <div className="flex items-center space-x-3 pr-2">
        {icon}
        <div>
          <div className="text-xs font-medium text-white">{label}</div>
          <div className="text-[10px] text-gray-400">{description}</div>
        </div>
      </div>
      <button
        onClick={() => onChange(!checked)}
        className={`w-10 h-5.5 flex items-center rounded-full p-0.5 cursor-pointer transition-colors duration-200 ${
          checked ? 'bg-blue-600' : 'bg-gray-700'
        }`}
      >
        <div
          className={`bg-white w-4.5 h-4.5 rounded-full shadow-md transform transition-transform duration-200 ${
            checked ? 'translate-x-4.5' : 'translate-x-0'
          }`}
        />
      </button>
    </div>
  );
};
