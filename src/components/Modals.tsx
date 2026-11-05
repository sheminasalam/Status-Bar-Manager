import React from 'react';
import {
  ShieldCheck,
  Check,
  X,
  Database,
  Smartphone,
  Clock,
  Layers,
  Lock
} from 'lucide-react';
import { BackupData } from '../types';

interface AccessibilityModalProps {
  isOpen: boolean;
  onClose: () => void;
  onGrant: () => void;
  isGranted: boolean;
}

export const AccessibilityModal: React.FC<AccessibilityModalProps> = ({
  isOpen,
  onClose,
  onGrant,
  isGranted
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-[#111827] border border-gray-800 rounded-3xl p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 p-1 rounded-full text-gray-400 hover:text-white hover:bg-gray-800"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="w-12 h-12 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center mb-4">
          <ShieldCheck className="w-6 h-6 text-blue-400" />
        </div>

        <h3 className="text-lg font-bold text-white mb-2">
          Accessibility Service Permission
        </h3>

        <p className="text-xs text-gray-300 leading-relaxed mb-4">
          To hide or restore your phone&apos;s status bar clock and system icons across all apps,
          StatusBar Manager requires permission to interact with your phone&apos;s native Settings app
          on your behalf.
        </p>

        <div className="space-y-2.5 p-3.5 rounded-xl bg-[#1A2234] border border-gray-800 mb-5 text-xs">
          <div className="flex items-start space-x-2 text-gray-200">
            <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <span>Used <strong>exclusively</strong> to navigate to Settings → Notifications &amp; status bar.</span>
          </div>
          <div className="flex items-start space-x-2 text-gray-200">
            <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <span>Remains <strong>100% idle</strong> outside explicitly requested status bar operations.</span>
          </div>
          <div className="flex items-start space-x-2 text-gray-200">
            <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <span><strong>Never collects, records, or transmits</strong> keystrokes, personal data, or content.</span>
          </div>
          <div className="flex items-start space-x-2 text-gray-200">
            <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <span><strong>Zero internet permission</strong>: completely local execution.</span>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 px-4 rounded-xl bg-gray-800 hover:bg-gray-700 text-xs font-semibold text-gray-200"
          >
            Cancel
          </button>
          <button
            onClick={() => {
              onGrant();
              onClose();
            }}
            className="flex-1 py-2.5 px-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-xs font-bold text-white shadow-lg shadow-blue-500/20"
          >
            {isGranted ? 'Permission Granted' : 'Enable Service'}
          </button>
        </div>
      </div>
    </div>
  );
};

interface BackupModalProps {
  isOpen: boolean;
  onClose: () => void;
  backup: BackupData | null;
}

export const BackupModal: React.FC<BackupModalProps> = ({
  isOpen,
  onClose,
  backup
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-[#111827] border border-gray-800 rounded-3xl p-6 shadow-2xl relative max-h-[90vh] flex flex-col">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 p-1 rounded-full text-gray-400 hover:text-white hover:bg-gray-800"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center space-x-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center">
            <Database className="w-5 h-5 text-blue-400" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">Preserved Baseline Snapshot</h3>
            <p className="text-xs text-gray-400">Android Jetpack Preferences DataStore Record</p>
          </div>
        </div>

        {backup ? (
          <div className="flex-1 overflow-y-auto space-y-4 pr-1 text-xs">
            <div className="p-3 bg-[#1A2234] rounded-xl border border-gray-800 space-y-1.5">
              <div className="flex justify-between">
                <span className="text-gray-400">Backup ID:</span>
                <span className="font-mono text-gray-200">{backup.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Timestamp:</span>
                <span className="text-gray-200">{new Date(backup.timestamp).toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Device Hardware:</span>
                <span className="text-gray-200">{backup.manufacturer} {backup.deviceModel} (API {backup.androidVersion})</span>
              </div>
            </div>

            <div>
              <div className="font-semibold text-gray-300 mb-2">Original General Settings</div>
              <div className="grid grid-cols-2 gap-2">
                <div className="p-3 bg-[#1F2937] rounded-xl">
                  <div className="text-[10px] text-gray-400 uppercase font-medium">Time Setting</div>
                  <div className="text-xs font-bold text-white mt-1">{backup.timeDisplay}</div>
                </div>
                <div className="p-3 bg-[#1F2937] rounded-xl">
                  <div className="text-[10px] text-gray-400 uppercase font-medium">Real-Time Speed</div>
                  <div className="text-xs font-bold text-white mt-1">
                    {backup.realTimeNetworkSpeed ? 'Enabled (ON)' : 'Disabled (OFF)'}
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div className="font-semibold text-gray-300 mb-2">
                Preserved Status Icons State ({Object.keys(backup.statusIcons).length})
              </div>
              <div className="p-3 bg-[#1A2234] rounded-xl border border-gray-800 space-y-2 max-h-48 overflow-y-auto font-mono text-[11px]">
                {Object.entries(backup.statusIcons).map(([key, val]) => (
                  <div key={key} className="flex items-center justify-between py-1 border-b border-gray-800/60 last:border-0">
                    <span className="text-gray-300">{key}</span>
                    <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${val ? 'bg-emerald-950 text-emerald-400 border border-emerald-500/30' : 'bg-gray-800 text-gray-400'}`}>
                      {val ? 'ORIGINALLY ON' : 'ORIGINALLY OFF'}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <p className="text-[11px] text-gray-400 italic">
              Notice: Icons that were originally OFF remain OFF during restoration. The app never turns everything blindly ON.
            </p>
          </div>
        ) : (
          <div className="p-6 text-center text-gray-400 text-xs">
            No active backup found in DataStore. A baseline snapshot is created automatically when you first toggle the master switch ON.
          </div>
        )}

        <div className="pt-4 border-t border-gray-800 flex justify-end">
          <button
            onClick={onClose}
            className="py-2 px-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-xs font-semibold text-white"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
