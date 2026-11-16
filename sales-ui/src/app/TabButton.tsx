import React from 'react';

interface TabButtonProps {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}

export const TabButton: React.FC<TabButtonProps> = ({ active, onClick, icon, label }) => (
  <button
    type="button"
    onClick={onClick}
    className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5 cursor-pointer ${
      active
        ? 'bg-brand text-ink shadow-xs'
        : 'text-gray-300 hover:bg-gray-800 hover:text-white'
    }`}
  >
    {icon}
    <span>{label}</span>
  </button>
);
