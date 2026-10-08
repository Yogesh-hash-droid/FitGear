import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import CategoryCard from '../components/CategoryCard'
import Footer from '../components/Footer'
import HeroSection from '../components/HeroSection'
import Navbar from '../components/Navbar'
import ProductCard from '../components/ProductCard'
import PromoBanner from '../components/PromoBanner'
import SectionHeader from '../components/SectionHeader'
import SkeletonCards from '../components/SkeletonCards'
import TrustSection from '../components/TrustSection'
import { errorMessage, fetchProducts } from '../services/api'
import { useCart } from '../context/useCart'
import { useToast } from '../context/useToast'

const unsplash = (photoId, width = 900) =>
  `https://images.unsplash.com/${photoId}?auto=format&fit=crop&w=${width}&q=80`

const HERO_IMAGE = unsplash('photo-1770664612843-b44e26070024', 1920)

const CATEGORY_CARDS = [
  { name: 'Dumbbells', blurb: 'Hex & adjustable', image: unsplash('photo-1679792756270-c90bb6f34868') },
  { name: 'Barbells', blurb: 'Olympic & curl bars', image: unsplash('photo-1590239480892-9059a902df60') },
  { name: 'Weight Plates', blurb: 'Iron & bumper', image: unsplash('photo-1718254951550-8724745444b9') },
  { name: 'Benches', blurb: 'Flat & adjustable', image: unsplash('photo-1628935291759-bbaf33a66dc6') },
  { name: 'Racks', blurb: 'Squat & power', image: unsplash('photo-1577992805669-c80be3285f36') },
  { name: 'Home Gym', blurb: 'All-in-one stations', image: unsplash('photo-1627257058769-0a99529e4312') },
]

const PROMO_IMAGE = unsplash('photo-1722248211551-68fa9e63ad4e', 1200)

export default function HomePage() {
  const [bestSellers, setBestSellers] = useState([])
  const [newArrivals, setNewArrivals] = useState([])
  const [loading, setLoading] = useState(true)
  const { add } = useCart()
  const { push } = useToast()

  useEffect(() => {
    let active = true
    Promise.allSettled([
      fetchProducts({ page: 0, size: 4, sort: 'id,asc' }),
      fetchProducts({ page: 0, size: 4, sort: 'id,desc' }),
    ])
      .then(([best, latest]) => {
        if (!active) return
        if (best.status === 'fulfilled') setBestSellers(best.value.content || [])
        if (latest.status === 'fulfilled') setNewArrivals(latest.value.content || [])
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

      <HeroSection
        image={HERO_IMAGE}
        eyebrow="Premium fitness equipment"
        title={<>Build your <em>strongest self</em></>}
        text="Premium fitness equipment built for serious training."
        primary={{ to: '/products', label: 'Shop equipment' }}
        secondary={{ to: '/products', label: 'Explore collection' }}
        notes={['Free shipping on selected equipment', 'Cash on delivery available']}
      />

      <section className="section" aria-labelledby="category-heading">
        <SectionHeader
          eyebrow="Browse the store"
          id="category-heading"
          title="Shop by category"
          action={<Link className="clear-filter" to="/products">View all equipment →</Link>}
        />
        <div className="category-grid">
          {CATEGORY_CARDS.map((category) => (
            <CategoryCard key={category.name} {...category} />
          ))}
        </div>
      </section>

      <section className="section alt" aria-labelledby="best-sellers-heading">
        <SectionHeader
          eyebrow="The lineup"
          id="best-sellers-heading"
          title="Best sellers"
          action={<span className="status online">Live catalog</span>}
        />
        {loading ? (
          <div className="product-grid"><SkeletonCards /></div>
        ) : bestSellers.length ? (
          <div className="product-grid">
            {bestSellers.map((product) => (
              <ProductCard key={product.id} product={product} onAdd={handleAdd} />
            ))}
          </div>
        ) : (
          <p className="empty-state">No products yet. Check back soon.</p>
        )}
      </section>

      <section className="section" aria-label="Home gym promotion">
        <PromoBanner
          eyebrow="Build at home"
          title="Level up your home gym"
          text="Stations, racks and benches that turn a spare room into a serious training space."
          image={PROMO_IMAGE}
          imageAlt="Home gym with training equipment"
          cta={{ to: '/products?category=Home%20Gym', label: 'Shop home gym' }}
        />
      </section>

      <section className="section alt" aria-labelledby="trust-heading">
        <SectionHeader eyebrow="Why FitGear" id="trust-heading"
          title="Why train with FitGear?" />
        <TrustSection />
      </section>

      <section className="section" aria-labelledby="new-arrivals-heading">
        <SectionHeader
          eyebrow="Just landed"
          id="new-arrivals-heading"
          title="New arrivals"
          action={<Link className="clear-filter" to="/products">Shop all →</Link>}
        />
        {loading ? (
          <div className="product-grid"><SkeletonCards /></div>
        ) : newArrivals.length ? (
          <div className="product-grid">
            {newArrivals.map((product) => (
              <ProductCard key={product.id} product={product} onAdd={handleAdd} />
            ))}
          </div>
        ) : (
          <p className="empty-state">No products yet. Check back soon.</p>
        )}
      </section>

      <section className="cta-band">
        <p className="eyebrow">Start training</p>
        <h2>Ready to train harder?</h2>
        <p>Equipment that earns its place in your routine — browse the full catalog and build your setup.</p>
        <Link className="primary-button" to="/products">Shop now <span>→</span></Link>
      </section>

      <Footer />
    </div>
  )
}
