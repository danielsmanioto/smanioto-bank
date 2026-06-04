import { getAccount } from './api.js';
import { requireAuth, getAccountId, logout } from './auth.js';

document.getElementById('btnLogout').addEventListener('click', logout);

const token = await requireAuth();
if (!token) throw new Error('not authenticated');

const accountId = getAccountId();
if (!accountId) {
  document.getElementById('errorMsg').textContent = 'ID da conta não encontrado. Faça login novamente.';
  document.getElementById('errorMsg').classList.add('show');
} else {
  sessionStorage.setItem('accountId', accountId);
  document.getElementById('linkStatement').href = `statement.html?accountId=${accountId}`;
  document.getElementById('linkTransfer').href  = `transfer.html?accountId=${accountId}`;

  try {
    const acc = await getAccount(accountId, token);
    document.getElementById('bank').textContent   = acc.bank;
    document.getElementById('branch').textContent = acc.branch;
    document.getElementById('number').textContent = acc.number;
    document.getElementById('balance').textContent =
      `R$ ${Number(acc.balance).toFixed(2).replace('.', ',')}`;
  } catch (err) {
    const msg = document.getElementById('errorMsg');
    msg.textContent = err.message || 'Erro ao carregar dados da conta.';
    msg.classList.add('show');
  }
}
