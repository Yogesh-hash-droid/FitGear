export default function SectionHeader({ eyebrow, title, action = null, id }) {
  return (
    <div className="section-heading">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2 id={id}>{title}</h2>
      </div>
      {action}
    </div>
  )
}
