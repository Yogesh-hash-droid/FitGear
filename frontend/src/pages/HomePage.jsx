import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import ProductCard from '../components/ProductCard'
import { errorMessage, fetchProducts } from '../services/api'
import { useCart } from '../context/useCart'
import { useToast } from '../context/useToast'

const categories = [
  { name: 'Strength', description: 'Build capacity' },
  { name: 'Recovery', description: 'Move better' },
  { name: 'Accessories', description: 'Train anywhere' },
  { name: 'Cardio', description: 'Find your pace' },
]

export default function HomePage() {
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(true)
  const { add } = useCart()
  const { push } = useToast()

  useEffect(() => {
    let active = true
    fetchProducts({ page: 0, size: 4, sort: 'id,desc' })
      .then((page) => {
        if (active) setFeatured(page.content || [])
      })
      .catch(() => {
        if (active) setFeatured([])
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  async function handleAdd(product) {
    try {
      await add(product, 1)
      push(`${product.name} added to your bag.`, 'success')
    } catch (error) {
      push(errorMessage(error, 'Could not add to bag.'), 'error')
    }
  }

  return (
    <div className="site-shell">
      <Navbar />

      <section className="hero-band">
        <div className="hero-copy">
          <p className="eyebrow">EQUIPMENT FOR THE EVERYDAY ATHLETE</p>
          <h1>Train with<br /><em>intention.</em></h1>
          <p className="hero-text">Well-made essentials for stronger sessions, smaller spaces, and the work you keep showing up for.</p>
          <Link className="primary-button" to="/products">Shop the collection <span>↘</span></Link>
        </div>
        <div className="hero-art" aria-label="Abstract fitness equipment illustration">
          <div className="weight weight-one" />
          <div className="weight weight-two" />
          <div className="bar" />
          <div className="sun" />
          <span>01 / 04</span>
        </div>
      </section>

      <section className="category-strip" aria-label="Shop by category">
        {categories.map((item, index) => (
          <Link key={item.name} to={`/products?category=${encodeURIComponent(item.name)}`}>
            <span>0{index + 1}</span>
            <strong>{item.name}</strong>
            <small>{item.description}</small>
          </Link>
        ))}
      </section>

      <section className="catalog-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">THE STARTING LINEUP</p>
            <h2>Useful by design.</h2>
          </div>
          <div className="catalog-tools">
            <span className="status online">Live catalog</span>
            <Link className="clear-filter" to="/products">View all equipment →</Link>
          </div>
        </div>

        <div className="product-grid">
          {loading ? (
            <p className="empty-state">Loading the collection...</p>
          ) : featured.length ? (
            featured.map((product, index) => (
              <ProductCard key={product.id} product={product} index={index} onAdd={handleAdd} />
            ))
          ) : (
            <p className="empty-state">No products yet. Check back soon.</p>
          )}
        </div>
      </section>

      <section className="standard-band">
        <p className="eyebrow">THE FITGEAR STANDARD</p>
        <h2>Less noise.<br /><em>More work.</em></h2>
        <p>We choose dependable materials, considered proportions, and equipment that earns its place in your routine.</p>
        <Link to="/products">Explore all equipment ↗</Link>
      </section>

      <Footer />
    </div>
  )
}
