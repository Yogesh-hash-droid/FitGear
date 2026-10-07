import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { addCartItem, fetchCart, fetchProducts, removeCartItem } from './services/api'
import { useAuth } from './context/useAuth'
import './App.css'

const fallbackProducts = [
  { id: 'demo-1', name: 'Adjustable Dumbbell Set', description: 'Compact strength training for serious home sessions.', price: 4999, stock: 12, category: 'Strength', imageUrl: '/images/products/strength.svg' },
  { id: 'demo-2', name: 'Core Stability Mat', description: 'Dense, grippy support for floor work and mobility.', price: 1299, stock: 28, category: 'Recovery', imageUrl: '/images/products/recovery.svg' },
  { id: 'demo-3', name: 'Resistance Band Kit', description: 'Five progressive bands for warmups and full-body training.', price: 899, stock: 42, category: 'Accessories', imageUrl: '/images/products/accessories.svg' },
  { id: 'demo-4', name: 'Kettlebell 16kg', description: 'Balanced cast-iron weight for swings, carries, and squats.', price: 2499, stock: 7, category: 'Strength', imageUrl: '/images/products/strength.svg' },
  { id: 'demo-5', name: 'Indoor Cycling Bike', description: 'Compact cardio setup for steady rides at home.', price: 24999, stock: 4, category: 'Cardio', imageUrl: '/images/products/cardio.svg' },
]

const categories = [
  { name: 'Strength', description: 'Build capacity' },
  { name: 'Recovery', description: 'Move better' },
  { name: 'Accessories', description: 'Train anywhere' },
  { name: 'Cardio', description: 'Find your pace' },
]

const formatPrice = (price) => `₹${Number(price).toLocaleString('en-IN')}`

