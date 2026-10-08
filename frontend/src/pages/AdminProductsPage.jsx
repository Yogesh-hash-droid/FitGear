import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createProduct, deleteProduct, fetchAdminProducts, updateProduct } from '../services/api'
import './AdminPages.css'

const emptyProduct = {
  name: '',
  description: '',
  price: '',
  stock: '',
  category: 'Strength',
  imageUrl: '',
}

const categories = [
  'Dumbbells',
  'Barbells',
  'Weight Plates',
  'Benches',
  'Racks',
  'Home Gym',
  'Strength',
  'Recovery',
  'Accessories',
  'Cardio',
]

function getErrorMessage(error) {
  return error.response?.data?.message || error.response?.data?.error || error.message || 'Please try again.'
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(emptyProduct)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function refreshProducts() {
    setLoading(true)
    setError('')
    try {
      setProducts(await fetchAdminProducts())
    } catch (requestError) {
      setError(`Could not load products. ${getErrorMessage(requestError)}`)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    fetchAdminProducts()
      .then((items) => {
        if (active) setProducts(items)
      })
      .catch((requestError) => {
        if (active) setError(`Could not load products. ${getErrorMessage(requestError)}`)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  function startEditing(product) {
    setEditingId(product.id)
    setForm({
      name: product.name || '',
      description: product.description || '',
      price: product.price ?? '',
      stock: product.stock ?? '',
      category: product.category || 'Strength',
      imageUrl: product.imageUrl || '',
    })
    setNotice('')
    setError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function cancelEditing() {
    setEditingId(null)
    setForm(emptyProduct)
    setError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    const payload = {
      ...form,
      price: Number(form.price),
      stock: Number(form.stock),
      imageUrl: form.imageUrl.trim() || null,
    }

    try {
      if (editingId === null) {
        await createProduct(payload)
        setNotice('Product created.')
      } else {
        await updateProduct(editingId, payload)
        setNotice('Product updated.')
      }
      setEditingId(null)
      setForm(emptyProduct)
      await refreshProducts()
    } catch (requestError) {
      setError(`Could not save the product. ${getErrorMessage(requestError)}`)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(product) {
    if (!window.confirm(`Delete "${product.name}"? This cannot be undone.`)) return
    setError('')
    setNotice('')
    try {
      await deleteProduct(product.id)
      setNotice('Product deleted.')
      await refreshProducts()
      if (editingId === product.id) cancelEditing()
    } catch (requestError) {
      setError(`Could not delete the product. ${getErrorMessage(requestError)}`)
    }
  }

  return (
    <main className="admin-page site-shell">
      <nav className="nav-bar">
        <Link className="brand" to="/"><span className="brand-mark">F</span> FitGear</Link>
        <Link className="back-link" to="/">Back to storefront</Link>
      </nav>

      <section className="admin-content">
        <header className="admin-heading">
          <div>
            <p className="eyebrow">FITGEAR ADMIN</p>
            <h1>Product<br /><em>management.</em></h1>
          </div>
          <p>Keep the catalog current. Update pricing, stock, categories, and product imagery from one place.</p>
        </header>

        <section className="admin-form-section" aria-labelledby="product-form-heading">
          <div className="admin-section-title">
            <h2 id="product-form-heading">{editingId === null ? 'Add a product' : 'Edit product'}</h2>
            {editingId !== null && <button className="admin-text-button" type="button" onClick={cancelEditing}>Cancel edit</button>}
          </div>
          <form className="admin-product-form" onSubmit={handleSubmit}>
            <label>Product name<input required maxLength="120" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
            <label>Category<input required list="product-categories" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} /><datalist id="product-categories">{categories.map((category) => <option key={category} value={category} />)}</datalist></label>
            <label>Description<textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} rows="3" /></label>
            <div className="admin-form-row">
              <label>Price (₹)<input required type="number" min="0.01" step="0.01" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} /></label>
              <label>Stock<input required type="number" min="0" step="1" value={form.stock} onChange={(event) => setForm({ ...form, stock: event.target.value })} /></label>
            </div>
            <label>Image URL<input type="text" placeholder="https://… or /images/products/…" value={form.imageUrl} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} /></label>
            {error && <p className="admin-message admin-error" role="alert">{error}</p>}
            {notice && <p className="admin-message admin-notice" role="status">{notice}</p>}
            <button className="primary-button admin-submit" type="submit" disabled={saving}>{saving ? 'Saving…' : editingId === null ? 'Create product' : 'Save changes'} <span>↗</span></button>
          </form>
        </section>

        <section className="admin-inventory" aria-labelledby="inventory-heading">
          <div className="admin-section-title">
            <h2 id="inventory-heading">Catalog <span>{products.length}</span></h2>
            <button className="admin-text-button" type="button" onClick={refreshProducts} disabled={loading}>{loading ? 'Refreshing…' : 'Refresh'}</button>
          </div>
          {error && <p className="admin-message admin-error" role="alert">{error}</p>}
          {loading ? <p className="admin-empty">Loading catalog…</p> : products.length ? (
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead><tr><th>Product</th><th>Category</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead>
                <tbody>{products.map((product) => (
                  <tr key={product.id}>
                    <td><strong>{product.name}</strong><small>{product.imageUrl ? 'Image set' : 'No image URL'}</small></td>
                    <td>{product.category || '—'}</td>
                    <td>₹{Number(product.price).toLocaleString('en-IN')}</td>
                    <td className={Number(product.stock) === 0 ? 'out-of-stock' : ''}>{product.stock}</td>
                    <td><div className="admin-actions"><button type="button" onClick={() => startEditing(product)}>Edit</button><button type="button" onClick={() => handleDelete(product)}>Delete</button></div></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          ) : <p className="admin-empty">{error ? 'The catalog could not be displayed.' : 'No products yet. Add your first item above.'}</p>}
        </section>
      </section>
    </main>
  )
}
