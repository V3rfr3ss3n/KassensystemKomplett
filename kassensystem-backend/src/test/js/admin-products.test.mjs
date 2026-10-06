import test from 'node:test';
import assert from 'node:assert/strict';
import {createLabelSelection} from '../../main/resources/static/admin/admin-products.js';

function fixture() {
    const state = {products: [{id: 1, name: 'Apfel'}, {id: 2, name: 'Birne'}]};
    const dom = {
        searchInput: {value: ''}, unitFilter: {value: ''}, categoryFilter: {value: ''},
        taxFilter: {value: ''}, stockFilter: {checked: false},
        selectVisibleProducts: {checked: true}, downloadLabelsButton: {}
    };
    const status = [];
    let requests = [];
    const labels = createLabelSelection({state, dom,
        apiFetch: async (url, options) => { requests.push({url, options}); return {ok: true, blob: async () => new Blob(['%PDF'])}; },
        matchesFilter: product => product.id === 1,
        setStatus: message => status.push(message),
        showError: () => { throw new Error('Unexpected HTTP error'); }
    });
    return {labels, dom, status, getRequests: () => requests};
}

test('Auswahl sichtbarer Produkte erzeugt Browser-PDF-Download', async () => {
    const oldWindow = globalThis.window;
    const oldDocument = globalThis.document;
    const oldCreate = URL.createObjectURL;
    const oldRevoke = URL.revokeObjectURL;
    let clicked = false;
    globalThis.window = {setTimeout: () => 0};
    globalThis.document = {createElement: () => ({click() { clicked = true; }})};
    URL.createObjectURL = () => 'blob:test';
    URL.revokeObjectURL = () => {};
    try {
        const fixtureData = fixture();
        fixtureData.labels.selectVisible();
        fixtureData.labels.updateControls();
        assert.equal(fixtureData.dom.downloadLabelsButton.disabled, false);
        await fixtureData.labels.download();
        assert.equal(clicked, true);
        assert.deepEqual(JSON.parse(fixtureData.getRequests()[0].options.body), {produktIds: [1]});
    } finally {
        globalThis.window = oldWindow;
        globalThis.document = oldDocument;
        URL.createObjectURL = oldCreate;
        URL.revokeObjectURL = oldRevoke;
    }
});

test('WebView-Bridge erhält ausschließlich die gewählten Produkt-IDs', async () => {
    const oldWindow = globalThis.window;
    let ids;
    globalThis.window = {kassensystemBridge: {saveLabels(value) { ids = JSON.parse(value); }}};
    try {
        const fixtureData = fixture();
        fixtureData.labels.selectVisible();
        await fixtureData.labels.download();
        assert.deepEqual(ids, [1]);
        assert.equal(fixtureData.getRequests().length, 0);
    } finally {
        globalThis.window = oldWindow;
    }
});
