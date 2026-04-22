import Cookies from 'js-cookie'

const TokenKey = 'boylu-blog-admin-token'

export function setToken(token: string, expires = 1 / 24) {
  return Cookies.set(TokenKey, token, { expires })
}

export function getToken() {
  return Cookies.get(TokenKey)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
} 
