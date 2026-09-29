const apiUrl = '../api/produkte';
const sessionUrl = '../api/session';

const state = {
    products: [],
    selectedProduct: null,
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
    taxFilter: document.querySelector('#taxFilter'),
    stockFilter: document.querySelector('#stockFilter'),
    roleBadge: document.querySelector('#roleBadge'),
    totalProducts: document.querySelector('#totalProducts'),
    availableProducts: document.querySelector('#availableProducts'),
    outOfStockProducts: document.querySelector('#outOfStockProducts'),
    themeButton: document.querySelector('#themeButton'),
    reloadButton: document.querySelector('#reloadButton'),
    newButton: document.querySelector('#newButton'),
    deleteButton: document.querySelector('#deleteButton'),
    saveButton: document.querySelector('#saveButton'),
    logoutButton: document.querySelector('#logoutButton'),
    productId: document.querySelector('#productId'),
    nameInput: document.querySelector('#nameInput'),
    priceInput: document.querySelector('#priceInput'),
    stockInput: document.querySelector('#stockInput'),
    unitInput: document.querySelector('#unitInput'),
    taxInput: document.querySelector('#taxInput'),
    imageInput: document.querySelector('#imageInput'),
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
    dom.reloadButton.addEventListener('click', loadProducts);
    dom.newButton.addEventListener('click', clearForm);
    dom.deleteButton.addEventListener('click', deleteSelected);
    dom.stockAddButton.addEventListener('click', addStock);
    dom.themeButton.addEventListener('click', toggleTheme);
    dom.logoutButton.addEventListener('click', logout);
    dom.form.addEventListener('submit', saveProduct);
    dom.stockAddInput.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            addStock();
        }
    });

    for (const input of [dom.searchInput, dom.unitFilter, dom.taxFilter, dom.stockFilter]) {
        input.addEventListener('input', renderRows);
        input.addEventListener('change', renderRows);
    }
}

