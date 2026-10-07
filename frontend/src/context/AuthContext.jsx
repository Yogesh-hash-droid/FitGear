import { useState } from 'react'
import { loginUser, registerUser } from '../services/api'
import { AuthContext } from './authContextValue'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('fitgear_user')
    return savedUser ? JSON.parse(savedUser) : null
  })

  async function login(credentials) {
    const response = await loginUser(credentials)
    localStorage.setItem('fitgear_token', response.token)
    localStorage.setItem('fitgear_user', JSON.stringify(response))
    setUser(response)
    return response
  }

  async function register(details) {
    await registerUser(details)
    return login({ email: details.email, password: details.password })
  }

  function logout() {
    localStorage.removeItem('fitgear_token')
    localStorage.removeItem('fitgear_user')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>
}

