import React, { useState } from 'react';
import {
  FileCode,
  Folder,
  Copy,
  Check,
  Search,
  Download,
  Terminal,
  FileText
} from 'lucide-react';
import { ANDROID_FILES } from '../data/androidFiles';
import { CodeFile } from '../types';

interface CodeExplorerProps {
  onExportZip: () => void;
}

export const CodeExplorer: React.FC<CodeExplorerProps> = ({ onExportZip }) => {
  const [selectedFile, setSelectedFile] = useState<CodeFile>(ANDROID_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');

  const categories = ['All', 'Kotlin Source', 'Manifest & Resources', 'Gradle Build'];

  const filteredFiles = ANDROID_FILES.filter((file) => {
    const matchesCategory =
      selectedCategory === 'All' || file.category === selectedCategory;
    const matchesSearch =
      file.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      file.path.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex flex-col lg:flex-row h-[720px] bg-[#0E131F] rounded-2xl border border-gray-800 overflow-hidden shadow-2xl">
      {/* File Tree / Sidebar */}
      <div className="w-full lg:w-80 bg-[#111827] border-r border-gray-800 flex flex-col shrink-0">
        <div className="p-3.5 border-b border-gray-800">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <Folder className="w-4 h-4 text-blue-400" />
              <span className="text-xs font-bold text-white uppercase tracking-wider">
                Android Project Tree
              </span>
            </div>
            <span className="text-[10px] text-gray-400 font-mono">
              {filteredFiles.length} files
            </span>
          </div>

          {/* Search */}
          <div className="relative mb-2.5">
            <Search className="w-3.5 h-3.5 text-gray-500 absolute left-2.5 top-2.5" />
            <input
              type="text"
              placeholder="Search source files..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 bg-[#1F2937] text-white text-xs rounded-lg border border-gray-700 focus:outline-none focus:border-blue-500"
            />
          </div>

          {/* Category Tabs */}
          <div className="flex flex-wrap gap-1">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`text-[10px] px-2 py-0.5 rounded-md font-medium transition-colors ${
                  selectedCategory === cat
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-800 text-gray-400 hover:text-white'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* File List */}
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {filteredFiles.map((file) => {
            const isSelected = selectedFile.path === file.path;
            return (
              <button
                key={file.path}
                onClick={() => setSelectedFile(file)}
                className={`w-full flex items-center space-x-2.5 px-3 py-2 rounded-lg text-left transition-all ${
                  isSelected
                    ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30'
                    : 'text-gray-400 hover:bg-gray-800/60 hover:text-gray-200'
                }`}
              >
                <FileCode className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-blue-400' : 'text-gray-500'}`} />
                <div className="truncate">
                  <div className="text-xs font-mono font-medium truncate">{file.name}</div>
                  <div className="text-[10px] text-gray-500 truncate">{file.path}</div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Bottom Download Prompt */}
        <div className="p-3 bg-[#161F30] border-t border-gray-800">
          <button
            onClick={onExportZip}
            className="w-full py-2 px-3 bg-blue-600 hover:bg-blue-500 active:scale-98 text-white rounded-lg text-xs font-bold flex items-center justify-center space-x-2 shadow-md shadow-blue-500/20"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download Project (.ZIP)</span>
          </button>
        </div>
      </div>

      {/* Code Display Area */}
      <div className="flex-1 flex flex-col bg-[#0A0E17] overflow-hidden">
        {/* Code Header Bar */}
        <div className="flex items-center justify-between px-4 py-3 bg-[#111827] border-b border-gray-800">
          <div className="flex items-center space-x-2 truncate pr-2">
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-500/30 uppercase">
              {selectedFile.language}
            </span>
            <span className="text-xs font-mono text-gray-200 truncate">{selectedFile.path}</span>
          </div>

          <button
            onClick={handleCopy}
            className="flex items-center space-x-1.5 px-2.5 py-1 rounded bg-[#1F2937] hover:bg-gray-700 text-xs text-gray-300 hover:text-white transition-colors"
          >
            {copied ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span className="text-emerald-400">Copied</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" />
                <span>Copy</span>
              </>
            )}
          </button>
        </div>

        {/* Source Code Content */}
        <div className="flex-1 overflow-auto p-4 font-mono text-xs text-gray-300 leading-relaxed bg-[#0A0E17]">
          <pre className="select-text whitespace-pre">
            {selectedFile.content.split('\n').map((line, i) => (
              <div key={i} className="table-row">
                <span className="table-cell pr-4 text-gray-600 select-none text-right w-10 text-[11px]">
                  {i + 1}
                </span>
                <span className="table-cell">{line}</span>
              </div>
            ))}
          </pre>
        </div>
      </div>
    </div>
  );
};
