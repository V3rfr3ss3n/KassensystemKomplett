import {responseMessage} from './admin-api.js';

/** Benutzeroberfläche und Rechteberechnung der Verwaltung. */
export function createUserManagement({state, dom, apiFetch, hasPermission, permissionLabels, formatRole}) {
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
            title.textContent = permissionLabels[permission] || permission;
            title.title = permission;
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
        dom.effectiveRights.textContent = `Wirksame Rechte: ${granted.length
            ? granted.map(permission => permissionLabels[permission] || permission).join(', ') : 'keine'}`;
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

    return {loadUserCatalog, loadUsers, renderUsers, clearUserForm, saveUser, resetUserPassword, changeOwnPassword};
}
