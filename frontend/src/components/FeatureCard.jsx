export default function FeatureCard({ icon, title, text }) {
  return (
    <article className="feature-card">
      <span className="feature-icon" aria-hidden="true">{icon}</span>
      <h3>{title}</h3>
      <p>{text}</p>
    </article>
  )
}
