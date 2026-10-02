const apiUrl = '../api/produkte';
const sessionUrl = '../api/session';
let ready = false;
let pendingArea = null;

const state = {
    products: [],
    selectedProduct: null,
    activeArea: 'edit',
    imageUploading: false,
    users: [],
    selectedUserId: null,
    permissionCatalog: null,
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

Object.assign(dom, {
    productsView: document.querySelector('#productsView'), usersView: document.querySelector('#usersView'),
    productsNav: document.querySelector('#productsNav'), usersNav: document.querySelector('#usersNav'),
    userList: document.querySelector('#userList'), userSearch: document.querySelector('#userSearch'),
    newUserButton: document.querySelector('#newUserButton'), userForm: document.querySelector('#userForm'),
    userFormTitle: document.querySelector('#userFormTitle'), userLogin: document.querySelector('#userLogin'),
    userDisplay: document.querySelector('#userDisplay'), userPassword: document.querySelector('#userPassword'),
    userPasswordLabel: document.querySelector('#userPasswordLabel'), userActive: document.querySelector('#userActive'),
    userRoles: document.querySelector('#userRoles'), userOverrides: document.querySelector('#userOverrides'),
    effectiveRights: document.querySelector('#effectiveRights'), userStatus: document.querySelector('#userStatus'),
    resetPasswordButton: document.querySelector('#resetPasswordButton'),
    changePasswordDialog: document.querySelector('#changePasswordDialog'),
    changePasswordForm: document.querySelector('#changePasswordForm'),
    oldPassword: document.querySelector('#oldPassword'), newPassword: document.querySelector('#newPassword'),
    repeatPassword: document.querySelector('#repeatPassword'), passwordStatus: document.querySelector('#passwordStatus')
});

const customSelects = new Map();
let openCustomSelect = null;

window.setThemeFromJavaFx = setTheme;
enhanceSelects();
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
    dom.productsNav.addEventListener('click', () => showArea('products'));
    dom.usersNav.addEventListener('click', () => showArea('users'));
    dom.newUserButton.addEventListener('click', clearUserForm);
    dom.userSearch.addEventListener('input', renderUsers);
    dom.userForm.addEventListener('submit', saveUser);
    dom.resetPasswordButton.addEventListener('click', resetUserPassword);
    dom.changePasswordForm.addEventListener('submit', changeOwnPassword);
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
        if (state.session.mustChangePassword) {
            if (typeof dom.changePasswordDialog.showModal === 'function') dom.changePasswordDialog.showModal();
            else dom.changePasswordDialog.setAttribute('open', '');
            return;
        }
        if (hasPermission('products.read')) await loadProducts();
        if (hasPermission('users.manage')) await loadUserCatalog();
        showArea(hasPermission('products.read') ? 'products' : 'users');
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
    const canManageUsers = hasPermission('users.manage');

    dom.roleBadge.textContent = formatUserLabel();
    document.querySelector('.topbar h1').textContent = canManageProducts ? 'Verwaltung' : 'Warenzugang';
    dom.productEditorPanel.hidden = !canManageProducts;
    dom.deleteButton.hidden = !canManageProducts;
    dom.saveButton.hidden = !canManageProducts;
    dom.stockPanel.hidden = !canBookStock;
    dom.usersNav.hidden = !canManageUsers;
    dom.productsNav.hidden = !hasPermission('products.read');
    if (!canManageProducts && canBookStock) {
        state.activeArea = 'stock';
        dom.stockPanel.open = true;
    }
    document.body.classList.toggle('stock-only', !canManageProducts && canBookStock);

    for (const element of [dom.nameInput, dom.categoryInput, dom.priceInput, dom.stockInput, dom.unitInput, dom.taxInput, dom.imageInput, dom.imageFile]) {
        element.disabled = !canManageProducts;
    }

    updateStockControls();
    syncCustomSelects();
}

