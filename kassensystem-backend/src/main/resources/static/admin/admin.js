const apiUrl = '../api/produkte';
const sessionUrl = '../api/session';
let ready = false;
let pendingArea = null;

const state = {
    products: [],
    selectedProduct: null,
    activeArea: 'edit',
    imageUploading: false,
    session: {
        username: '',
        roles: [],
        permissions: {
            manageProducts: false,
            bookStock: false
        }
    }
};

const dom = {
    rows: document.querySelector('#productRows'),
    form: document.querySelector('#productForm'),
    productEditorPanel: document.querySelector('#productEditorPanel'),
    statusText: document.querySelector('#statusText'),
    searchInput: document.querySelector('#searchInput'),
    unitFilter: document.querySelector('#unitFilter'),
    categoryFilter: document.querySelector('#categoryFilter'),
    taxFilter: document.querySelector('#taxFilter'),
    stockFilter: document.querySelector('#stockFilter'),
    roleBadge: document.querySelector('#roleBadge'),
    totalProducts: document.querySelector('#totalProducts'),
    availableProducts: document.querySelector('#availableProducts'),
    outOfStockProducts: document.querySelector('#outOfStockProducts'),
    themeButton: document.querySelector('#themeButton'),
    newProductButton: document.querySelector('#newProductButton'),
    productEditorTitle: document.querySelector('#productEditorTitle'),
    inventoryTitle: document.querySelector('#inventoryTitle'),
    deleteButton: document.querySelector('#deleteButton'),
    saveButton: document.querySelector('#saveButton'),
    logoutButton: document.querySelector('#logoutButton'),
    productId: document.querySelector('#productId'),
    nameInput: document.querySelector('#nameInput'),
    categoryInput: document.querySelector('#categoryInput'),
    priceInput: document.querySelector('#priceInput'),
    stockInput: document.querySelector('#stockInput'),
    unitInput: document.querySelector('#unitInput'),
    taxInput: document.querySelector('#taxInput'),
    imageInput: document.querySelector('#imageInput'),
    imageFile: document.querySelector('#imageFile'),
    imagePreview: document.querySelector('#imagePreview'),
    stockPanel: document.querySelector('#stockPanel'),
    selectedProductSummary: document.querySelector('#selectedProductSummary'),
    stockAddInput: document.querySelector('#stockAddInput'),
    stockAddButton: document.querySelector('#stockAddButton')
};

window.setThemeFromJavaFx = setTheme;
bindEvents();
initTheme();
init();

function bindEvents() {
    dom.newProductButton.addEventListener('click', clearForm);
    dom.deleteButton.addEventListener('click', deleteSelected);
    dom.stockAddButton.addEventListener('click', addStock);
    dom.themeButton.addEventListener('click', toggleTheme);
    dom.logoutButton.addEventListener('click', logout);
    dom.form.addEventListener('submit', saveProduct);
    dom.stockPanel.addEventListener('toggle', () => {
        if (dom.stockPanel.open && state.activeArea !== 'stock') {
            state.activeArea = 'stock';
            dom.productEditorPanel.open = false;
            setStatus('Warenzugang: Produkt auswählen und Menge eingeben.');
        }
    });
    dom.productEditorPanel.addEventListener('toggle', () => {
        if (dom.productEditorPanel.open && state.activeArea !== 'edit') {
            state.activeArea = 'edit';
            dom.stockPanel.open = false;
            setStatus('Produktdaten bearbeiten oder neues Produkt anlegen.');
        }
    });
    dom.imageFile.addEventListener('change', uploadImage);
    dom.imageInput.addEventListener('input', updateImagePreview);
    dom.stockAddInput.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            addStock();
        }
    });

    for (const input of [dom.searchInput, dom.unitFilter, dom.categoryFilter, dom.taxFilter, dom.stockFilter]) {
        input.addEventListener('input', renderRows);
        input.addEventListener('change', renderRows);
    }
    window.addEventListener('focus', () => loadProducts(true));
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) loadProducts(true);
    });
    window.setInterval(() => { if (!document.hidden) loadProducts(true); }, 30000);
}

