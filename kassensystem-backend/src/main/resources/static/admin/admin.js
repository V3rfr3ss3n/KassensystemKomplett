import {createApiFetch, isJsonResponse, escapeHtml} from './admin-api.js';
import {createLabelSelection} from './admin-products.js';
import {createUserManagement} from './admin-users.js';
import {createProductManagement} from './admin-product-management.js';
import {createCameraScanner} from './admin-camera-scan.js';

const apiUrl = '../api/produkte';
const sessionUrl = '../api/session';
const permissionLabels = {
    'products.read': 'Produkte ansehen',
    'products.manage': 'Produkte verwalten',
    'stock.book': 'Warenzugang buchen',
    'sales.create': 'Verkäufe abschließen',
    'receipts.read': 'Bons ansehen',
    'users.manage': 'Benutzer verwalten'
};
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
const apiFetch = createApiFetch(() => state.session);

const dom = {
    rows: document.querySelector('#productRows'),
    selectVisibleProducts: document.querySelector('#selectVisibleProducts'),
    downloadLabelsButton: document.querySelector('#downloadLabelsButton'),
    scanCodeInput: document.querySelector('#scanCodeInput'),
    form: document.querySelector('#productForm'),
    productEditorPanel: document.querySelector('#productEditorPanel'),
    statusText: document.querySelector('#statusText'),
    searchInput: document.querySelector('#searchInput'),
    cameraScanButton: document.querySelector('#cameraScanButton'),
    cameraScanPanel: document.querySelector('#cameraScanPanel'),
    cameraScanVideo: document.querySelector('#cameraScanVideo'),
    cameraScanStatus: document.querySelector('#cameraScanStatus'),
    cameraScanClose: document.querySelector('#cameraScanClose'),
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
    areaTitle: document.querySelector('#areaTitle'), areaSubtitle: document.querySelector('#areaSubtitle'),
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
const products = createProductManagement({state, dom, apiFetch, apiUrl, hasPermission, setStatus,
    syncCustomSelects, parseNumber, formatInput, formatMoney, formatAmount, escapeHtml, getLabels: () => labels});
const labels = createLabelSelection({state, dom, apiFetch, matchesFilter: products.matchesFilter,
    setStatus, showError: products.showError});
const users = createUserManagement({state, dom, apiFetch, hasPermission, permissionLabels, formatRole});
const cameraScanner = createCameraScanner({dom, apiFetch, onCode: selectScannedProduct, setStatus});

const customSelects = new Map();
let openCustomSelect = null;

window.setThemeFromJavaFx = setTheme;
enhanceSelects();
bindEvents();
initTheme();
init();

function bindEvents() {
    dom.newProductButton.addEventListener('click', products.clearForm);
    dom.deleteButton.addEventListener('click', products.deleteSelected);
    dom.stockAddButton.addEventListener('click', products.addStock);
    dom.themeButton.addEventListener('click', toggleTheme);
    dom.logoutButton.addEventListener('click', logout);
    dom.form.addEventListener('submit', products.saveProduct);
    dom.downloadLabelsButton.addEventListener('click', labels.download);
    dom.cameraScanButton.addEventListener('click', cameraScanner.start);
    dom.selectVisibleProducts.addEventListener('change', () => {
        labels.selectVisible();
        products.renderRows();
    });
    dom.productsNav.addEventListener('click', () => showArea('products'));
    dom.usersNav.addEventListener('click', () => showArea('users'));
    dom.newUserButton.addEventListener('click', users.clearUserForm);
    dom.userSearch.addEventListener('input', users.renderUsers);
    dom.userForm.addEventListener('submit', users.saveUser);
    dom.resetPasswordButton.addEventListener('click', users.resetUserPassword);
    dom.changePasswordForm.addEventListener('submit', users.changeOwnPassword);
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
    dom.imageFile.addEventListener('change', products.uploadImage);
    dom.imageInput.addEventListener('input', products.updateImagePreview);
    dom.stockAddInput.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            products.addStock();
        }
    });

    for (const input of [dom.searchInput, dom.unitFilter, dom.categoryFilter, dom.taxFilter, dom.stockFilter]) {
        input.addEventListener('input', products.renderRows);
        input.addEventListener('change', products.renderRows);
    }
    window.addEventListener('focus', () => products.loadProducts(true));
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) products.loadProducts(true);
    });
    window.setInterval(() => { if (!document.hidden) products.loadProducts(true); }, 30000);
}

