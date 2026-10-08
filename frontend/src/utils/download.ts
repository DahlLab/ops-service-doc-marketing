import { ApiError } from '../api/api';

// Lädt eine Datei vom Backend als Blob und stößt den Browser-Download an.
// Ich nutze bewusst fetch mit credentials:'include' + Blob/ObjectURL statt
// eines einfachen <a href>: so geht das Session-Cookie sicher mit, und ein
// 401/404 wird als Fehler erkannt, statt dass eine Fehlerseite als "PDF"
// heruntergeladen wird. Wirft ApiError bei Fehlerantwort.
export async function ladeDateiHerunter(pfad: string, dateiname: string): Promise<void> {
    const response = await fetch(pfad, { credentials: 'include' });
    if (!response.ok) {
        throw new ApiError(response.status, response.statusText);
    }
    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);

    // Flüchtigen Download-Link erzeugen, klicken, wieder entfernen.
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = dateiname;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(objectUrl);
}
