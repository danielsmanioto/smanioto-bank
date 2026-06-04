import { getStatement } from './api.js';
import { requireAuth, getAccountId, logout } from './auth.js';

document.getElementById('btnLogout').addEventListener('click', logout);

const token = await requireAuth();
if (!token) throw new Error('not authenticated');

const accountId = getAccountId();
document.getElementById('backLink').href = `account.html?accountId=${accountId}`;

if (!accountId) {
  const msg = document.getElementById('errorMsg');
  msg.textContent = 'ID da conta não encontrado.';
  msg.classList.add('show');
} else {
  try {
    const movements = await getStatement(accountId, token);

    if (!movements || movements.length === 0) {
      document.getElementById('emptyMsg').style.display = 'block';
    } else {
      const table = document.getElementById('statementTable');
      const tbody = document.getElementById('statementBody');
      table.style.display = 'table';

      movements.forEach(m => {
        const tr = document.createElement('tr');
        const isDebit = m.type === 'DEBIT';
        const date = new Date(m.createdAt).toLocaleString('pt-BR', {
          day: '2-digit', month: '2-digit', year: 'numeric',
          hour: '2-digit', minute: '2-digit',
        });
        const valueFormatted = Number(m.amount).toFixed(2).replace('.', ',');

        tr.innerHTML = `
          <td>${date}</td>
          <td class="${isDebit ? 'debit' : 'credit'}">${isDebit ? 'Débito' : 'Crédito'}</td>
          <td>${m.description || '—'}</td>
          <td style="text-align:right" class="${isDebit ? 'debit' : 'credit'}">
            ${isDebit ? '-' : '+'}${valueFormatted}
          </td>`;
        tbody.appendChild(tr);
      });
    }
  } catch (err) {
    const msg = document.getElementById('errorMsg');
    msg.textContent = err.message || 'Erro ao carregar extrato.';
    msg.classList.add('show');
  }
}
