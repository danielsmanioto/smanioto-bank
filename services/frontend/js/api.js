const AUTH_URL     = 'http://localhost:8080';
const PEOPLE_URL   = 'http://localhost:8081';
const ACCOUNTS_URL = 'http://localhost:8082';

async function request(url, options = {}) {
  const res = await fetch(url, options);
  if (!res.ok) {
    const text = await res.text().catch(() => 'Erro desconhecido');
    throw new Error(text || `HTTP ${res.status}`);
  }
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return res.json();
  return null;
}

export async function login(username, password) {
  return request(`${AUTH_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
}

export async function validateToken(token) {
  return request(`${AUTH_URL}/auth/validate`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function getAccount(accountId, token) {
  return request(`${ACCOUNTS_URL}/accounts/${accountId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function getStatement(accountId, token) {
  return request(`${ACCOUNTS_URL}/accounts/${accountId}/statement`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function transfer(fromAccountId, toAccountId, amount, token) {
  return request(`${ACCOUNTS_URL}/accounts/transfer`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ fromAccountId, toAccountId, amount }),
  });
}
