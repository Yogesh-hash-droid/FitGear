import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  addCartItem,
  clearCartRequest,
  fetchCart,
  removeCartItem,
  updateCartItem,
} from '../services/api'
import { CartContext } from './cartContextValue'
import { useAuth } from './useAuth'

const GUEST_CART_KEY = 'fitgear_guest_cart'

function loadGuestCart() {
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveGuestCart(items) {
  localStorage.setItem(GUEST_CART_KEY, JSON.stringify(items))
}

function toCartItem(entry) {
  const product = entry.product || entry
  return {
    id: product.id,
    cartItemId: entry.id ?? entry.cartItemId ?? null,
    name: product.name,
    price: Number(product.price),
    stock: product.stock,
    category: product.category,
    imageUrl: product.imageUrl,
    description: product.description,
    quantity: entry.quantity,
  }
}

/**
 * Cart state for the whole app.
 * Signed in  -> server cart (authoritative totals from the API).
 * Guest      -> localStorage cart, merged into the server cart at login.
 */
export function CartProvider({ children }) {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const previousUserRef = useRef(user?.email ?? null)

  const persistGuest = useCallback((next) => {
    setItems(next)
    saveGuestCart(next)
  }, [])

  const refresh = useCallback(async () => {
    if (!user) {
      setItems(loadGuestCart())
      return
    }
    setLoading(true)
    try {
      const cart = await fetchCart()
      setItems((cart.items || []).map(toCartItem))
    } catch {
      // Keep whatever we had if the request fails.
    } finally {
      setLoading(false)
    }
  }, [user])

  // On sign-in, push any guest items into the server cart, then load it.
  useEffect(() => {
    const currentEmail = user?.email ?? null
    const previousEmail = previousUserRef.current
    previousUserRef.current = currentEmail

    let cancelled = false

    async function sync() {
      if (currentEmail && !previousEmail) {
        const guestItems = loadGuestCart()
        if (guestItems.length) {
          try {
            for (const item of guestItems) {
              if (item.quantity > 0) {
                await addCartItem(item.id, item.quantity)
              }
            }
          } catch {
            // Merge is best-effort; the server cart below is authoritative.
          }
          saveGuestCart([])
        }
      }
      if (!cancelled) {
        await refresh()
      }
    }

    sync()
    return () => {
      cancelled = true
    }
  }, [user?.email, refresh])

  // Guest carts stay in sync across tabs.
  useEffect(() => {
    if (user) return undefined
    function onStorage(event) {
      if (event.key === GUEST_CART_KEY) {
        setItems(loadGuestCart())
      }
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [user])

  const add = useCallback(
    async (product, quantity = 1) => {
      if (user) {
        const entry = await addCartItem(product.id, quantity)
        const cart = await fetchCart()
        setItems((cart.items || []).map(toCartItem))
        return toCartItem(entry)
      }
      const current = loadGuestCart()
      const existing = current.find((item) => item.id === product.id)
      let next
      if (existing) {
        next = current.map((item) =>
          item.id === product.id ? { ...item, quantity: item.quantity + quantity } : item,
        )
      } else {
        next = [...current, { ...product, id: product.id, quantity }]
      }
      persistGuest(next)
      return next.find((item) => item.id === product.id)
    },
    [user, persistGuest],
  )

  const setQuantity = useCallback(
    async (productId, quantity) => {
      if (quantity < 1) return
      if (user) {
        const item = items.find((entry) => entry.id === productId)
        if (!item?.cartItemId) return
        await updateCartItem(item.cartItemId, quantity)
        const cart = await fetchCart()
        setItems((cart.items || []).map(toCartItem))
        return
      }
      const next = loadGuestCart().map((item) =>
        item.id === productId ? { ...item, quantity } : item,
      )
      persistGuest(next)
    },
    [user, items, persistGuest],
  )

  const remove = useCallback(
    async (productId) => {
      if (user) {
        const item = items.find((entry) => entry.id === productId)
        if (!item?.cartItemId) return
        await removeCartItem(item.cartItemId)
        const cart = await fetchCart()
        setItems((cart.items || []).map(toCartItem))
        return
      }
      persistGuest(loadGuestCart().filter((item) => item.id !== productId))
    },
    [user, items, persistGuest],
  )

  const clear = useCallback(async () => {
    if (user) {
      try {
        await clearCartRequest()
      } catch {
        // Clearing is best-effort.
      }
      setItems([])
      return
    }
    persistGuest([])
  }, [user, persistGuest])

  const itemCount = useMemo(
    () => items.reduce((total, item) => total + item.quantity, 0),
    [items],
  )

  const subtotal = useMemo(
    () => items.reduce((total, item) => total + item.price * item.quantity, 0),
    [items],
  )

  const value = useMemo(
    () => ({ items, itemCount, subtotal, loading, add, setQuantity, remove, clear, refresh }),
    [items, itemCount, subtotal, loading, add, setQuantity, remove, clear, refresh],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