window.refreshProducts = () => products.loadProducts(true);
window.focusAdminArea = async area => {
    if (!ready) {
        pendingArea = area;
        return;
    }
    await products.loadProducts(true);
    showArea('products');
    if (area === 'new') {
        state.activeArea = 'edit';
        products.clearForm();
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
        if (hasPermission('products.read')) await products.loadProducts();
        if (hasPermission('users.manage')) await users.loadUserCatalog();
        showArea(hasPermission('products.read') ? 'products' : 'users');
        ready = true;
        if (new URLSearchParams(window.location.search).get('scan') === '1' && hasPermission('products.read')) {
            cameraScanner.start();
        }
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
    dom.productEditorPanel.hidden = !canManageProducts;
    dom.deleteButton.hidden = !canManageProducts;
    dom.downloadLabelsButton.hidden = !canManageProducts;
    dom.cameraScanButton.hidden = !hasPermission('products.read');
    dom.cameraScanButton.textContent = typeof window.kassensystemBridge?.openScannerInBrowser === 'function'
        ? 'QR-Code im Browser scannen' : 'QR-Code scannen';
    dom.selectVisibleProducts.hidden = !canManageProducts;
    dom.saveButton.hidden = !canManageProducts;
    dom.stockPanel.hidden = !canBookStock;
    dom.usersNav.hidden = !canManageUsers;
    dom.productsNav.hidden = !hasPermission('products.read');
    if (!canManageProducts && canBookStock) {
        state.activeArea = 'stock';
        dom.stockPanel.open = true;
    }
    document.body.classList.toggle('stock-only', !canManageProducts && canBookStock);

    for (const element of [dom.nameInput, dom.scanCodeInput, dom.categoryInput, dom.priceInput, dom.stockInput, dom.unitInput, dom.taxInput, dom.imageInput, dom.imageFile]) {
        element.disabled = !canManageProducts;
    }

    products.updateStockControls();
    syncCustomSelects();
}


function hasPermission(permission) {
    return Boolean(state.session.permissions && state.session.permissions[permission]);
}

async function selectScannedProduct(code) {
    await products.loadProducts(true);
    const produkt = state.products.find(eintrag => eintrag.scanCode === code);
    if (!produkt) {
        setStatus(`Kein Produkt zum Scan-Code ${code} gefunden.`);
        return;
    }
    dom.searchInput.value = code;
    dom.unitFilter.value = '';
    dom.categoryFilter.value = '';
    dom.taxFilter.value = '';
    dom.stockFilter.checked = false;
    syncCustomSelects();
    products.selectProduct(produkt);
    dom.rows.querySelector('tr.selected')?.scrollIntoView({behavior: 'smooth', block: 'center'});
    setStatus(`Produkt ${produkt.name} per QR-Code gefunden.`);
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

function showArea(area) {
    const showUsers = area === 'users' && hasPermission('users.manage');
    dom.areaTitle.textContent = showUsers ? 'Benutzer & Rechte'
        : hasPermission('manageProducts') ? 'Verwaltung' : 'Warenzugang';
    dom.areaSubtitle.textContent = showUsers ? 'Konten und Zuständigkeiten' : 'Produkte und Lagerbestand';
    dom.productsView.hidden = showUsers || !hasPermission('products.read');
    dom.usersView.hidden = !showUsers;
    dom.productsNav.classList.toggle('active', !showUsers);
    dom.usersNav.classList.toggle('active', showUsers);
    dom.productsNav.setAttribute('aria-pressed', String(!showUsers));
    dom.usersNav.setAttribute('aria-pressed', String(showUsers));
    if (showUsers) users.loadUsers();
}
