import { validateToken } from './api.js';

export function getToken() {
  return sessionStorage.getItem('token');
}

export function getAccountId() {
  return new URLSearchParams(window.location.search).get('accountId')
    || sessionStorage.getItem('accountId');
}

export function logout() {
  sessionStorage.clear();
  window.location.href = 'login.html';
}

export async function requireAuth() {
  const token = getToken();
  if (!token) { window.location.href = 'login.html'; return null; }
  try {
    await validateToken(token);
    return token;
  } catch {
    sessionStorage.clear();
    window.location.href = 'login.html';
    return null;
  }
}
