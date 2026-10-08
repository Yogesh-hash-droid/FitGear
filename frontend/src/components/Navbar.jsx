import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { useCart } from '../context/useCart'

const CATEGORY_LINKS = ['Dumbbells', 'Barbells', 'Weight Plates', 'Benches', 'Racks', 'Home Gym']

const iconProps = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.9,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

const SearchIcon = () => (
  <svg {...iconProps} width={14} height={14}>
    <circle cx="11" cy="11" r="7" />
    <path d="M20 20l-3.5-3.5" />
  </svg>
)
const BagIcon = () => (
  <svg {...iconProps}>
    <path d="M6 8h12l-1 12H7L6 8z" />
    <path d="M9 8V6a3 3 0 0 1 6 0v2" />
  </svg>
)
const UserIcon = () => (
  <svg {...iconProps}>
    <circle cx="12" cy="8.5" r="3.5" />
    <path d="M5 20c1.5-3.2 4-4.8 7-4.8s5.5 1.6 7 4.8" />
  </svg>
)
const OrdersIcon = () => (
  <svg {...iconProps}>
    <path d="M4 7.5L12 4l8 3.5v9L12 20l-8-3.5v-9z" />
    <path d="M4 7.5L12 11l8-3.5M12 11v9" />
  </svg>
)
const LogoutIcon = () => (
  <svg {...iconProps}>
    <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
    <path d="M10 8l-4 4 4 4M6 12h9" />
  </svg>
)
const ShieldIcon = () => (
  <svg {...iconProps}>
    <path d="M12 3l7 3v5c0 4.5-3 8.4-7 10-4-1.6-7-5.5-7-10V6l7-3z" />
  </svg>
)

export default function Navbar() {
  const { user, logout } = useAuth()
  const { itemCount } = useCart()
  const [menuOpen, setMenuOpen] = useState(false)
  const [term, setTerm] = useState('')
  const navigate = useNavigate()
  const location = useLocation()

  const categoryParam = new URLSearchParams(location.search).get('category') || ''
  const shopActive = location.pathname === '/products' && !categoryParam

  function runSearch(event) {
    event.preventDefault()
    const query = term.trim()
    navigate(query ? `/products?search=${encodeURIComponent(query)}` : '/products')
    setMenuOpen(false)
  }

  function handleSignOut() {
    logout()
    setMenuOpen(false)
  }

  return (
    <>
      <div className="announce-bar">Free shipping on selected fitness equipment</div>

      <nav className="nav-main">
        <div className="nav-inner">
          <Link className="brand" to="/" aria-label="FitGear home">
            <span className="brand-mark">F</span> FitGear
          </Link>

          <div className="nav-links">
            <Link className={shopActive ? 'active' : ''} to="/products">Shop</Link>
            {CATEGORY_LINKS.map((name) => (
              <Link
                key={name}
                className={categoryParam === name ? 'active' : ''}
                to={`/products?category=${encodeURIComponent(name)}`}
              >
                {name}
              </Link>
            ))}
          </div>

          <div className="nav-actions">
            <form className="nav-search" onSubmit={runSearch} role="search">
              <input
                value={term}
                onChange={(event) => setTerm(event.target.value)}
                placeholder="Search equipment"
                aria-label="Search equipment"
              />
              <button type="submit" aria-label="Search"><SearchIcon /></button>
            </form>

            {user?.role === 'ADMIN' && (
              <Link className="nav-icon-btn" to="/admin" aria-label="Admin console" title="Admin console">
                <ShieldIcon />
              </Link>
            )}
            {user && (
              <Link className="nav-icon-btn" to="/orders" aria-label="Your orders" title="Orders">
                <OrdersIcon />
              </Link>
            )}
            <Link className="nav-icon-btn" to="/cart" aria-label={`Bag, ${itemCount} items`} title="Bag">
              <BagIcon />
              {itemCount > 0 && <span className="cart-count">{itemCount}</span>}
            </Link>
            <Link
              className="nav-icon-btn"
              to={user ? '/profile' : '/login'}
              aria-label={user ? 'Your profile' : 'Sign in'}
              title={user ? user.name : 'Sign in'}
            >
              <UserIcon />
            </Link>
            {user && (
              <button className="nav-signout" type="button" onClick={handleSignOut} aria-label="Sign out" title="Sign out">
                <LogoutIcon />
              </button>
            )}

            <button
              className="nav-toggle"
              type="button"
              aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <span />
              <span />
              <span />
            </button>
          </div>
        </div>
      </nav>

      {menuOpen && (
        <div className="drawer-overlay" onClick={() => setMenuOpen(false)}>
          <div
            className="drawer-panel"
            role="dialog"
            aria-label="Menu"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="drawer-head">
              <Link className="brand" to="/" onClick={() => setMenuOpen(false)}>
                <span className="brand-mark">F</span> FitGear
              </Link>
              <button className="drawer-close" type="button" onClick={() => setMenuOpen(false)} aria-label="Close menu">
                ×
              </button>
            </div>

            <form className="drawer-search" onSubmit={runSearch} role="search">
              <input
                value={term}
                onChange={(event) => setTerm(event.target.value)}
                placeholder="Search equipment"
                aria-label="Search equipment"
              />
              <button type="submit" aria-label="Search"><SearchIcon /></button>
            </form>

            <div className="drawer-nav">
              <Link className={shopActive ? 'active' : ''} to="/products" onClick={() => setMenuOpen(false)}>
                Shop
              </Link>
              {CATEGORY_LINKS.map((name) => (
                <Link
                  key={name}
                  className={categoryParam === name ? 'active' : ''}
                  to={`/products?category=${encodeURIComponent(name)}`}
                  onClick={() => setMenuOpen(false)}
                >
                  {name}
                </Link>
              ))}
            </div>

            <div className="drawer-actions">
              <Link className="nav-icon-btn" to="/cart" onClick={() => setMenuOpen(false)}>
                <BagIcon /> Bag {itemCount > 0 && <span className="cart-count">{itemCount}</span>}
              </Link>
              {user && (
                <Link className="nav-icon-btn" to="/orders" onClick={() => setMenuOpen(false)}>
                  <OrdersIcon /> Orders
                </Link>
              )}
              <Link
                className="nav-icon-btn"
                to={user ? '/profile' : '/login'}
                onClick={() => setMenuOpen(false)}
              >
                <UserIcon /> {user ? 'Profile' : 'Sign in'}
              </Link>
              {user?.role === 'ADMIN' && (
                <Link className="nav-icon-btn" to="/admin" onClick={() => setMenuOpen(false)}>
                  <ShieldIcon /> Admin
                </Link>
              )}
              {user && (
                <button className="nav-signout" type="button" onClick={handleSignOut}>
                  <LogoutIcon /> Sign out
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  )
}
