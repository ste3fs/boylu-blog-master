import Cookies from 'js-cookie'

const TokenKey = 'blog_token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token, expires = 7) {
  return Cookies.set(TokenKey, token, { expires })
}

export function removeToken() {
  return Cookies.remove(TokenKey)
} 

export function setCookieExpires(key,value,expires) {
  return Cookies.set(key,value,{expires:expires})
} 
export function setCookie(key,value) {
  return Cookies.set(key,value)
} 
export function getCookie(key) {
  return Cookies.get(key)
} 

export function removeCookie(key) {
  return Cookies.remove(key)
} 
