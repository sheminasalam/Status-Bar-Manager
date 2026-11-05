import React from 'react';
import {
  ShieldAlert,
  Cpu,
  Layers,
  Database,
  Terminal,
  CheckCircle2,
  Workflow,
  Sparkles
} from 'lucide-react';

export const ArchitectureDoc: React.FC = () => {
  return (
    <div className="space-y-6 max-w-4xl mx-auto py-2 text-gray-300">
      {/* Hero Overview */}
      <div className="p-6 rounded-2xl bg-gradient-to-br from-[#111827] to-[#1E293B] border border-gray-800 shadow-xl">
        <div className="flex items-center space-x-3 mb-3">
          <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
            <Cpu className="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">
              StatusBar Manager – Architecture & Engineering Specifications
            </h2>
            <p className="text-xs text-blue-400">
              Native Android Accessibility Service & Jetpack Compose Material 3 System Bar Automation
            </p>
          </div>
        </div>
        <p className="text-xs leading-relaxed text-gray-300">
          StatusBar Manager controls the visibility of the Android status bar clock and status icons
          across the entire phone without requiring root access, ADB commands, or{' '}
          <code className="text-blue-300 bg-black/40 px-1 py-0.5 rounded font-mono">WRITE_SECURE_SETTINGS</code>.
          It achieves system-wide persistence by programmatically driving the device manufacturer's native
          Settings UI through a dedicated Android <code className="text-blue-300 bg-black/40 px-1 py-0.5 rounded font-mono">AccessibilityService</code>.
        </p>
      </div>

      {/* 4 Architectural Pillars */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Pillar 1 */}
        <div className="p-4 rounded-xl bg-[#111827] border border-gray-800 space-y-2">
          <div className="flex items-center space-x-2 text-white font-bold text-xs">
            <Workflow className="w-4 h-4 text-blue-400" />
            <span>State Machine & Node Traversal</span>
          </div>
          <p className="text-[11px] text-gray-400 leading-relaxed">
            The service inspects the system UI tree using{' '}
            <code className="text-blue-300 font-mono">AccessibilityNodeInfo</code>, performing BFS traversal
            for target labels like &quot;Notifications &amp; status bar&quot;, &quot;Status bar&quot;, &quot;Time&quot;,
            and &quot;Status icons&quot;. When accessibility click actions are intercepted by custom view wrappers,
            it dispatches exact gesture taps via <code className="text-blue-300 font-mono">dispatchGesture()</code>.
          </p>
        </div>

        {/* Pillar 2 */}
        <div className="p-4 rounded-xl bg-[#111827] border border-gray-800 space-y-2">
          <div className="flex items-center space-x-2 text-white font-bold text-xs">
            <Database className="w-4 h-4 text-emerald-400" />
            <span>Persistent DataStore Snapshot</span>
          </div>
          <p className="text-[11px] text-gray-400 leading-relaxed">
            Prior to altering any toggle, the service captures the exact user baseline: Time format,
            Real-time speed toggle, and every individual status icon state. This structured record is
            persisted in Android Jetpack Preferences DataStore and protected against overwriting during active hide mode.
          </p>
        </div>

        {/* Pillar 3 */}
        <div className="p-4 rounded-xl bg-[#111827] border border-gray-800 space-y-2">
          <div className="flex items-center space-x-2 text-white font-bold text-xs">
            <Layers className="w-4 h-4 text-amber-400" />
            <span>Accurate Restoration Guarantee</span>
          </div>
          <p className="text-[11px] text-gray-400 leading-relaxed">
            When toggled OFF or clicking &quot;Restore Original Settings&quot;, the service never turns everything ON.
            It consults the saved DataStore model. If NFC or Alarm was originally disabled before the app
            was used, it remains disabled upon restoration.
          </p>
        </div>

        {/* Pillar 4 */}
        <div className="p-4 rounded-xl bg-[#111827] border border-gray-800 space-y-2">
          <div className="flex items-center space-x-2 text-white font-bold text-xs">
            <ShieldAlert className="w-4 h-4 text-purple-400" />
            <span>Zero-Permission Privacy Model</span>
          </div>
          <p className="text-[11px] text-gray-400 leading-relaxed">
            No internet permission (<code className="text-red-300 font-mono">INTERNET</code>) is declared or requested
            in <code className="text-purple-300 font-mono">AndroidManifest.xml</code>. The service operates locally,
            remains completely idle outside of requested operations, and never captures sensitive screen text or keystrokes.
          </p>
        </div>
      </div>

      {/* Target UI Hierarchy Sequence */}
      <div className="p-5 rounded-xl bg-[#111827] border border-gray-800">
        <h3 className="text-xs font-bold text-white uppercase tracking-wider mb-3">
          Automated Settings Hierarchy Sequence
        </h3>
        <div className="space-y-3 font-mono text-xs">
          <div className="flex items-start space-x-3 p-2.5 rounded-lg bg-[#1F2937]/50 border border-gray-700/50">
            <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 text-[10px] font-bold">1</span>
            <div>
              <div className="text-white font-semibold">Settings → Notifications &amp; status bar</div>
              <div className="text-[11px] text-gray-400 mt-0.5">Dispatches launch intent <code className="text-blue-300 font-mono">Settings.ACTION_SETTINGS</code> or direct OEM notification action.</div>
            </div>
          </div>

          <div className="flex items-start space-x-3 p-2.5 rounded-lg bg-[#1F2937]/50 border border-gray-700/50">
            <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 text-[10px] font-bold">2</span>
            <div>
              <div className="text-white font-semibold">Status bar Section</div>
              <div className="text-[11px] text-gray-400 mt-0.5">Identifies the &quot;Status bar&quot; item with fuzzy matching and invokes <code className="text-blue-300 font-mono">ACTION_CLICK</code>.</div>
            </div>
          </div>

          <div className="flex items-start space-x-3 p-2.5 rounded-lg bg-[#1F2937]/50 border border-gray-700/50">
            <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 text-[10px] font-bold">3</span>
            <div>
              <div className="text-white font-semibold">Time: Change to &quot;Do not show&quot;</div>
              <div className="text-[11px] text-gray-400 mt-0.5">Opens Time selection dialog, backs up initial format (e.g., &quot;Show hours and minutes&quot;), and selects &quot;Do not show&quot;.</div>
            </div>
          </div>

          <div className="flex items-start space-x-3 p-2.5 rounded-lg bg-[#1F2937]/50 border border-gray-700/50">
            <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 text-[10px] font-bold">4</span>
            <div>
              <div className="text-white font-semibold">Real-time network speed: Switch OFF</div>
              <div className="text-[11px] text-gray-400 mt-0.5">Inspects checkable sibling node for network speed and flips state if enabled.</div>
            </div>
          </div>

          <div className="flex items-start space-x-3 p-2.5 rounded-lg bg-[#1F2937]/50 border border-gray-700/50">
            <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 text-[10px] font-bold">5</span>
            <div>
              <div className="text-white font-semibold">Status icons Submenu: Disable all supported icons</div>
              <div className="text-[11px] text-gray-400 mt-0.5">
                Navigates to Status icons; toggles SIM/Mobile, Wi-Fi, Bluetooth, Alarm, NFC, Earphones.
                <span className="text-amber-400 block mt-1">
                  ⚠️ Special rule: Explicitly ignores &quot;Show battery percentage&quot; so percentage format is not confused with the battery icon visibility setting.
                </span>
              </div>
            </div>
          </div>

          <div className="flex items-start space-x-3 p-2.5 rounded-lg bg-[#1F2937]/50 border border-gray-700/50">
            <span className="w-5 h-5 rounded-full bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 text-[10px] font-bold">6</span>
            <div>
              <div className="text-white font-semibold">Verification &amp; Return</div>
              <div className="text-[11px] text-gray-400 mt-0.5">Reads resulting state from screen hierarchy. If verified, calls <code className="text-blue-300 font-mono">performGlobalAction(GLOBAL_ACTION_BACK)</code> and brings StatusBar Manager to the foreground.</div>
            </div>
          </div>
        </div>
      </div>

      {/* Build & Installation Commands */}
      <div className="p-5 rounded-xl bg-[#111827] border border-gray-800">
        <div className="flex items-center space-x-2 text-white font-bold text-xs mb-3">
          <Terminal className="w-4 h-4 text-emerald-400" />
          <span>Building the Native Android APK</span>
        </div>
        <div className="p-3 bg-black/60 rounded-lg font-mono text-xs text-emerald-400 space-y-2 overflow-x-auto">
          <div><span className="text-gray-500"># 1. Compile and package the debug APK</span></div>
          <div>./gradlew assembleDebug</div>
          <div className="pt-2"><span className="text-gray-500"># 2. Install onto physical device or emulator via ADB</span></div>
          <div>adb install -r app/build/outputs/apk/debug/app-debug.apk</div>
          <div className="pt-2"><span className="text-gray-500"># 3. Grant Accessibility Service permission via Settings or test command</span></div>
          <div>adb shell settings put secure enabled_accessibility_services com.statusbarmanager.app/com.statusbarmanager.app.service.StatusBarAccessibilityService</div>
          <div>adb shell settings put secure accessibility_enabled 1</div>
        </div>
      </div>

      {/* GitHub Actions CI/CD Fix for 'Failed to find package tools' */}
      <div className="p-5 rounded-xl bg-gradient-to-br from-red-950/30 to-[#111827] border border-red-500/40">
        <div className="flex items-center space-x-2 text-white font-bold text-xs mb-2">
          <ShieldAlert className="w-4 h-4 text-red-400" />
          <span>GitHub Actions Fix: &quot;Warning: Failed to find package &apos;tools&apos;&quot; (Exit code 1)</span>
        </div>
        <p className="text-[11px] text-gray-300 leading-relaxed mb-3">
          <strong>Root Cause:</strong> In <code className="text-red-300 font-mono">android-actions/setup-android@v3</code>,
          specifying <code className="text-red-300 font-mono">packages: tools platform-tools</code> causes <code className="text-red-300 font-mono">sdkmanager</code> to fail
          because Google removed the legacy <code className="text-red-300 font-mono">tools</code> package from modern SDK repositories.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 font-mono text-[11px]">
          <div className="p-3 bg-red-950/40 border border-red-500/30 rounded-lg">
            <span className="text-red-400 font-bold block mb-1">❌ Broken (in your workflow):</span>
            <pre className="text-gray-300 whitespace-pre overflow-x-auto">{`- name: Set up Android SDK
  uses: android-actions/setup-android@v3
  with:
    packages: tools platform-tools`}</pre>
          </div>

          <div className="p-3 bg-emerald-950/40 border border-emerald-500/30 rounded-lg">
            <span className="text-emerald-400 font-bold block mb-1">✅ Fixed Solution:</span>
            <pre className="text-gray-200 whitespace-pre overflow-x-auto">{`- name: Set up Android SDK
  uses: android-actions/setup-android@v3
  with:
    accept-android-sdk-licenses: true
    packages: 'platform-tools platforms;android-35 build-tools;35.0.0'`}</pre>
          </div>
        </div>

        <p className="text-[11px] text-gray-400 mt-3">
          Ready-to-run workflow files have been generated at <code className="text-blue-300 font-mono">.github/workflows/android-debug-apk.yml</code> and <code className="text-blue-300 font-mono">.github/workflows/build-apk.yml</code> and are included in the project export.
        </p>
      </div>
    </div>
  );
};