function App() {
  const { user, logout } = useAuth()
  const [products, setProducts] = useState(fallbackProducts)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [loading, setLoading] = useState(false)
  const [apiOnline, setApiOnline] = useState(false)
  const [cart, setCart] = useState([])
  const [cartOpen, setCartOpen] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState(null)

  useEffect(() => {
    let active = true
    if (!user) return undefined

    fetchCart()
      .then((items) => {
        if (active) setCart(items.map((item) => ({ ...item.product, cartItemId: item.id, quantity: item.quantity })))
      })
      .catch(() => {})

    return () => { active = false }
  }, [user])

  const addToCart = async (product) => {
    if (user && Number.isInteger(Number(product.id))) {
      try {
        const item = await addCartItem(product.id)
        setCart((currentCart) => {
          const existing = currentCart.find((entry) => entry.id === product.id)
          if (existing) return currentCart.map((entry) => entry.id === product.id ? { ...entry, quantity: item.quantity, cartItemId: item.id } : entry)
          return [...currentCart, { ...item.product, cartItemId: item.id, quantity: item.quantity }]
        })
        return
      } catch {
        // Keep the local cart usable if the API is unavailable.
      }
    }
    setCart((currentCart) => {
      const existing = currentCart.find((item) => item.id === product.id)
      if (existing) return currentCart.map((item) => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item)
      return [...currentCart, { ...product, quantity: 1 }]
    })
  }

  const cartCount = cart.reduce((total, item) => total + item.quantity, 0)
  const cartTotal = cart.reduce((total, item) => total + Number(item.price) * item.quantity, 0)

  const removeFromCart = async (item) => {
    if (user && item.cartItemId) {
      try { await removeCartItem(item.cartItemId) } catch { return }
    }
    setCart((current) => current.filter((cartItem) => cartItem.id !== item.id))
  }

  useEffect(() => {
    let active = true

    async function loadProducts() {
      setLoading(true)
      try {
        const results = await fetchProducts(search, category)
        if (active) {
          setProducts(results)
          setApiOnline(true)
        }
      } catch {
        if (active) {
          const query = search.trim().toLowerCase()
          const filtered = fallbackProducts.filter((product) => {
            const matchesSearch = !query || `${product.name} ${product.description}`.toLowerCase().includes(query)
            const matchesCategory = !category || product.category.toLowerCase() === category.toLowerCase()
            return matchesSearch && matchesCategory
          })
          setProducts(filtered)
          setApiOnline(false)
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    const timer = setTimeout(loadProducts, 250)
    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [search, category])

  return (
    <main className="site-shell">
      <nav className="nav-bar">
        <a className="brand" href="/" aria-label="FitGear home"><span>F</span> FITGEAR</a>
        <div className="nav-links"><a href="#catalog">Shop</a><a href="#categories">Categories</a><a href="#story">Our standard</a></div>
        <div className="nav-actions"><button type="button" aria-label="Wishlist">♡</button><button type="button" aria-label="Shopping bag" onClick={() => setCartOpen(true)}>Bag <small>{cartCount}</small></button>{user?.role === 'ADMIN' && <Link to="/admin/products">Manage products</Link>}{user ? <button type="button" onClick={logout}>{user.name} · Sign out</button> : <a href="/login">Sign in</a>}</div>
      </nav>

      <section className="hero-band">
        <div className="hero-copy"><p className="eyebrow">EQUIPMENT FOR THE EVERYDAY ATHLETE</p><h1>Train with<br /><em>intention.</em></h1><p className="hero-text">Well-made essentials for stronger sessions, smaller spaces, and the work you keep showing up for.</p><a className="primary-button" href="#catalog">Shop the collection <span>↘</span></a></div>
        <div className="hero-art" aria-label="Abstract fitness equipment illustration"><div className="weight weight-one"></div><div className="weight weight-two"></div><div className="bar"></div><div className="sun"></div><span>01 / 04</span></div>
      </section>

      <section id="categories" className="category-strip" aria-label="Shop by category">
        {categories.map((item, index) => (
          <button
            className={category === item.name ? 'active' : ''}
            type="button"
            key={item.name}
            aria-pressed={category === item.name}
            onClick={() => {
              setCategory(category === item.name ? '' : item.name)
              document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' })
            }}
          >
            <span>0{index + 1}</span>
            <strong>{item.name}</strong>
            <small>{item.description}</small>
          </button>
        ))}
      </section>

      <section id="catalog" className="catalog-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">THE STARTING LINEUP</p>
            <h2>{category || 'Useful by design.'}</h2>
            {category && <button className="clear-filter" type="button" onClick={() => setCategory('')}>Show all equipment ×</button>}
          </div>
          <div className="catalog-tools">
            <label htmlFor="product-search">Search products</label>
            <input id="product-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Try “dumbbell”" />
            <span className={apiOnline ? 'status online' : 'status'}>{apiOnline ? 'Live catalog' : 'Preview catalog'}</span>
          </div>
        </div>

        <div className="product-grid">
          {loading ? (
            <p className="empty-state">Loading the collection...</p>
          ) : products.length ? (
            products.map((product, index) => (
              <article className="product-card" key={product.id}>
                <button className={`product-visual visual-${index % 4}`} type="button" onClick={() => setSelectedProduct(product)}>
                  <span>{product.category || 'FITGEAR'}</span>
                  {product.imageUrl ? <img src={product.imageUrl} alt={product.name} loading="lazy" /> : <div className="visual-shape"></div>}
                </button>
                <div className="product-info">
                  <div>
                    <button className="product-title" type="button" onClick={() => setSelectedProduct(product)}><h3>{product.name}</h3></button>
                    <p>{product.description}</p>
                  </div>
                  <strong>{formatPrice(product.price)}</strong>
                </div>
                <button className="add-button" type="button" onClick={() => addToCart(product)}>Add to bag <span>+</span></button>
              </article>
            ))
          ) : (
            <p className="empty-state">No products match that search.</p>
          )}
        </div>
      </section>

      <section id="story" className="standard-band"><p className="eyebrow">THE FITGEAR STANDARD</p><h2>Less noise.<br /><em>More work.</em></h2><p>We choose dependable materials, considered proportions, and equipment that earns its place in your routine.</p><a href="#catalog">Explore all equipment ↗</a></section>
      <footer><span>© 2026 FitGear</span><span>Strength, made accessible.</span><span>Built for the long run.</span></footer>

      {selectedProduct && (
        <div className="overlay" role="presentation" onClick={() => setSelectedProduct(null)}>
          <section className="detail-panel" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <button className="close-button" type="button" onClick={() => setSelectedProduct(null)} aria-label="Close product details">×</button>
            <p className="eyebrow">PRODUCT DETAIL</p>
            <h2>{selectedProduct.name}</h2>
            <p>{selectedProduct.description}</p>
            <strong className="detail-price">{formatPrice(selectedProduct.price)}</strong>
            <button className="primary-button" type="button" onClick={() => { addToCart(selectedProduct); setSelectedProduct(null); setCartOpen(true) }}>Add to bag <span>+</span></button>
          </section>
        </div>
      )}

      {cartOpen && (
        <div className="overlay" role="presentation" onClick={() => setCartOpen(false)}>
          <aside className="cart-panel" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="panel-header">
              <h2>Your bag</h2>
              <button className="close-button" type="button" onClick={() => setCartOpen(false)} aria-label="Close bag">×</button>
            </div>
            {cart.length ? (
              <>
                <div className="cart-items">{cart.map((item) => (
                  <div className="cart-item" key={item.id}>
                    <div>
                      <strong>{item.name}</strong>
                      <span>{item.quantity} × {formatPrice(item.price)}</span>
                    </div>
                    <button type="button" onClick={() => removeFromCart(item)}>Remove</button>
                  </div>
                ))}</div>
                <div className="cart-total"><span>Total</span><strong>{formatPrice(cartTotal)}</strong></div>
                <a className="primary-button checkout-button" href="/checkout">Checkout <span>↗</span></a>
              </>
            ) : (
              <p className="empty-state">Your bag is ready for its first set.</p>
            )}
          </aside>
        </div>
      )}
    </main>
  )
}

export default App
