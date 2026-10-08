import { useState } from 'react'
import { Link } from 'react-router-dom'

const HERO_FALLBACK = '/images/hero-fallback.svg'

/**
 * Homepage hero: full-bleed photo with overlay copy. If the photo fails
 * to load, a local illustration is shown instead.
 */
export default function HeroSection({
  image,
  eyebrow,
  title,
  text,
  primary,
  secondary,
  notes = [],
}) {
  const [failedImage, setFailedImage] = useState(null)
  const source = image && failedImage !== image ? image : HERO_FALLBACK

  return (
    <section className="hero">
      <div className="hero-media">
        <img
          src={source}
          alt=""
          onError={() => {
            if (image && source !== HERO_FALLBACK) setFailedImage(image)
          }}
        />
      </div>
      <div className="hero-overlay" />
      <div className="hero-inner">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="hero-text">{text}</p>
        <div className="hero-actions">
          <Link className="primary-button" to={primary.to}>
            {primary.label}
          </Link>
          <Link className="ghost-button" to={secondary.to}>
            {secondary.label}
          </Link>
        </div>
        {notes.length > 0 && (
          <div className="hero-notes">
            {notes.map((note) => (
              <span key={note}>{note}</span>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
