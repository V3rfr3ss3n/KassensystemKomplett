/** Kamerascanner für die Browser-Verwaltung; Bilder bleiben nur im Arbeitsspeicher. */
export function createCameraScanner({dom, apiFetch, onCode}) {
    let stream = null;
    let aktiv = false;
    let lauf = 0;

    function kameraStoppen() {
        aktiv = false;
        lauf++;
        stream?.getTracks().forEach(track => track.stop());
        stream = null;
        dom.cameraScanVideo.pause();
        dom.cameraScanVideo.srcObject = null;
    }

    function stop() {
        kameraStoppen();
        dom.cameraScanPanel.hidden = true;
        dom.cameraScanStatus.hidden = true;
        dom.cameraScanButton.disabled = false;
    }

    function hinweis(text) {
        kameraStoppen();
        dom.cameraScanPanel.hidden = true;
        dom.cameraScanStatus.textContent = text;
        dom.cameraScanStatus.hidden = false;
        dom.cameraScanButton.disabled = false;
    }

    async function start() {
        if (aktiv) return;
        if (typeof window.kassensystemBridge?.openScannerInBrowser === 'function') {
            window.kassensystemBridge.openScannerInBrowser();
            return;
        }
        if (!navigator.mediaDevices?.getUserMedia) {
            hinweis('Kamera nicht verfügbar. Bitte Kamerafreigabe und HTTPS prüfen.');
            return;
        }
        aktiv = true;
        const aktuellerLauf = ++lauf;
        dom.cameraScanStatus.textContent = 'Kamera wird gestartet …';
        dom.cameraScanStatus.hidden = false;
        dom.cameraScanButton.disabled = true;
        try {
            const kamera = await navigator.mediaDevices.getUserMedia({audio: false,
                video: {facingMode: {ideal: 'environment'}}});
            if (!aktiv || aktuellerLauf !== lauf) {
                kamera.getTracks().forEach(track => track.stop());
                return;
            }
            stream = kamera;
            dom.cameraScanVideo.srcObject = kamera;
            await dom.cameraScanVideo.play();
            if (!aktiv || aktuellerLauf !== lauf) return;
            dom.cameraScanPanel.hidden = false;
            dom.cameraScanStatus.textContent = 'QR-Code vor die Kamera halten …';
            scanFrame(aktuellerLauf);
        } catch (error) {
            if (!aktiv || aktuellerLauf !== lauf) return;
            const meldung = error.name === 'NotAllowedError'
                ? 'Kamera blockiert. Bitte in den Website-Einstellungen erlauben und erneut scannen.'
                : error.name === 'NotFoundError'
                    ? 'Keine Kamera gefunden. Bitte eine Kamera anschließen und erneut scannen.'
                    : error.name === 'NotReadableError'
                        ? 'Kamera ist belegt. Bitte andere Kamera-Apps schließen und erneut scannen.'
                        : 'Kamera konnte nicht gestartet werden. Bitte erneut scannen.';
            hinweis(meldung);
        }
    }

    async function scanFrame(aktuellerLauf) {
        if (!aktiv || aktuellerLauf !== lauf) return;
        try {
            const video = dom.cameraScanVideo;
            if (video.videoWidth && video.videoHeight) {
                const canvas = document.createElement('canvas');
                const faktor = Math.min(1, 960 / video.videoWidth);
                canvas.width = Math.round(video.videoWidth * faktor);
                canvas.height = Math.round(video.videoHeight * faktor);
                canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
                const bild = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.8));
                if (!bild) throw new Error('Kamerabild konnte nicht erstellt werden.');
                if (!aktiv || aktuellerLauf !== lauf) return;
                const response = await apiFetch('../api/produkte/scan-bild', {
                    method: 'POST', headers: {'Content-Type': 'image/jpeg'}, body: bild
                });
                if (!aktiv || aktuellerLauf !== lauf) return;
                if (response.ok && response.status !== 204) {
                    const code = (await response.json()).scanCode;
                    stop();
                    await onCode(code);
                    return;
                }
                if (response.status !== 204) throw new Error(`Scan fehlgeschlagen (HTTP ${response.status}).`);
            }
        } catch (error) {
            hinweis(error.message || 'QR-Code konnte nicht gelesen werden.');
            return;
        }
        window.setTimeout(() => scanFrame(aktuellerLauf), 400);
    }

    dom.cameraScanClose.addEventListener('click', stop);
    document.addEventListener('visibilitychange', () => { if (document.hidden) stop(); });
    window.addEventListener('pagehide', stop);
    return {start, stop};
}
