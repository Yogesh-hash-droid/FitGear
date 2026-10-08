export default function SkeletonCards({ count = 4 }) {
  return Array.from({ length: count }, (_, index) => (
    <div className="skeleton-card" key={index} aria-hidden="true">
      <div className="skeleton-media" />
      <div className="skeleton-body">
        <div className="skeleton-line w-70" />
        <div className="skeleton-line w-40" />
      </div>
    </div>
  ))
}
