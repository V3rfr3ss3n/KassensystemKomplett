/** Auswahl und PDF-Export der QR-Etiketten in der Produktverwaltung. */
export function createLabelSelection({state, dom, apiFetch, matchesFilter, setStatus, showError}) {
    const selected = new Set();

    function visibleProducts() {
        return state.products.filter(product => matchesFilter(product,
            dom.searchInput.value.trim().toLowerCase(), dom.unitFilter.value,
            dom.categoryFilter.value, dom.taxFilter.value, dom.stockFilter.checked));
    }

    function selectVisible() {
        for (const product of visibleProducts()) {
            if (dom.selectVisibleProducts.checked) selected.add(product.id);
            else selected.delete(product.id);
        }
    }

    function renderCheckbox(product) {
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = selected.has(product.id);
        checkbox.setAttribute('aria-label', `Etikett für ${product.name} auswählen`);
        checkbox.addEventListener('click', event => event.stopPropagation());
        checkbox.addEventListener('change', () => {
            if (checkbox.checked) selected.add(product.id);
            else selected.delete(product.id);
            updateControls();
        });
        return checkbox;
    }

    function updateControls() {
        const vorhandeneIds = new Set(state.products.map(product => product.id));
        for (const id of selected) if (!vorhandeneIds.has(id)) selected.delete(id);
        const visible = visibleProducts();
        dom.selectVisibleProducts.checked = visible.length > 0 && visible.every(product => selected.has(product.id));
        dom.downloadLabelsButton.disabled = selected.size === 0;
        dom.downloadLabelsButton.textContent = `QR-Etiketten als PDF (${selected.size})`;
    }

    async function download() {
        const produktIds = [...selected].filter(id => state.products.some(product => product.id === id));
        if (!produktIds.length) { setStatus('Bitte mindestens ein Produkt auswählen.'); return; }
        if (window.kassensystemBridge && typeof window.kassensystemBridge.saveLabels === 'function') {
            window.kassensystemBridge.saveLabels(JSON.stringify(produktIds));
            return;
        }
        try {
            const response = await apiFetch('../api/produkte/etiketten', {
                method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({produktIds})
            });
            if (!response.ok) { await showError(response); return; }
            const url = URL.createObjectURL(await response.blob());
            const link = document.createElement('a');
            link.href = url;
            link.download = 'qr-etiketten.pdf';
            link.click();
            window.setTimeout(() => URL.revokeObjectURL(url), 60000);
            setStatus('QR-Etiketten wurden heruntergeladen.');
        } catch (error) {
            setStatus('QR-Etiketten konnten nicht geladen werden.');
        }
    }

    return {selectVisible, renderCheckbox, updateControls, download};
}
