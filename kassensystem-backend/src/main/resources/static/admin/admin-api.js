export function createApiFetch(session) {
    return async function apiFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        if (options.method && !['GET', 'HEAD'].includes(options.method.toUpperCase())) {
            headers.set('X-CSRF-TOKEN', session().csrfToken || '');
        }
        const response = await fetch(url, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) window.location.href = '../login?session-expired';
        return response;
    };
}

export function isJsonResponse(response) {
    return (response.headers.get('content-type') || '').includes('application/json');
}

export function responseMessage(response) {
    return response.json().then(body => body.message || `Aktion fehlgeschlagen (HTTP ${response.status}).`)
        .catch(() => `Aktion fehlgeschlagen (HTTP ${response.status}).`);
}

export function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
