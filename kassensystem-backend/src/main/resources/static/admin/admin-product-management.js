/** Produktliste, Editor und Warenzugang. */
export function createProductManagement({state, dom, apiFetch, apiUrl, hasPermission, setStatus,
        syncCustomSelects, parseNumber, formatInput, formatMoney, formatAmount, escapeHtml, getLabels}) {
    async function loadProducts(silent = false) {
        if (!silent) setStatus('Lade Produkte...');
        try {
            const response = await apiFetch(apiUrl);
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
                <td class="label-selection"></td>
                <td>${product.id}</td>
                <td>${escapeHtml(product.name)}</td>
                <td>${escapeHtml(product.scanCode || '')}</td>
                <td>${escapeHtml(product.kategorie || 'Sonstiges')}</td>
                <td>${formatMoney(product.preis)}</td>
                <td>${formatAmount(product.lagerbestand)}</td>
                <td>${escapeHtml(product.einheitLabel)}</td>
                <td>${formatAmount(product.steuerSatz)} %</td>
            `;
            if (hasPermission('manageProducts')) {
                row.querySelector('.label-selection').append(getLabels().renderCheckbox(product));
            }
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
            cell.colSpan = 9;
            cell.className = 'empty-row';
            cell.textContent = state.products.length ? 'Keine Produkte für diese Filter gefunden.' : 'Noch keine Produkte vorhanden.';
            row.appendChild(cell);
            dom.rows.appendChild(row);
        }
        getLabels().updateControls();
    }

    function matchesFilter(product, filter, unit, category, tax, onlyStock) {
        const searchable = `${product.id} ${product.name} ${product.scanCode || ''} ${product.kategorie || ''} ${product.einheit} ${product.einheitLabel} ${product.steuerSatz}`.toLowerCase();
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
        dom.scanCodeInput.value = product.scanCode || '';
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
            scanCode: dom.scanCodeInput.value,
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

    return {loadProducts, renderRows, selectProduct, matchesFilter, clearForm, saveProduct, deleteSelected,
        addStock, uploadImage, updateImagePreview, updateStockControls, showError};
}