window.refreshProducts = () => loadProducts(true);
window.focusAdminArea = async area => {
    if (!ready) {
        pendingArea = area;
        return;
    }
    await loadProducts(true);
    if (area === 'new') {
        state.activeArea = 'edit';
        clearForm();
        dom.stockPanel.open = false;
        dom.productEditorPanel.open = true;
        dom.productEditorPanel.scrollIntoView({behavior: 'smooth', block: 'start'});
        dom.nameInput.focus();
    } else if (area === 'stock') {
        state.activeArea = 'stock';
        dom.productEditorPanel.open = false;
        dom.stockPanel.open = true;
        window.scrollTo(0, 0);
        dom.stockPanel.classList.add('attention');
        window.setTimeout(() => dom.stockPanel.classList.remove('attention'), 1800);
        setStatus(state.selectedProduct
            ? `Warenzugang für ${state.selectedProduct.name}: Menge eingeben.`
            : 'Warenzugang: Produkt in der Liste auswählen.');
        if (state.selectedProduct) dom.stockAddInput.focus();
    } else {
        state.activeArea = 'inventory';
        dom.productEditorPanel.open = false;
        dom.stockPanel.open = false;
        dom.inventoryTitle.scrollIntoView({behavior: 'smooth', block: 'start'});
        document.querySelector('.list-section').classList.add('attention');
        window.setTimeout(() => document.querySelector('.list-section').classList.remove('attention'), 1800);
        dom.searchInput.focus();
        setStatus('Lagerbestand wird in der Produktliste angezeigt.');
    }
};

async function init() {
    try {
        await loadSession();
        applyPermissions();
        await loadProducts();
        ready = true;
        if (pendingArea) {
            const area = pendingArea;
            pendingArea = null;
            window.focusAdminArea(area);
        }
    } catch (error) {
        setStatus('Verwaltung konnte nicht geladen werden.');
    }
}

async function loadSession() {
    const response = await fetch(sessionUrl, {credentials: 'same-origin'});
    if (!response.ok || !isJsonResponse(response)) {
        window.location.href = '../login';
        throw new Error('Session fehlt.');
    }
    state.session = await response.json();
}

function applyPermissions() {
    const canManageProducts = hasPermission('manageProducts');
    const canBookStock = hasPermission('bookStock');

    dom.roleBadge.textContent = formatUserLabel();
    document.querySelector('.topbar h1').textContent = canManageProducts ? 'Verwaltung' : 'Warenzugang';
    dom.productEditorPanel.hidden = !canManageProducts;
    dom.deleteButton.hidden = !canManageProducts;
    dom.saveButton.hidden = !canManageProducts;
    dom.stockPanel.hidden = !canBookStock;
    if (!canManageProducts && canBookStock) {
        state.activeArea = 'stock';
        dom.stockPanel.open = true;
    }
    document.body.classList.toggle('stock-only', !canManageProducts && canBookStock);

    for (const element of [dom.nameInput, dom.categoryInput, dom.priceInput, dom.stockInput, dom.unitInput, dom.taxInput, dom.imageInput, dom.imageFile]) {
        element.disabled = !canManageProducts;
    }

    updateStockControls();
}

async function loadProducts(silent = false) {
    if (!silent) setStatus('Lade Produkte...');
    try {
        const response = await fetch(apiUrl, {credentials: 'same-origin'});
        if (!response.ok) {
            setStatus('Produkte konnten nicht geladen werden.');
            return;
        }
        state.products = await response.json();
    } catch (error) {
        setStatus('Verbindung zur Verwaltung fehlgeschlagen. Bitte Backend prüfen.');
        return;
    }
    renderOverview();
    if (state.selectedProduct) {
        state.selectedProduct = state.products.find(product => product.id === state.selectedProduct.id) || null;
    }
    renderRows();
    updateSelectedProduct();
    if (!silent) setStatus(`${state.products.length} Produkte geladen.`);
}

function renderOverview() {
    const available = state.products.filter(product => Number(product.lagerbestand) > 0).length;
    dom.totalProducts.textContent = String(state.products.length);
    dom.availableProducts.textContent = String(available);
    dom.outOfStockProducts.textContent = String(state.products.length - available);
}