async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.method && !['GET', 'HEAD'].includes(options.method.toUpperCase())) {
        headers.set('X-CSRF-TOKEN', state.session.csrfToken || '');
    }
    const response = await fetch(url, {...options, credentials: 'same-origin', headers});
    if (response.status === 401) window.location.href = '../login?session-expired';
    return response;
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
    syncCustomSelects();
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
    syncCustomSelects();
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
        response = await apiFetch(id ? `${apiUrl}/${id}` : apiUrl, {
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
        response = await apiFetch(`${apiUrl}/${state.selectedProduct.id}`, {
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
        response = await apiFetch(`${apiUrl}/${state.selectedProduct.id}/warenzugang`, {
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
        const response = await apiFetch('../api/bilder', {
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

function enhanceSelects() {
    for (const select of document.querySelectorAll('select')) {
        const container = document.createElement('span');
        container.className = 'custom-select';
        select.before(container);
        container.appendChild(select);
        select.hidden = true;

        const trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'custom-select-trigger';
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');
        trigger.setAttribute('aria-label', select.getAttribute('aria-label')
            || select.closest('label')?.textContent.trim().split('\n')[0].trim()
            || 'Auswahl');
        const value = document.createElement('span');
        value.className = 'custom-select-value';
        const arrow = document.createElement('span');
        arrow.className = 'custom-select-arrow';
        arrow.setAttribute('aria-hidden', 'true');
        arrow.textContent = '▾';
        trigger.append(value, arrow);
        container.appendChild(trigger);

        const menu = document.createElement('div');
        menu.className = 'custom-select-menu';
        menu.id = `${select.id}-menu`;
        menu.setAttribute('role', 'listbox');
        menu.hidden = true;
        trigger.setAttribute('aria-controls', menu.id);
        document.body.appendChild(menu);

        const optionButtons = Array.from(select.options, (option, index) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'custom-select-option';
            button.textContent = option.textContent;
            button.id = `${select.id}-option-${index}`;
            button.tabIndex = -1;
            button.setAttribute('role', 'option');
            button.addEventListener('click', () => chooseCustomOption(select, index));
            menu.appendChild(button);
            return button;
        });

        const control = {select, trigger, value, menu, optionButtons, activeIndex: select.selectedIndex};
        customSelects.set(select, control);
        trigger.addEventListener('click', () => {
            if (openCustomSelect === control) closeCustomSelect();
            else showCustomSelect(control);
        });
        trigger.addEventListener('keydown', event => handleCustomSelectKey(event, control));
        select.addEventListener('change', () => syncCustomSelect(control));
        syncCustomSelect(control);
    }

    document.addEventListener('mousedown', event => {
        if (openCustomSelect && !openCustomSelect.trigger.contains(event.target)
                && !openCustomSelect.menu.contains(event.target)) closeCustomSelect();
    });
    document.addEventListener('scroll', event => {
        if (openCustomSelect && !openCustomSelect.menu.contains(event.target)) closeCustomSelect();
    }, true);
    window.addEventListener('resize', () => closeCustomSelect());
}

function syncCustomSelects() {
    for (const control of customSelects.values()) syncCustomSelect(control);
}

function syncCustomSelect(control) {
    const {select, trigger, value, optionButtons} = control;
    value.textContent = select.selectedOptions[0]?.textContent || 'Bitte wählen';
    trigger.disabled = select.disabled;
    if (select.disabled && openCustomSelect === control) closeCustomSelect();
    optionButtons.forEach((button, index) => {
        const selected = index === select.selectedIndex;
        button.setAttribute('aria-selected', String(selected));
        button.classList.toggle('selected', selected);
    });
}

function showCustomSelect(control) {
    closeCustomSelect();
    openCustomSelect = control;
    control.activeIndex = Math.max(0, control.select.selectedIndex);
    control.menu.hidden = false;
    control.trigger.setAttribute('aria-expanded', 'true');

    const rect = control.trigger.getBoundingClientRect();
    const below = window.innerHeight - rect.bottom - 8;
    const above = rect.top - 8;
    const openAbove = below < 160 && above > below;
    control.menu.style.left = `${rect.left}px`;
    control.menu.style.width = `${rect.width}px`;
    control.menu.style.maxHeight = `${Math.max(60, Math.min(240, openAbove ? above : below))}px`;
    control.menu.style.top = `${openAbove ? rect.top - control.menu.offsetHeight - 4 : rect.bottom + 4}px`;
    highlightCustomOption(control);
}

function closeCustomSelect() {
    if (!openCustomSelect) return;
    openCustomSelect.menu.hidden = true;
    openCustomSelect.trigger.setAttribute('aria-expanded', 'false');
    openCustomSelect.trigger.removeAttribute('aria-activedescendant');
    openCustomSelect = null;
}

function chooseCustomOption(select, index) {
    const control = customSelects.get(select);
    select.selectedIndex = index;
    select.dispatchEvent(new Event('change', {bubbles: true}));
    closeCustomSelect();
    control.trigger.focus();
}

function highlightCustomOption(control) {
    control.optionButtons.forEach((button, index) => button.classList.toggle('active', index === control.activeIndex));
    const active = control.optionButtons[control.activeIndex];
    if (active) {
        control.trigger.setAttribute('aria-activedescendant', active.id);
        if (active.offsetTop < control.menu.scrollTop) {
            control.menu.scrollTop = active.offsetTop;
        } else if (active.offsetTop + active.offsetHeight > control.menu.scrollTop + control.menu.clientHeight) {
            control.menu.scrollTop = active.offsetTop + active.offsetHeight - control.menu.clientHeight;
        }
    }
}

function handleCustomSelectKey(event, control) {
    const key = event.key;
    if (key === 'Escape' || key === 'Tab') {
        closeCustomSelect();
        if (key === 'Escape') event.preventDefault();
        return;
    }
    if (key === 'ArrowDown' || key === 'ArrowUp' || key === 'Home' || key === 'End') {
        event.preventDefault();
        if (openCustomSelect !== control) showCustomSelect(control);
        if (key === 'ArrowDown') control.activeIndex = Math.min(control.activeIndex + 1, control.optionButtons.length - 1);
        if (key === 'ArrowUp') control.activeIndex = Math.max(control.activeIndex - 1, 0);
        if (key === 'Home') control.activeIndex = 0;
        if (key === 'End') control.activeIndex = control.optionButtons.length - 1;
        highlightCustomOption(control);
        return;
    }
    if (key === 'Enter' || key === ' ') {
        event.preventDefault();
        if (openCustomSelect === control) chooseCustomOption(control.select, control.activeIndex);
        else showCustomSelect(control);
        return;
    }
    if (key.length === 1) {
        const index = control.optionButtons.findIndex(button => button.textContent.toLocaleLowerCase('de')
            .startsWith(key.toLocaleLowerCase('de')));
        if (index >= 0) {
            if (openCustomSelect !== control) showCustomSelect(control);
            control.activeIndex = index;
            highlightCustomOption(control);
        }
    }
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
        await apiFetch('../logout', {
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

function showArea(area) {
    const users = area === 'users' && hasPermission('users.manage');
    dom.productsView.hidden = users || !hasPermission('products.read');
    dom.usersView.hidden = !users;
    dom.productsNav.classList.toggle('active', !users);
    dom.usersNav.classList.toggle('active', users);
    if (users) loadUsers();
}

async function loadUserCatalog() {
    const response = await apiFetch('../api/admin/permissions');
    if (!response.ok) throw new Error('Rechte konnten nicht geladen werden.');
    state.permissionCatalog = await response.json();
    dom.userRoles.replaceChildren();
    for (const role of state.permissionCatalog.roles) {
        const label = document.createElement('label');
        const input = document.createElement('input');
        input.type = 'checkbox'; input.value = role; input.name = 'userRole';
        input.addEventListener('change', updateEffectiveRights);
        label.append(input, document.createTextNode(formatRole(role)));
        dom.userRoles.appendChild(label);
    }
    dom.userOverrides.replaceChildren();
    for (const permission of state.permissionCatalog.permissions) {
        const row = document.createElement('div');
        row.className = 'permission-row';
        const title = document.createElement('strong');
        title.textContent = permission;
        row.append(title);
        for (const [value, labelText] of [['default', 'Standard'], ['allow', 'Erlauben'], ['deny', 'Verweigern']]) {
            const label = document.createElement('label');
            const input = document.createElement('input');
            input.type = 'radio'; input.name = `override_${permission}`; input.value = value;
            input.checked = value === 'default';
            input.addEventListener('change', updateEffectiveRights);
            label.append(input, document.createTextNode(labelText));
            row.append(label);
        }
        dom.userOverrides.append(row);
    }
    clearUserForm();
    await loadUsers();
}

async function loadUsers() {
    if (!hasPermission('users.manage')) return;
    const response = await apiFetch('../api/admin/users');
    if (!response.ok) { dom.userStatus.textContent = 'Benutzer konnten nicht geladen werden.'; return; }
    state.users = await response.json();
    renderUsers();
}

function renderUsers() {
    dom.userList.replaceChildren();
    const search = dom.userSearch.value.trim().toLocaleLowerCase('de');
    for (const user of state.users.filter(item => `${item.username} ${item.displayName}`.toLocaleLowerCase('de').includes(search))) {
        const button = document.createElement('button');
        button.type = 'button'; button.setAttribute('role', 'listitem');
        button.classList.toggle('selected', state.selectedUserId === user.id);
        button.textContent = `${user.displayName} (${user.username})`;
        const detail = document.createElement('small');
        detail.textContent = `${user.roles.map(formatRole).join(', ')} · ${user.active ? 'Aktiv' : 'Gesperrt'}`;
        button.append(detail);
        button.addEventListener('click', () => selectUser(user));
        dom.userList.append(button);
    }
    if (!dom.userList.childElementCount) dom.userList.textContent = 'Keine Benutzer gefunden.';
}

function clearUserForm() {
    state.selectedUserId = null;
    dom.userForm.reset();
    dom.userFormTitle.textContent = 'Neuer Benutzer';
    dom.userLogin.disabled = false;
    dom.userPassword.required = true;
    dom.userPasswordLabel.firstChild.textContent = 'Startpasswort';
    dom.resetPasswordButton.hidden = true;
    dom.userActive.checked = true;
    for (const input of dom.userRoles.querySelectorAll('input')) input.checked = input.value === 'KASSIERER';
    for (const input of dom.userOverrides.querySelectorAll('input[value="default"]')) input.checked = true;
    dom.userStatus.textContent = '';
    updateEffectiveRights();
    renderUsers();
}

function selectUser(user) {
    state.selectedUserId = user.id;
    dom.userFormTitle.textContent = `Benutzer bearbeiten: ${user.displayName}`;
    dom.userLogin.value = user.username; dom.userLogin.disabled = true;
    dom.userDisplay.value = user.displayName;
    dom.userPassword.value = ''; dom.userPassword.required = false;
    dom.userPasswordLabel.firstChild.textContent = 'Neues Startpasswort für Zurücksetzen';
    dom.userActive.checked = user.active;
    dom.resetPasswordButton.hidden = false;
    for (const input of dom.userRoles.querySelectorAll('input')) input.checked = user.roles.includes(input.value);
    for (const permission of state.permissionCatalog.permissions) {
        const value = user.overrides[permission] === true ? 'allow'
            : user.overrides[permission] === false ? 'deny' : 'default';
        dom.userOverrides.querySelector(`input[name="override_${permission}"][value="${value}"]`).checked = true;
    }
    dom.userStatus.textContent = user.mustChangePassword ? 'Passwortwechsel beim nächsten Login erforderlich.' : '';
    updateEffectiveRights();
    renderUsers();
}

function chosenRoles() {
    return Array.from(dom.userRoles.querySelectorAll('input:checked'), input => input.value);
}

function chosenOverrides() {
    const overrides = {};
    for (const permission of state.permissionCatalog.permissions) {
        const value = dom.userOverrides.querySelector(`input[name="override_${permission}"]:checked`)?.value;
        if (value === 'allow') overrides[permission] = true;
        if (value === 'deny') overrides[permission] = false;
    }
    return overrides;
}

function updateEffectiveRights() {
    if (!state.permissionCatalog) return;
    const roles = chosenRoles();
    const overrides = chosenOverrides();
    const granted = state.permissionCatalog.permissions.filter(permission =>
        Object.prototype.hasOwnProperty.call(overrides, permission) ? overrides[permission]
            : roles.some(role => (state.permissionCatalog.roleDefaults[role] || []).includes(permission)));
    dom.effectiveRights.textContent = `Wirksame Rechte: ${granted.length ? granted.join(', ') : 'keine'}`;
}

async function saveUser(event) {
    event.preventDefault();
    const roles = chosenRoles();
    if (!roles.length) { dom.userStatus.textContent = 'Mindestens eine Rolle auswählen.'; return; }
    const payload = {username: dom.userLogin.value, displayName: dom.userDisplay.value,
        active: dom.userActive.checked, roles, overrides: chosenOverrides()};
    if (!state.selectedUserId) payload.password = dom.userPassword.value;
    const response = await apiFetch(state.selectedUserId ? `../api/admin/users/${state.selectedUserId}` : '../api/admin/users', {
        method: state.selectedUserId ? 'PATCH' : 'POST',
        headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });
    if (!response.ok) { dom.userStatus.textContent = await responseMessage(response); return; }
    const saved = await response.json();
    await loadUsers();
    selectUser(saved);
    dom.userStatus.textContent = 'Benutzer gespeichert.';
}

async function resetUserPassword() {
    const password = dom.userPassword.value;
    if (!state.selectedUserId || password.length < 10) {
        dom.userStatus.textContent = 'Neues Startpasswort mit mindestens 10 Zeichen eingeben.';
        return;
    }
    const response = await apiFetch(`../api/admin/users/${state.selectedUserId}/password-reset`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({password})
    });
    if (!response.ok) { dom.userStatus.textContent = await responseMessage(response); return; }
    dom.userPassword.value = '';
    dom.userStatus.textContent = 'Passwort zurückgesetzt. Der Benutzer muss es beim nächsten Login ändern.';
    await loadUsers();
}

async function changeOwnPassword(event) {
    event.preventDefault();
    if (dom.newPassword.value !== dom.repeatPassword.value || dom.newPassword.value.length < 10) {
        dom.passwordStatus.textContent = 'Mindestens 10 Zeichen; beide Eingaben müssen übereinstimmen.';
        return;
    }
    const response = await apiFetch('../api/account/password', {method: 'POST',
        headers: {'Content-Type': 'application/json'}, body: JSON.stringify({
            oldPassword: dom.oldPassword.value, newPassword: dom.newPassword.value})});
    if (!response.ok) { dom.passwordStatus.textContent = await responseMessage(response); return; }
    window.location.href = '../login?password-changed';
}

async function responseMessage(response) {
    try { return (await response.json()).message || `Aktion fehlgeschlagen (HTTP ${response.status}).`; }
    catch (error) { return `Aktion fehlgeschlagen (HTTP ${response.status}).`; }
}