async function init() {
    try {
        await loadSession();
        applyPermissions();
        await loadProducts();
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
    dom.newButton.hidden = !canManageProducts;
    dom.productEditorPanel.hidden = !canManageProducts;
    dom.deleteButton.hidden = !canManageProducts;
    dom.saveButton.hidden = !canManageProducts;
    dom.stockPanel.hidden = !canBookStock;
    document.body.classList.toggle('stock-only', !canManageProducts && canBookStock);

    for (const element of [dom.nameInput, dom.priceInput, dom.stockInput, dom.unitInput, dom.taxInput, dom.imageInput]) {
        element.disabled = !canManageProducts;
    }

    updateStockControls();
}

async function loadProducts() {
    setStatus('Lade Produkte...');
    try {
        const response = await fetch(apiUrl, {credentials: 'same-origin'});
        if (!response.ok) {
            setStatus('Produkte konnten nicht geladen werden.');
            return;
        }
        state.products = await response.json();
    } catch (error) {
        setStatus('Verbindung zur Verwaltung fehlgeschlagen. Bitte Backend pruefen.');
        return;
    }
    renderOverview();
    if (state.selectedProduct) {
        state.selectedProduct = state.products.find(product => product.id === state.selectedProduct.id) || null;
    }
    renderRows();
    updateSelectedProduct();
    setStatus(`${state.products.length} Produkte geladen.`);
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
    const tax = dom.taxFilter.value;
    const onlyStock = dom.stockFilter.checked;

    dom.rows.innerHTML = '';
    let sichtbar = 0;
    for (const product of state.products) {
        if (!matchesFilter(product, filter, unit, tax, onlyStock)) {
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
        cell.colSpan = 6;
        cell.className = 'empty-row';
        cell.textContent = state.products.length ? 'Keine Produkte fuer diese Filter gefunden.' : 'Noch keine Produkte vorhanden.';
        row.appendChild(cell);
        dom.rows.appendChild(row);
    }
}

function matchesFilter(product, filter, unit, tax, onlyStock) {
    const searchable = `${product.id} ${product.name} ${product.einheit} ${product.einheitLabel} ${product.steuerSatz}`.toLowerCase();
    if (filter && !searchable.includes(filter)) {
        return false;
    }
    if (unit && product.einheit !== unit) {
        return false;
    }
    if (tax && Number(product.steuerSatz) !== Number(tax)) {
        return false;
    }
    return !onlyStock || product.lagerbestand > 0;
}

function selectProduct(product) {
    state.selectedProduct = product;
    fillProductForm(product);
    updateSelectedProduct();
    renderRows();
}

function fillProductForm(product) {
    dom.productId.value = product.id;
    dom.nameInput.value = product.name;
    dom.priceInput.value = formatInput(product.preis);
    dom.stockInput.value = formatInput(product.lagerbestand);
    dom.unitInput.value = product.einheit;
    dom.taxInput.value = String(product.steuerSatz);
    dom.imageInput.value = product.bildPfad || '';
    dom.stockAddInput.value = '';
}

function updateSelectedProduct() {
    if (!state.selectedProduct) {
        dom.selectedProductSummary.textContent = 'Kein Produkt ausgewaehlt';
        updateStockControls();
        return;
    }

    const product = state.selectedProduct;
    dom.selectedProductSummary.innerHTML = `
        <strong>${escapeHtml(product.name)}</strong>
        <span>Lager: ${formatAmount(product.lagerbestand)} ${escapeHtml(product.einheitLabel)}</span>
        <span>Preis: ${formatMoney(product.preis)} / ${escapeHtml(product.einheitLabel)}</span>
    `;
    updateStockControls();
}

function updateStockControls() {
    dom.stockAddInput.disabled = !hasPermission('bookStock') || !state.selectedProduct;
    dom.stockAddButton.disabled = !hasPermission('bookStock') || !state.selectedProduct;
}

function clearForm() {
    if (!hasPermission('manageProducts')) {
        setStatus('Nur Admins duerfen neue Produkte erfassen.');
        return;
    }
    state.selectedProduct = null;
    dom.form.reset();
    dom.productId.value = '';
    dom.unitInput.value = 'STUECK';
    dom.taxInput.value = '19';
    dom.stockAddInput.value = '';
    updateSelectedProduct();
    renderRows();
    setStatus('Neues Produkt.');
}

async function saveProduct(event) {
    event.preventDefault();
    if (!hasPermission('manageProducts')) {
        setStatus('Nur Admins duerfen Produkte speichern.');
        return;
    }

    const id = dom.productId.value;
    const payload = {
        name: dom.nameInput.value,
        preis: parseNumber(dom.priceInput.value),
        lagerbestand: parseNumber(dom.stockInput.value),
        einheit: dom.unitInput.value,
        steuerSatz: parseNumber(dom.taxInput.value),
        bildPfad: dom.imageInput.value
    };

    if (!Number.isFinite(payload.preis) || payload.preis <= 0) {
        setStatus('Preis muss eine Zahl groesser als 0 sein.');
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
        setStatus('Produkt konnte nicht gespeichert werden. Verbindung pruefen.');
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
        setStatus('Nur Admins duerfen Produkte loeschen.');
        return;
    }
    if (!state.selectedProduct) {
        setStatus('Kein Produkt ausgewaehlt.');
        return;
    }
    if (!confirm('Produkt wirklich loeschen?')) {
        return;
    }

    let response;
    try {
        response = await fetch(`${apiUrl}/${state.selectedProduct.id}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });
    } catch (error) {
        setStatus('Produkt konnte nicht geloescht werden. Verbindung pruefen.');
        return;
    }
    if (!response.ok) {
        await showError(response);
        return;
    }
    clearForm();
    await loadProducts();
    setStatus('Produkt geloescht.');
}

async function addStock() {
    if (!hasPermission('bookStock')) {
        setStatus('Keine Berechtigung fuer Warenzugang.');
        return;
    }
    if (!state.selectedProduct) {
        setStatus('Bitte Produkt aus der Liste auswaehlen.');
        return;
    }

    const menge = parseNumber(dom.stockAddInput.value);
    if (!Number.isFinite(menge) || menge <= 0) {
        setStatus('Menge muss groesser als 0 sein.');
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
        setStatus('Warenzugang konnte nicht gebucht werden. Verbindung pruefen.');
        return;
    }
    if (!response.ok) {
        await showError(response);
        return;
    }
    const updated = await response.json();
    await loadProducts();
    selectProduct(updated);
    setStatus(`Warenzugang fuer ${updated.name} gebucht.`);
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
    setTheme(params.get('theme') || savedTheme || 'light');
}

function toggleTheme() {
    setTheme(document.body.classList.contains('dark-mode') ? 'light' : 'dark');
}

function setTheme(theme) {
    const darkMode = theme === 'dark';
    document.documentElement.classList.toggle('dark-mode', darkMode);
    document.body.classList.toggle('dark-mode', darkMode);
    dom.themeButton.textContent = darkMode ? 'Hellmodus' : 'Darkmode';
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
