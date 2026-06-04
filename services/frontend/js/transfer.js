import { transfer } from './api.js';
import { requireAuth, getAccountId, logout } from './auth.js';

document.getElementById('btnLogout').addEventListener('click', logout);

const token = await requireAuth();
if (!token) throw new Error('not authenticated');

const fromAccountId = getAccountId();
document.getElementById('backLink').href = `account.html?accountId=${fromAccountId}`;

const form       = document.getElementById('transferForm');
const errorMsg   = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');

function showError(text) {
  successMsg.classList.remove('show');
  errorMsg.textContent = text;
  errorMsg.classList.add('show');
}

function showSuccess(text) {
  errorMsg.classList.remove('show');
  successMsg.textContent = text;
  successMsg.classList.add('show');
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.classList.remove('show');
  successMsg.classList.remove('show');

  const toAccountId = document.getElementById('toAccountId').value.trim();
  const amount      = parseFloat(document.getElementById('amount').value);

  if (!toAccountId) { showError('Informe o ID da conta destino.'); return; }
  if (!amount || amount <= 0) { showError('O valor deve ser maior que zero.'); return; }
  if (!fromAccountId) { showError('Conta de origem não identificada.'); return; }

  const btn = form.querySelector('button[type=submit]');
  btn.disabled = true;
  btn.textContent = 'Transferindo...';

  try {
    await transfer(fromAccountId, toAccountId, amount, token);
    showSuccess(`Transferência de R$ ${amount.toFixed(2).replace('.', ',')} realizada com sucesso!`);
    form.reset();
  } catch (err) {
    showError(err.message || 'Erro ao realizar transferência.');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Transferir';
  }
});
