const apiUrl = '../api/produkte';
const sessionUrl = '../api/session';

const state = {
    products: [],
    selectedId: null,
    session: {
        username: '',
        roles: [],
        permissions: {
            manageProducts: false,
            bookStock: false
        }
    }
};

const rows = document.querySelector('#productRows');
const form = document.querySelector('#productForm');
const statusText = document.querySelector('#statusText');
const searchInput = document.querySelector('#searchInput');
const unitFilter = document.querySelector('#unitFilter');
const taxFilter = document.querySelector('#taxFilter');
const stockFilter = document.querySelector('#stockFilter');
const roleBadge = document.querySelector('#roleBadge');
const themeButton = document.querySelector('#themeButton');
const logoutButton = document.querySelector('#logoutButton');

document.querySelector('#reloadButton').addEventListener('click', loadProducts);
document.querySelector('#newButton').addEventListener('click', clearForm);
document.querySelector('#deleteButton').addEventListener('click', deleteSelected);
document.querySelector('#stockAddButton').addEventListener('click', addStock);
themeButton.addEventListener('click', toggleTheme);
logoutButton.addEventListener('click', logout);
form.addEventListener('submit', saveProduct);

for (const input of [searchInput, unitFilter, taxFilter, stockFilter]) {
    input.addEventListener('input', renderRows);
    input.addEventListener('change', renderRows);
}

window.setThemeFromJavaFx = setTheme;
initTheme();
init();

async function init() {
    await loadSession();
    applyPermissions();
    await loadProducts();
}

async function loadSession() {
    const response = await fetch(sessionUrl);
    if (!response.ok) {
        setStatus('Anmeldung konnte nicht geladen werden.');
        return;
    }
    state.session = await response.json();
}

function applyPermissions() {
    const canManageProducts = Boolean(state.session.permissions?.manageProducts);
    const canBookStock = Boolean(state.session.permissions?.bookStock);

    roleBadge.textContent = `${state.session.username} - ${state.session.roles.join(', ')}`;
    document.querySelector('#newButton').hidden = !canManageProducts;
    document.querySelector('#deleteButton').hidden = !canManageProducts;
    form.classList.toggle('readonly', !canManageProducts);
    form.querySelector('button[type="submit"]').hidden = !canManageProducts;

    for (const element of [
        '#nameInput',
        '#priceInput',
        '#stockInput',
        '#unitInput',
        '#taxInput',
        '#imageInput'
    ].map(selector => document.querySelector(selector))) {
        element.disabled = !canManageProducts;
    }

    document.querySelector('#stockAddInput').disabled = !canBookStock;
    document.querySelector('#stockAddButton').disabled = !canBookStock;
}

async function loadProducts() {
    setStatus('Lade Produkte...');
    const response = await fetch(apiUrl);
    if (!response.ok) {
        setStatus('Produkte konnten nicht geladen werden.');
        return;
    }
    state.products = await response.json();
    renderRows();
    setStatus(`${state.products.length} Produkte geladen.`);
}

function renderRows() {
    const filter = searchInput.value.trim().toLowerCase();
    const unit = unitFilter.value;
    const tax = taxFilter.value;
    const onlyStock = stockFilter.checked;

    rows.innerHTML = '';
    for (const product of state.products) {
        const searchable = `${product.id} ${product.name} ${product.einheit} ${product.einheitLabel} ${product.steuerSatz}`.toLowerCase();
        if (filter && !searchable.includes(filter)) {
            continue;
        }
        if (unit && product.einheit !== unit) {
            continue;
        }
        if (tax && Number(product.steuerSatz) !== Number(tax)) {
            continue;
        }
        if (onlyStock && product.lagerbestand <= 0) {
            continue;
        }

        const tr = document.createElement('tr');
        if (product.id === state.selectedId) {
            tr.classList.add('selected');
        }
        tr.innerHTML = `
            <td>${product.id}</td>
            <td>${escapeHtml(product.name)}</td>
            <td>${formatMoney(product.preis)}</td>
            <td>${formatAmount(product.lagerbestand)}</td>
            <td>${escapeHtml(product.einheitLabel)}</td>
            <td>${formatAmount(product.steuerSatz)} %</td>
        `;
        tr.addEventListener('click', () => selectProduct(product));
        rows.appendChild(tr);
    }
}

