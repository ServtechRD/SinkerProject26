import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(setHealth)
      .catch((err) => setError(err.message))
  }, [])

  return (
    <>
      <h1>Sinker Project</h1>
      <div className="card">
        <h2>API Health Check</h2>
        {error && <p style={{ color: 'red' }}>Error: {error}</p>}
        {health && (
          <div>
            <p>Status: <strong>{health.status}</strong></p>
            <p>Timestamp: {health.timestamp}</p>
          </div>
        )}
        {!health && !error && <p>Loading...</p>}
      </div>
    </>
  )
}

export default App
