/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#F4F5F0',
        'paper-dim': '#EAEBE3',
        ink: '#16233B',
        'ink-soft': '#3C4A61',
        'ink-faint': '#6B7688',
        bronze: '#9C6B30',
        'bronze-soft': '#C79A5C',
        'bronze-dim': '#EFE6D6',
        verified: '#2F6B4F',
        'verified-dim': '#E1EBE5',
        alert: '#A6432D',
        'alert-dim': '#F3E3DE',
        revoked: '#8A6D1D',
        'revoked-dim': '#F1EAD6',
        line: '#D9DAD2',
        'line-soft': '#E6E7E0',
      },
      fontFamily: {
        display: ['"Fraunces"', 'serif'],
        sans: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(22,35,59,0.06), 0 1px 0 rgba(22,35,59,0.04)',
        stamp: '0 8px 24px rgba(22,35,59,0.18)',
      },
      keyframes: {
        stampIn: {
          '0%': { transform: 'scale(2.2) rotate(-14deg)', opacity: '0' },
          '55%': { transform: 'scale(0.94) rotate(-4deg)', opacity: '1' },
          '75%': { transform: 'scale(1.04) rotate(-6deg)' },
          '100%': { transform: 'scale(1) rotate(-4deg)', opacity: '1' },
        },
        linkLight: {
          '0%': { opacity: '0.25' },
          '100%': { opacity: '1' },
        },
        fadeUp: {
          '0%': { opacity: '0', transform: 'translateY(6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        stampIn: 'stampIn 0.5s cubic-bezier(.2,.9,.3,1.1) forwards',
        linkLight: 'linkLight 0.4s ease forwards',
        fadeUp: 'fadeUp 0.4s ease forwards',
      },
    },
  },
  plugins: [],
}