function selectProduct(product) {
    state.selectedId = product.id;
    document.querySelector('#productId').value = product.id;
    document.querySelector('#nameInput').value = product.name;
    document.querySelector('#priceInput').value = formatInput(product.preis);
    document.querySelector('#stockInput').value = formatInput(product.lagerbestand);
    document.querySelector('#unitInput').value = product.einheit;
    document.querySelector('#taxInput').value = String(product.steuerSatz);
    document.querySelector('#imageInput').value = product.bildPfad || '';
    document.querySelector('#stockAddInput').value = '';
    renderRows();
}

function clearForm() {
    if (!state.session.permissions?.manageProducts) {
        setStatus('Nur Admins duerfen neue Produkte erfassen.');
        return;
    }
    state.selectedId = null;
    form.reset();
    document.querySelector('#productId').value = '';
    document.querySelector('#unitInput').value = 'STUECK';
    document.querySelector('#taxInput').value = '19';
    document.querySelector('#stockAddInput').value = '';
    renderRows();
    setStatus('Neues Produkt.');
}

async function saveProduct(event) {
    event.preventDefault();
    if (!state.session.permissions?.manageProducts) {
        setStatus('Nur Admins duerfen Produkte speichern.');
        return;
    }
    const id = document.querySelector('#productId').value;
    const payload = {
        name: document.querySelector('#nameInput').value,
        preis: parseNumber(document.querySelector('#priceInput').value),
        lagerbestand: parseNumber(document.querySelector('#stockInput').value),
        einheit: document.querySelector('#unitInput').value,
        steuerSatz: parseNumber(document.querySelector('#taxInput').value),
        bildPfad: document.querySelector('#imageInput').value
    };

    const response = await fetch(id ? `${apiUrl}/${id}` : apiUrl, {
        method: id ? 'PUT' : 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        await showError(response);
        return;
    }
    const saved = await response.json();
    state.selectedId = saved.id;
    await loadProducts();
    const current = state.products.find(product => product.id === saved.id);
    if (current) {
        selectProduct(current);
    }
    setStatus('Produkt gespeichert.');
}

async function deleteSelected() {
    if (!state.session.permissions?.manageProducts) {
        setStatus('Nur Admins duerfen Produkte loeschen.');
        return;
    }
    const id = document.querySelector('#productId').value;
    if (!id) {
        setStatus('Kein Produkt ausgewaehlt.');
        return;
    }
    if (!confirm('Produkt wirklich loeschen?')) {
        return;
    }

    const response = await fetch(`${apiUrl}/${id}`, {method: 'DELETE'});
    if (!response.ok) {
        await showError(response);
        return;
    }
    clearForm();
    await loadProducts();
    setStatus('Produkt geloescht.');
}

async function addStock() {
    if (!state.session.permissions?.bookStock) {
        setStatus('Keine Berechtigung fuer Warenzugang.');
        return;
    }
    const id = document.querySelector('#productId').value;
    if (!id) {
        setStatus('Kein Produkt ausgewaehlt.');
        return;
    }
    const menge = parseNumber(document.querySelector('#stockAddInput').value);
    const response = await fetch(`${apiUrl}/${id}/warenzugang`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({menge})
    });
    if (!response.ok) {
        await showError(response);
        return;
    }
    const updated = await response.json();
    await loadProducts();
    selectProduct(updated);
    setStatus('Warenzugang gebucht.');
}

async function showError(response) {
    try {
        const body = await response.json();
        setStatus(body.message || 'Aktion fehlgeschlagen.');
    } catch {
        setStatus('Aktion fehlgeschlagen.');
    }
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
    statusText.textContent = text;
}

function initTheme() {
    const params = new URLSearchParams(window.location.search);
    const requestedTheme = params.get('theme') || localStorage.getItem('kassensystem-theme') || 'light';
    setTheme(requestedTheme);
}

function toggleTheme() {
    setTheme(document.body.classList.contains('dark-mode') ? 'light' : 'dark');
}

function setTheme(theme) {
    const darkMode = theme === 'dark';
    document.body.classList.toggle('dark-mode', darkMode);
    themeButton.textContent = darkMode ? 'Hellmodus' : 'Darkmode';
    localStorage.setItem('kassensystem-theme', darkMode ? 'dark' : 'light');
}

async function logout() {
    await fetch('../logout', {method: 'POST'});
    window.location.href = '../login?logout';
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