function renderRows() {
    const filter = dom.searchInput.value.trim().toLowerCase();
    const unit = dom.unitFilter.value;
    const category = dom.categoryFilter.value;
    const tax = dom.taxFilter.value;
    const onlyStock = dom.stockFilter.checked;

    dom.rows.innerHTML = '';
    let sichtbar = 0;
    for (const product of state.products) {
        if (!matchesFilter(product, filter, unit, category, tax, onlyStock)) {
            continue;
        }

        const row = document.createElement('tr');
        row.tabIndex = 0;
        row.setAttribute('aria-label', `${product.name}, Bestand ${formatAmount(product.lagerbestand)} ${product.einheitLabel}`);
        if (state.selectedProduct && product.id === state.selectedProduct.id) {
            row.classList.add('selected');
        }
        row.innerHTML = `
            <td>${product.id}</td>
            <td>${escapeHtml(product.name)}</td>
            <td>${escapeHtml(product.kategorie || 'Sonstiges')}</td>
            <td>${formatMoney(product.preis)}</td>
            <td>${formatAmount(product.lagerbestand)}</td>
            <td>${escapeHtml(product.einheitLabel)}</td>
            <td>${formatAmount(product.steuerSatz)} %</td>
        `;
        row.addEventListener('click', () => selectProduct(product));
        row.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                selectProduct(product);
            }
        });
        dom.rows.appendChild(row);
        sichtbar++;
    }
    if (sichtbar === 0) {
        const row = document.createElement('tr');
        const cell = document.createElement('td');
        cell.colSpan = 7;
        cell.className = 'empty-row';
        cell.textContent = state.products.length ? 'Keine Produkte für diese Filter gefunden.' : 'Noch keine Produkte vorhanden.';
        row.appendChild(cell);
        dom.rows.appendChild(row);
    }
}

function matchesFilter(product, filter, unit, category, tax, onlyStock) {
    const searchable = `${product.id} ${product.name} ${product.kategorie || ''} ${product.einheit} ${product.einheitLabel} ${product.steuerSatz}`.toLowerCase();
    if (filter && !searchable.includes(filter)) {
        return false;
    }
    if (unit && product.einheit !== unit) {
        return false;
    }
    if (category && (product.kategorie || 'Sonstiges') !== category) return false;
    if (tax && Number(product.steuerSatz) !== Number(tax)) {
        return false;
    }
    return !onlyStock || product.lagerbestand > 0;
}

function selectProduct(product) {
    state.selectedProduct = product;
    fillProductForm(product);
    if (state.activeArea === 'stock') {
        dom.productEditorPanel.open = false;
        dom.stockPanel.open = true;
        setStatus(`Warenzugang für ${product.name}: Menge eingeben.`);
        dom.stockAddInput.focus();
    } else if (hasPermission('manageProducts')) {
        state.activeArea = 'edit';
        dom.productEditorPanel.open = true;
        dom.stockPanel.open = false;
        setStatus(`Produkt ${product.name} bearbeiten.`);
    }
    updateSelectedProduct();
    renderRows();
}

function fillProductForm(product) {
    dom.productId.value = product.id;
    dom.nameInput.value = product.name;
    dom.categoryInput.value = product.kategorie || 'Sonstiges';
    dom.priceInput.value = formatInput(product.preis);
    dom.stockInput.value = formatInput(product.lagerbestand);
    dom.unitInput.value = product.einheit;
    dom.taxInput.value = String(product.steuerSatz);
    dom.imageInput.value = product.bildPfad || '';
    dom.imageFile.value = '';
    updateImagePreview();
    dom.stockAddInput.value = '';
}

function updateSelectedProduct() {
    if (!state.selectedProduct) {
        dom.selectedProductSummary.textContent = 'Kein Produkt ausgewählt';
        dom.productEditorTitle.textContent = 'Neues Produkt';
        dom.newProductButton.hidden = true;
        dom.deleteButton.hidden = true;
        updateStockControls();
        return;
    }

    const product = state.selectedProduct;
    dom.productEditorTitle.textContent = 'Produkt bearbeiten: ' + product.name;
    dom.newProductButton.hidden = !hasPermission('manageProducts');
    dom.deleteButton.hidden = !hasPermission('manageProducts');
    dom.selectedProductSummary.innerHTML = `
        <strong>${escapeHtml(product.name)}</strong>
        <span>Aktueller Lagerbestand: ${formatAmount(product.lagerbestand)} ${escapeHtml(product.einheitLabel)}</span>
    `;
    updateStockControls();
}

