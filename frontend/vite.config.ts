import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Alle Aufrufe an /api/** während der Entwicklung an mein Spring-Boot-
    // Backend auf Port 8080 weiterleiten. Dadurch sieht der Browser nur
    // eine Origin (localhost:5173) und ich muss mich nicht mit CORS
    // herumschlagen - meine SecurityConfig hat ohnehin keine CORS-
    // Konfiguration, weil ich bisher Frontend und Backend als eine
    // Origin behandle (Login/Logout laufen direkt gegen Port 8080, nur
    // die reinen Daten-API-Calls laufen über diesen Proxy).
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})