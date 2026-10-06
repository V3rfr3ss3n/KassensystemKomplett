import test from 'node:test';
import assert from 'node:assert/strict';
import {createCameraScanner} from '../../main/resources/static/admin/admin-camera-scan.js';

function scannerUmgebung(getUserMedia, apiFetch = async () => ({status: 204})) {
    const vorher = {navigator: globalThis.navigator, document: globalThis.document, window: globalThis.window};
    const listeners = {};
    const dom = {
        cameraScanButton: {disabled: false},
        cameraScanClose: {addEventListener(type, callback) { listeners[type] = callback; }},
        cameraScanPanel: {hidden: true},
        cameraScanStatus: {hidden: true, textContent: ''},
        cameraScanVideo: {videoWidth: 640, videoHeight: 480, srcObject: null,
            play: async () => {}, pause() {}}
    };
    Object.defineProperty(globalThis, 'navigator', {configurable: true,
        value: getUserMedia ? {mediaDevices: {getUserMedia}} : {}});
    globalThis.document = {hidden: false, addEventListener() {}, createElement: () => ({
        getContext: () => ({drawImage() {}}), toBlob: callback => callback(new Blob(['bild']))
    })};
    globalThis.window = {addEventListener() {}, setTimeout() {}};
    const scanner = createCameraScanner({dom, apiFetch, onCode: async code => { dom.gefundenerCode = code; }});
    return {scanner, dom, listeners, restore() {
        Object.defineProperty(globalThis, 'navigator', {configurable: true, value: vorher.navigator});
        globalThis.document = vorher.document;
        globalThis.window = vorher.window;
    }};
}

test('Scanner startet direkt, liest den Code und schaltet die Kamera danach aus', async () => {
    let anfragen = 0;
    let gestoppt = 0;
    const umgebung = scannerUmgebung(async () => {
        anfragen++;
        return {getTracks: () => [{stop: () => gestoppt++}]};
    }, async () => ({ok: true, status: 200, json: async () => ({scanCode: 'KS-P-000123'})}));
    try {
        await umgebung.scanner.start();
        await new Promise(resolve => setImmediate(resolve));
        assert.equal(anfragen, 1);
        assert.equal(umgebung.dom.gefundenerCode, 'KS-P-000123');
        assert.equal(gestoppt, 1);
        assert.equal(umgebung.dom.cameraScanPanel.hidden, true);
        assert.equal(umgebung.dom.cameraScanStatus.hidden, true);
        assert.equal(umgebung.dom.cameraScanButton.disabled, false);
    } finally {
        umgebung.restore();
    }
});

test('ohne Kamera-API erscheint nur ein kurzer Hinweis', async () => {
    const umgebung = scannerUmgebung(null);
    try {
        await umgebung.scanner.start();
        assert.match(umgebung.dom.cameraScanStatus.textContent, /Kamera nicht verfügbar/);
        assert.equal(umgebung.dom.cameraScanStatus.hidden, false);
        assert.equal(umgebung.dom.cameraScanPanel.hidden, true);
    } finally {
        umgebung.restore();
    }
});

test('WebView-Scan öffnet weiter die Verwaltung im Standardbrowser', async () => {
    const umgebung = scannerUmgebung(null);
    let geoeffnet = 0;
    globalThis.window.kassensystemBridge = {openScannerInBrowser() { geoeffnet++; }};
    try {
        await umgebung.scanner.start();
        assert.equal(geoeffnet, 1);
        assert.equal(umgebung.dom.cameraScanStatus.hidden, true);
    } finally {
        umgebung.restore();
    }
});

test('bei verweigerter Kamera bleibt nur ein Hinweis; erneuter Klick versucht erneut', async () => {
    let anfragen = 0;
    const umgebung = scannerUmgebung(async () => {
        anfragen++;
        const error = new Error('denied');
        error.name = 'NotAllowedError';
        throw error;
    });
    try {
        await umgebung.scanner.start();
        assert.match(umgebung.dom.cameraScanStatus.textContent, /Kamera blockiert/);
        assert.equal(umgebung.dom.cameraScanPanel.hidden, true);
        assert.equal(umgebung.dom.cameraScanButton.disabled, false);
        await umgebung.scanner.start();
        assert.equal(anfragen, 2);
    } finally {
        umgebung.restore();
    }
});

test('ohne Kamera erscheint ein passender Hinweis', async () => {
    const umgebung = scannerUmgebung(async () => {
        const error = new Error('missing');
        error.name = 'NotFoundError';
        throw error;
    });
    try {
        await umgebung.scanner.start();
        assert.match(umgebung.dom.cameraScanStatus.textContent, /Keine Kamera gefunden/);
        assert.equal(umgebung.dom.cameraScanPanel.hidden, true);
    } finally {
        umgebung.restore();
    }
});

test('Scan beenden während der Freigabe öffnet kein verspätetes Kamerabild', async () => {
    let freigeben;
    let gestoppt = 0;
    const umgebung = scannerUmgebung(() => new Promise(resolve => { freigeben = resolve; }));
    try {
        const start = umgebung.scanner.start();
        umgebung.listeners.click();
        freigeben({getTracks: () => [{stop: () => gestoppt++}]});
        await start;
        assert.equal(gestoppt, 1);
        assert.equal(umgebung.dom.cameraScanPanel.hidden, true);
        assert.equal(umgebung.dom.cameraScanStatus.hidden, true);
    } finally {
        umgebung.restore();
    }
});
