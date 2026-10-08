import { Link } from 'react-router-dom'

const shopLinks = [
  { label: 'All equipment', to: '/products' },
  { label: 'Dumbbells', to: '/products?category=Dumbbells' },
  { label: 'Barbells', to: '/products?category=Barbells' },
  { label: 'Weight Plates', to: '/products?category=Weight%20Plates' },
  { label: 'Benches', to: '/products?category=Benches' },
  { label: 'Racks', to: '/products?category=Racks' },
  { label: 'Home Gym', to: '/products?category=Home%20Gym' },
]

const accountLinks = [
  { label: 'Sign in', to: '/login' },
  { label: 'Create account', to: '/register' },
  { label: 'Profile', to: '/profile' },
  { label: 'Orders', to: '/orders' },
  { label: 'Shopping bag', to: '/cart' },
]

const supportLinks = [
  { label: 'Track your order', to: '/orders' },
  { label: 'Shipping & delivery', to: '/checkout' },
  { label: 'Payment options', to: '/checkout' },
]

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-grid">
        <div className="footer-brand">
          <Link className="brand" to="/">
            <span className="brand-mark">F</span> FitGear
          </Link>
          <p>
            Premium fitness equipment built for serious training — dumbbells, barbells,
            benches, racks and home gym gear.
          </p>
        </div>

        <div className="footer-col">
          <h4>Shop</h4>
          <ul>
            {shopLinks.map((link) => (
              <li key={link.label}>
                <Link to={link.to}>{link.label}</Link>
              </li>
            ))}
          </ul>
        </div>

        <div className="footer-col">
          <h4>Account</h4>
          <ul>
            {accountLinks.map((link) => (
              <li key={link.label}>
                <Link to={link.to}>{link.label}</Link>
              </li>
            ))}
          </ul>
        </div>

        <div className="footer-col">
          <h4>Support</h4>
          <ul>
            {supportLinks.map((link) => (
              <li key={link.label}>
                <Link to={link.to}>{link.label}</Link>
              </li>
            ))}
            <li><span>Free shipping on selected fitness equipment</span></li>
            <li><span>Cash on delivery &amp; secure online payments</span></li>
            <li><span>Live order status updates</span></li>
          </ul>
        </div>
      </div>

      <div className="footer-bottom">
        <span>© 2026 FitGear</span>
        <span>Strength, made accessible.</span>
        <span>Built for the long run.</span>
      </div>
    </footer>
  )
}
