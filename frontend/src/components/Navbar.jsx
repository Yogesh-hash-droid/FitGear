import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { useCart } from '../context/useCart'

export default function Navbar() {
  const { user, logout } = useAuth()
  const { itemCount } = useCart()

  return (
    <nav className="nav-bar">
      <Link className="brand" to="/" aria-label="FitGear home"><span>F</span> FITGEAR</Link>
      <div className="nav-links">
        <NavLink to="/products">Shop</NavLink>
        <NavLink to="/products?category=Strength">Strength</NavLink>
        <NavLink to="/products?category=Cardio">Cardio</NavLink>
      </div>
      <div className="nav-actions">
        <Link to="/cart">Bag <small>{itemCount}</small></Link>
        {user && <Link to="/orders">Orders</Link>}
        {user?.role === 'ADMIN' && <Link to="/admin">Admin</Link>}
        {user ? (
          <>
            <Link to="/profile">{user.name}</Link>
            <button type="button" onClick={logout}>Sign out</button>
          </>
        ) : (
          <Link to="/login">Sign in</Link>
        )}
      </div>
    </nav>
  )
}
