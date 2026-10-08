import { ApiError } from '../api/api';

export async function downloadFile(path: string, fileName: string): Promise<void> {
    const response = await fetch(path, { credentials: 'include' });
    if (!response.ok) {
        throw new ApiError(response.status, response.statusText);
    }
    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(objectUrl);
}
