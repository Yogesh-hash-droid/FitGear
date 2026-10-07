import { Link } from 'react-router-dom'

export default function ProductsPage() {
  return (
    <main className="simple-page site-shell">
      <nav className="nav-bar"><Link className="brand" to="/"><span>F</span> FITGEAR</Link><Link className="back-link" to="/">Back to home</Link></nav>
      <section className="simple-page-content"><p className="eyebrow">THE COLLECTION</p><h1>All equipment.</h1><p>Product browsing is connected to the Spring Boot catalog. The full filter experience will arrive with the next catalog iteration.</p><Link className="primary-button" to="/#catalog">Browse products <span>↘</span></Link></section>
    </main>
  )
}
