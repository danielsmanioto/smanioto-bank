import { login } from './api.js';

const form    = document.getElementById('loginForm');
const errMsg  = document.getElementById('errorMsg');

function showError(text) {
  errMsg.textContent = text;
  errMsg.classList.add('show');
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errMsg.classList.remove('show');

  const username  = document.getElementById('username').value.trim();
  const password  = document.getElementById('password').value;
  const accountId = document.getElementById('accountId').value.trim();

  if (!accountId) { showError('Informe o ID da conta.'); return; }

  try {
    const { token } = await login(username, password);
    sessionStorage.setItem('token', token);
    sessionStorage.setItem('accountId', accountId);
    window.location.href = `account.html?accountId=${accountId}`;
  } catch (err) {
    showError(err.message || 'Credenciais inválidas. Tente novamente.');
  }
});