function updateStockControls() {
    dom.stockAddInput.disabled = !hasPermission('bookStock') || !state.selectedProduct;
    dom.stockAddButton.disabled = !hasPermission('bookStock') || !state.selectedProduct;
}

function clearForm() {
    if (!hasPermission('manageProducts')) {
        setStatus('Nur Admins dürfen neue Produkte erfassen.');
        return;
    }
    state.selectedProduct = null;
    state.activeArea = 'edit';
    dom.stockPanel.open = false;
    dom.productEditorPanel.open = true;
    dom.form.reset();
    dom.productId.value = '';
    dom.unitInput.value = 'STUECK';
    dom.categoryInput.value = 'Sonstiges';
    dom.taxInput.value = '19';
    dom.stockAddInput.value = '';
    updateImagePreview();
    updateSelectedProduct();
    renderRows();
    setStatus('Neues Produkt erfassen.');
}

async function saveProduct(event) {
    event.preventDefault();
    if (state.imageUploading) {
        setStatus('Bitte warten, bis das Produktbild hochgeladen ist.');
        return;
    }
    if (!hasPermission('manageProducts')) {
        setStatus('Nur Admins dürfen Produkte speichern.');
        return;
    }

    const id = dom.productId.value;
    const payload = {
        name: dom.nameInput.value,
        kategorie: dom.categoryInput.value,
        preis: parseNumber(dom.priceInput.value),
        lagerbestand: parseNumber(dom.stockInput.value),
        einheit: dom.unitInput.value,
        steuerSatz: parseNumber(dom.taxInput.value),
        bildPfad: dom.imageInput.value
    };

    if (!Number.isFinite(payload.preis) || payload.preis <= 0) {
        setStatus('Preis muss eine Zahl größer als 0 sein.');
        dom.priceInput.focus();
        return;
    }
    if (!Number.isFinite(payload.lagerbestand) || payload.lagerbestand < 0) {
        setStatus('Lagerbestand muss eine Zahl ab 0 sein.');
        dom.stockInput.focus();
        return;
    }

    let response;
    try {
        response = await fetch(id ? `${apiUrl}/${id}` : apiUrl, {
            method: id ? 'PUT' : 'POST',
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
    } catch (error) {
        setStatus('Produkt konnte nicht gespeichert werden. Verbindung prüfen.');
        return;
    }

    if (!response.ok) {
        await showError(response);
        return;
    }
    const saved = await response.json();
    await loadProducts();
    const current = state.products.find(product => product.id === saved.id);
    if (current) {
        selectProduct(current);
    }
    setStatus('Produkt gespeichert.');
}

async function deleteSelected() {
    if (!hasPermission('manageProducts')) {
        setStatus('Nur Admins dürfen Produkte löschen.');
        return;
    }
    if (!state.selectedProduct) {
        setStatus('Kein Produkt ausgewählt.');
        return;
    }
    if (!confirm('Produkt wirklich löschen?')) {
        return;
    }

    let response;
    try {
        response = await fetch(`${apiUrl}/${state.selectedProduct.id}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });
    } catch (error) {
        setStatus('Produkt konnte nicht gelöscht werden. Verbindung prüfen.');
        return;
    }
    if (!response.ok) {
        await showError(response);
        return;
    }
    clearForm();
    await loadProducts();
    setStatus('Produkt gelöscht.');
}

async function addStock() {
    if (!hasPermission('bookStock')) {
        setStatus('Keine Berechtigung für Warenzugang.');
        return;
    }
    if (!state.selectedProduct) {
        setStatus('Bitte Produkt aus der Liste auswählen.');
        return;
    }

    const menge = parseNumber(dom.stockAddInput.value);
    if (!Number.isFinite(menge) || menge <= 0) {
        setStatus('Menge muss größer als 0 sein.');
        return;
    }

    let response;
    try {
        response = await fetch(`${apiUrl}/${state.selectedProduct.id}/warenzugang`, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({menge})
        });
    } catch (error) {
        setStatus('Warenzugang konnte nicht gebucht werden. Verbindung prüfen.');
        return;
    }
    if (!response.ok) {
        await showError(response);
        return;
    }
    const updated = await response.json();
    await loadProducts();
    selectProduct(updated);
    setStatus(`Warenzugang für ${updated.name} gebucht.`);
}

async function uploadImage() {
    const datei = dom.imageFile.files[0];
    if (!datei) return;
    const produktId = dom.productId.value;
    state.imageUploading = true;
    dom.saveButton.disabled = true;
    setStatus('Produktbild wird hochgeladen …');
    try {
        const daten = new FormData();
        daten.append('datei', datei);
        const response = await fetch('../api/bilder', {
            method: 'POST',
            credentials: 'same-origin',
            body: daten
        });
        if (!response.ok) {
            await showError(response);
            return;
        }
        const bild = await response.json();
        if (dom.productId.value === produktId) {
            dom.imageInput.value = bild.url;
            updateImagePreview();
            setStatus('Bild hochgeladen. Speichern Sie jetzt das Produkt.');
        }
    } catch (error) {
        setStatus('Bild konnte nicht hochgeladen werden. Verbindung prüfen.');
    } finally {
        state.imageUploading = false;
        dom.saveButton.disabled = !hasPermission('manageProducts');
    }
}

function updateImagePreview() {
    const pfad = dom.imageInput.value.trim();
    const url = pfad.startsWith('api/bilder/') ? `../${pfad}`
        : /^https?:\/\//i.test(pfad) ? pfad : '';
    dom.imagePreview.hidden = !url;
    if (url) dom.imagePreview.src = url;
    else dom.imagePreview.removeAttribute('src');
}

async function showError(response) {
    try {
        const body = await response.json();
        setStatus(body.message || 'Aktion fehlgeschlagen.');
    } catch (error) {
        setStatus('Aktion fehlgeschlagen.');
    }
}

function hasPermission(permission) {
    return Boolean(state.session.permissions && state.session.permissions[permission]);
}

function formatUserLabel() {
    const roles = Array.isArray(state.session.roles) ? state.session.roles.map(formatRole).join(', ') : '';
    return `${state.session.username || 'Benutzer'} - ${roles}`;
}

function formatRole(role) {
    if (role === 'ADMIN') {
        return 'Admin';
    }
    if (role === 'LAGERIST') {
        return 'Lagerist';
    }
    if (role === 'KASSIERER') {
        return 'Kassierer';
    }
    return role;
}

function parseNumber(value) {
    return Number(String(value || '').replace(',', '.'));
}

function formatInput(value) {
    return String(value).replace('.', ',');
}

function formatMoney(value) {
    return new Intl.NumberFormat('de-DE', {style: 'currency', currency: 'EUR'}).format(value);
}

function formatAmount(value) {
    return new Intl.NumberFormat('de-DE', {maximumFractionDigits: 3}).format(value);
}

function setStatus(text) {
    dom.statusText.textContent = text;
}

function initTheme() {
    const params = new URLSearchParams(window.location.search);
    const savedTheme = readThemeFromStorage();
    setTheme(params.get('theme') || savedTheme || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
}

function toggleTheme() {
    setTheme(document.body.classList.contains('dark-mode') ? 'light' : 'dark');
}

function setTheme(theme) {
    const darkMode = theme === 'dark';
    document.documentElement.classList.toggle('dark-mode', darkMode);
    document.body.classList.toggle('dark-mode', darkMode);
    dom.themeButton.textContent = darkMode ? 'Hellmodus einschalten' : 'Dunkelmodus einschalten';
    writeThemeToStorage(darkMode ? 'dark' : 'light');
}

function readThemeFromStorage() {
    try {
        return localStorage.getItem('kassensystem-theme');
    } catch (error) {
        return null;
    }
}

function writeThemeToStorage(theme) {
    try {
        localStorage.setItem('kassensystem-theme', theme);
    } catch (error) {
        // WebView kann lokalen Speicher je nach Umgebung blockieren.
    }
}

async function logout() {
    try {
        await fetch('../logout', {
            method: 'POST',
            credentials: 'same-origin'
        });
    } finally {
        window.location.href = '../login?logout';
    }
}

function isJsonResponse(response) {
    const contentType = response.headers.get('content-type') || '';
    return contentType.indexOf('application/json') >= 0;
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
