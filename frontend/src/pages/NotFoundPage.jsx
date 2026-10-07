import { Link } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'

export default function NotFoundPage() {
  return (
    <div className="site-shell">
      <Navbar />
      <section className="simple-page-content not-found-page">
        <p className="eyebrow">ERROR 404</p>
        <h1>Wrong<br /><em>rep.</em></h1>
        <p>That page does not exist — or it never did. Head back to the catalog and keep moving.</p>
        <Link className="primary-button" to="/">Back to home <span>↗</span></Link>
      </section>
      <Footer />
    </div>
  )
}
