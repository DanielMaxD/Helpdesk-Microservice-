/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          950: "#0B1220",
          900: "#121B29",
          800: "#1B2637",
          700: "#26334A",
        },
        paper: {
          0: "#FFFFFF",
          50: "#F7F8FA",
        },
        slate: {
          600: "#4B5565",
          500: "#68748A",
          400: "#8B95A5",
          200: "#E3E6EB",
        },
        amber: {
          DEFAULT: "#E8A33D",
          100: "#FBEBD2",
        },
        coral: {
          DEFAULT: "#E85D4E",
          100: "#FBDAD5",
        },
        teal: {
          DEFAULT: "#2E8B7E",
          100: "#D9EEEA",
        },
        indigo: {
          DEFAULT: "#4C5FD5",
          600: "#4655C2",
          100: "#E3E5FB",
        },
      },
      fontFamily: {
        display: ['"Space Grotesk"', "sans-serif"],
        body: ['"IBM Plex Sans"', "sans-serif"],
        mono: ['"JetBrains Mono"', "monospace"],
      },
      boxShadow: {
        card: "0 1px 2px rgba(11, 18, 32, 0.06), 0 1px 1px rgba(11, 18, 32, 0.04)",
      },
    },
  },
  plugins: [],
};
